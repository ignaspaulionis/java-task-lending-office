package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotNull;

public record TransferRequest(@NotNull Long fromEmployeeId, @NotNull Long toEmployeeId) {}
