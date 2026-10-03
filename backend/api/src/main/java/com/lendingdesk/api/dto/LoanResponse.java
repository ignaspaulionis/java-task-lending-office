package com.lendingdesk.api.dto;

import java.time.Instant;

/**
 * A loan as shown to clients.
 *
 * @param lateFee amount in euros with two decimals, e.g. "1.50"
 */
public record LoanResponse(
    Long id,
    Long deviceId,
    String deviceName,
    Long employeeId,
    Instant borrowedAt,
    Instant dueAt,
    Instant returnedAt,
    boolean overdue,
    String lateFee) {}
