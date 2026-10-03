package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Loan;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface JpaLoanRepository
    extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from Loan l where l.id = :id")
  Optional<Loan> findByIdForUpdate(Long id);

  @Query(
      """
      select l from Loan l join fetch l.device
      where l.employee.id = :employeeId and l.returnedAt is null
      order by l.id
      """)
  List<Loan> findActiveByEmployeeWithDevice(Long employeeId);

  @Query(
      """
      select l from Loan l join fetch l.device
      where l.employee.id = :employeeId and l.returnedAt is not null
      order by l.returnedAt desc, l.id desc
      """)
  List<Loan> findReturnedByEmployee(Long employeeId);

  Optional<Loan> findByDeviceIdAndReturnedAtIsNull(Long deviceId);

  long countByEmployeeIdAndReturnedAtIsNull(Long employeeId);

  @Query(
      """
      select l from Loan l join fetch l.device
      where l.returnedAt is null and l.dueAt < :cutoff
      order by l.dueAt, l.id
      """)
  List<Loan> findActiveDueBefore(Instant cutoff);

  @Query(
      """
      select d.id as deviceId, d.name as deviceName, count(l) as loanCount
      from Loan l join l.device d
      where l.borrowedAt >= :from and l.borrowedAt < :to
      group by d.id, d.name
      order by count(l) desc, d.name, d.id
      """)
  List<TopDeviceRow> findMostBorrowedDevices(Instant from, Instant to, Limit limit);

  interface TopDeviceRow {
    Long getDeviceId();

    String getDeviceName();

    Long getLoanCount();
  }
}
