package com.lendingdesk.api.dto;

import com.lendingdesk.api.validation.InventoryTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
    @NotBlank @InventoryTag @Size(max = 50) String inventoryTag,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String category) {}
