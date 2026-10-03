package com.lendingdesk.api.dto;

import com.lendingdesk.core.domain.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDeviceRequest(
    @NotBlank @Size(max = 100) String name, @NotNull DeviceStatus status) {}
