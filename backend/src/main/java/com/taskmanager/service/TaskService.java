package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.dto.TaskStatusUpdateRequest;
import com.taskmanager.dto.UserDto;
import com.taskmanager.entity.*;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.exception.UnauthorizedAccessException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(Long projectId, TaskRequest request, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only ADMIN can create tasks");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            
            projectMemberRepository.findByProjectIdAndUserId(projectId, assignee.getId())
                    .orElseThrow(() -> new BadRequestException("Assignee is not a member of this project"));
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .priority(request.getPriority())
                .status(request.getStatus())
                .project(project)
                .assignee(assignee)
                .build();

        task = taskRepository.save(task);

        return mapToResponse(task);
    }

    public List<TaskResponse> getProjectTasks(Long projectId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        return taskRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long taskId, String currentUserEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        projectMemberRepository.findByProjectIdAndUserId(task.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, String currentUserEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(task.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            // Check if MEMBER is assignee, maybe allow updates?
            // "Only ADMIN can manage all project tasks... Members can only update their assigned tasks/status"
            // The prompt says "Members can update only their assigned tasks/status".
            // So if they are assignee, they can update everything about the task, or just status? Let's say status only or if assignee.
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Only ADMIN or the Assignee can update this task");
            }
        }

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            
            projectMemberRepository.findByProjectIdAndUserId(task.getProject().getId(), assignee.getId())
                    .orElseThrow(() -> new BadRequestException("Assignee is not a member of this project"));
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setAssignee(assignee);

        task = taskRepository.save(task);

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest request, String currentUserEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(task.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Only ADMIN or the Assignee can update task status");
            }
        }

        task.setStatus(request.getStatus());
        task = taskRepository.save(task);

        return mapToResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId, String currentUserEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(task.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only ADMIN can delete a task");
        }

        taskRepository.delete(task);
    }

    private TaskResponse mapToResponse(Task task) {
        UserDto assigneeDto = null;
        if (task.getAssignee() != null) {
            assigneeDto = UserDto.builder()
                    .id(task.getAssignee().getId())
                    .name(task.getAssignee().getName())
                    .email(task.getAssignee().getEmail())
                    .build();
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .priority(task.getPriority())
                .status(task.getStatus())
                .projectId(task.getProject().getId())
                .assignee(assigneeDto)
                .build();
    }
}
