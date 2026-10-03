package com.lendingdesk.core.model;

import com.lendingdesk.core.domain.Loan;

/** The returned loan and the employee who received the device next, if anyone. */
public record ReturnResult(Loan loan, Long nextEmployeeId) {}
