package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotNull;

public record ExtendRequest(@NotNull Long employeeId) {}
