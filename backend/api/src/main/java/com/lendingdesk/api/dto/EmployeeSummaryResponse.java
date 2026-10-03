package com.lendingdesk.api.dto;

import java.util.List;

public record EmployeeSummaryResponse(
    EmployeeResponse employee, List<LoanResponse> loans, List<WaitlistPositionResponse> waitlist) {}
