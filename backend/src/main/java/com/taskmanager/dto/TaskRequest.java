package com.taskmanager.dto;

import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {
    @NotBlank
    private String title;
    
    private String description;
    
    private LocalDate dueDate;
    
    @NotNull
    private TaskPriority priority;
    
    @NotNull
    private TaskStatus status;
    
    private Long assigneeId;
}
