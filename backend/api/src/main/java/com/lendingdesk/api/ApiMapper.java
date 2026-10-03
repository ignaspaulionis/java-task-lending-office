package com.lendingdesk.api;

import com.lendingdesk.api.dto.DeviceResponse;
import com.lendingdesk.api.dto.EmployeeResponse;
import com.lendingdesk.api.dto.LoanResponse;
import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.model.DeviceListItem;
import com.lendingdesk.core.service.LoanTerms;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Converts domain objects to API responses. */
@Component
public class ApiMapper {

  private final LoanTerms terms;
  private final Clock clock;

  public ApiMapper(LoanTerms terms, Clock clock) {
    this.terms = terms;
    this.clock = clock;
  }

  public EmployeeResponse toResponse(Employee employee) {
    return new EmployeeResponse(
        employee.getId(), employee.getName(), employee.getEmail(), employee.isActive());
  }

  public DeviceResponse toResponse(DeviceListItem item) {
    Device device = item.device();
    return new DeviceResponse(
        device.getId(),
        device.getInventoryTag(),
        device.getName(),
        device.getCategory(),
        device.getStatus(),
        item.available(),
        item.loanedTo());
  }

  public LoanResponse toResponse(Loan loan) {
    Instant now = clock.instant();
    BigDecimal fee = terms.lateFee(loan, now);
    return new LoanResponse(
        loan.getId(),
        loan.getDevice().getId(),
        loan.getDevice().getName(),
        loan.getEmployee().getId(),
        loan.getBorrowedAt(),
        loan.getDueAt(),
        loan.getReturnedAt(),
        terms.isOverdue(loan, now),
        fee == null ? null : fee.toPlainString());
  }
}
