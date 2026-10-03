package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotNull;

public record CancelLoanRequest(@NotNull Long employeeId) {}
