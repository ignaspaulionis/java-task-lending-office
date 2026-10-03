package com.lendingdesk.core.model;

import com.lendingdesk.core.domain.Loan;

/** Either a queue position or, when the device was free, the loan that was created. */
public record WaitlistJoinResult(Integer position, Loan loan) {}
