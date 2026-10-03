package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotNull;

public record WaitlistRequest(@NotNull Long employeeId) {}
