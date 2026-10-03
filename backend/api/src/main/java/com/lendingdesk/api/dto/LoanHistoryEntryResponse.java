package com.lendingdesk.api.dto;

import java.time.Instant;

public record LoanHistoryEntryResponse(
    Long loanId, Long deviceId, String deviceName, Instant borrowedAt, Instant returnedAt) {}
