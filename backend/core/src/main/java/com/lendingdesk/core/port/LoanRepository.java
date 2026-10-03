package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.Loan;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
  Optional<Loan> findById(Long id);

  List<Loan> findAll();

  /** Loans filtered by employee and/or state; {@code null} means "any". Ordered by id. */
  List<Loan> find(Long employeeId, Boolean active);

  List<Loan> findActiveByEmployee(Long employeeId);

  long countActiveByEmployee(Long employeeId);

  Optional<Loan> findActiveByDevice(Long deviceId);

  Loan save(Loan loan);
}
