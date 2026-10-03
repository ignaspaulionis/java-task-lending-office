package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Loan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface JpaLoanRepository
    extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

  List<Loan> findByEmployeeIdAndReturnedAtIsNullOrderByIdAsc(Long employeeId);

  Optional<Loan> findByDeviceIdAndReturnedAtIsNull(Long deviceId);

  long countByEmployeeIdAndReturnedAtIsNull(Long employeeId);

  @Query(
      """
      select l from Loan l join fetch l.device
      where l.returnedAt is null and l.dueAt < :cutoff
      order by l.dueAt, l.id
      """)
  List<Loan> findActiveDueBefore(Instant cutoff);
}
