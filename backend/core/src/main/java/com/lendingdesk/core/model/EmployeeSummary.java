package com.lendingdesk.core.model;

import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import java.util.List;

public record EmployeeSummary(
    Employee employee, List<Loan> activeLoans, List<WaitlistPosition> waitlist) {}
