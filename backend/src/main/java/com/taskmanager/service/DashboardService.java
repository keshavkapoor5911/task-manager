package com.taskmanager.service;

import com.taskmanager.dto.DashboardStatsResponse;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.exception.UnauthorizedAccessException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public DashboardStatsResponse getMyTasksStats(String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long totalTasks = taskRepository.countByAssigneeId(currentUser.getId());
        long completed = taskRepository.countByAssigneeIdAndStatus(currentUser.getId(), TaskStatus.DONE);
        long inProgress = taskRepository.countByAssigneeIdAndStatus(currentUser.getId(), TaskStatus.IN_PROGRESS);
        long todo = taskRepository.countByAssigneeIdAndStatus(currentUser.getId(), TaskStatus.TODO);
        
        long overdue = taskRepository.findByAssigneeIdAndDueDateBeforeAndStatusNot(
                currentUser.getId(), LocalDate.now(), TaskStatus.DONE).size();

        return DashboardStatsResponse.builder()
                .totalTasks(totalTasks)
                .tasksCompleted(completed)
                .tasksInProgress(inProgress)
                .tasksToDo(todo)
                .overdueTasks(overdue)
                .build();
    }

    public DashboardStatsResponse getProjectDashboardStats(Long projectId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        long totalTasks = taskRepository.countByProjectId(projectId);
        long completed = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        long inProgress = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS);
        long todo = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO);
        
        long overdue = taskRepository.findByProjectIdAndDueDateBeforeAndStatusNot(
                projectId, LocalDate.now(), TaskStatus.DONE).size();

        return DashboardStatsResponse.builder()
                .totalTasks(totalTasks)
                .tasksCompleted(completed)
                .tasksInProgress(inProgress)
                .tasksToDo(todo)
                .overdueTasks(overdue)
                .build();
    }
}
