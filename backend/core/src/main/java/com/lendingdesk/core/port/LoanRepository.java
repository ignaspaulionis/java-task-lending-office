package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.Loan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
  Optional<Loan> findById(Long id);

  List<Loan> findAll();

  /** Loans filtered by employee and/or state; {@code null} means "any". Ordered by id. */
  List<Loan> find(Long employeeId, Boolean active);

  List<Loan> findActiveByEmployee(Long employeeId);

  long countActiveByEmployee(Long employeeId);

  /** Active loans due before {@code cutoff}, most overdue first, with their devices. */
  List<Loan> findActiveDueBefore(Instant cutoff);

  Optional<Loan> findActiveByDevice(Long deviceId);

  Loan save(Loan loan);

  /** Saves and writes pending changes to the database immediately. */
  Loan saveAndFlush(Loan loan);
}
