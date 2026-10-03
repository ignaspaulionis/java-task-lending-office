package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReturnRequest(@NotNull Long employeeId) {}
