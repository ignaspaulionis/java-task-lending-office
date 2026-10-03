package com.lendingdesk.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
    @NotBlank @Pattern(regexp = "NTL-.+") @Size(max = 50) String inventoryTag,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String category) {}
