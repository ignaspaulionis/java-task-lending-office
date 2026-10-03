package com.lendingdesk.api.dto;

import com.lendingdesk.core.domain.DeviceStatus;

public record DeviceResponse(
    Long id,
    String inventoryTag,
    String name,
    String category,
    DeviceStatus status,
    boolean available,
    Long loanedTo) {}
