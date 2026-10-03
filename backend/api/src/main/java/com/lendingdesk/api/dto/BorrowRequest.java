package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotNull;

public record BorrowRequest(@NotNull Long deviceId, @NotNull Long employeeId) {}
