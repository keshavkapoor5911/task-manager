package com.taskmanager.service;

import com.taskmanager.dto.ProjectRequest;
import com.taskmanager.dto.ProjectResponse;
import com.taskmanager.dto.UserDto;
import com.taskmanager.entity.Project;
import com.taskmanager.entity.ProjectMember;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.User;
import com.taskmanager.exception.BadRequestException;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.exception.UnauthorizedAccessException;
import com.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(user)
                .build();

        project = projectRepository.save(project);

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(Role.ADMIN) // Creator is ADMIN
                .build();
        projectMemberRepository.save(member);

        return mapToResponse(project, Role.ADMIN.name());
    }

    public List<ProjectResponse> getUserProjects(String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return projectMemberRepository.findByUserId(user.getId()).stream()
                .map(pm -> mapToResponse(pm.getProject(), pm.getRole().name()))
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long projectId, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        return mapToResponse(pm.getProject(), pm.getRole().name());
    }

    @Transactional
    public void deleteProject(Long projectId, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only ADMIN can delete the project");
        }

        // Project members and tasks will be deleted if we set up cascade,
        // but since we didn't add cascade in entity, we should delete them manually.
        // Actually, let's keep it simple. It's better to delete project members first.
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        projectMemberRepository.deleteAll(members);
        
        // Note: Tasks should be deleted. We will inject TaskRepository or handle it later.
        projectRepository.deleteById(projectId);
    }

    @Transactional
    public void addMember(Long projectId, String newMemberEmail, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only ADMIN can add members");
        }

        User newMember = userRepository.findByEmail(newMemberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User to add not found"));

        if (projectMemberRepository.findByProjectIdAndUserId(projectId, newMember.getId()).isPresent()) {
            throw new BadRequestException("User is already a member of this project");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectMember newPm = ProjectMember.builder()
                .project(project)
                .user(newMember)
                .role(Role.MEMBER)
                .build();
        
        projectMemberRepository.save(newPm);
    }

    @Transactional
    public void removeMember(Long projectId, Long memberIdToRemove, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        if (pm.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only ADMIN can remove members");
        }
        
        if (currentUser.getId().equals(memberIdToRemove)) {
            throw new BadRequestException("ADMIN cannot remove themselves. Delete project instead.");
        }

        ProjectMember pmToRemove = projectMemberRepository.findByProjectIdAndUserId(projectId, memberIdToRemove)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in project"));

        projectMemberRepository.delete(pmToRemove);
    }
    
    public List<UserDto> getProjectMembers(Long projectId, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("You do not have access to this project"));

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(pm -> UserDto.builder()
                        .id(pm.getUser().getId())
                        .name(pm.getUser().getName())
                        .email(pm.getUser().getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    private ProjectResponse mapToResponse(Project project, String role) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .createdBy(UserDto.builder()
                        .id(project.getCreatedBy().getId())
                        .name(project.getCreatedBy().getName())
                        .email(project.getCreatedBy().getEmail())
                        .build())
                .currentUserRole(role)
                .build();
    }
}
