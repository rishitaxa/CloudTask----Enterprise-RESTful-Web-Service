package com.cloudtask.export.dto;

import java.time.Instant;

public record TaskExportDto(
        Long id,
        String title,
        String description,
        String status,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {}
