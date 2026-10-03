package com.lendingdesk.persistence.adapter;

import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.model.TopDevice;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.persistence.jpa.JpaLoanRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class LoanRepositoryAdapter implements LoanRepository {

  private final JpaLoanRepository jpa;

  public LoanRepositoryAdapter(JpaLoanRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Loan> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public Optional<Loan> findByIdForUpdate(Long id) {
    return jpa.findByIdForUpdate(id);
  }

  @Override
  public List<Loan> find(Long employeeId, Boolean active) {
    Specification<Loan> filter =
        (root, query, cb) -> {
          // Load each loan's device in the same query; the API shows the device name.
          root.fetch("device");
          List<Predicate> predicates = new ArrayList<>();
          if (employeeId != null) {
            predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
          }
          if (active != null) {
            predicates.add(
                active ? cb.isNull(root.get("returnedAt")) : cb.isNotNull(root.get("returnedAt")));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };
    return jpa.findAll(filter, Sort.by("id"));
  }

  @Override
  public List<Loan> findActiveByEmployee(Long employeeId) {
    return jpa.findActiveByEmployeeWithDevice(employeeId);
  }

  @Override
  public long countActiveByEmployee(Long employeeId) {
    return jpa.countByEmployeeIdAndReturnedAtIsNull(employeeId);
  }

  @Override
  public List<Loan> findActiveDueBefore(Instant cutoff) {
    return jpa.findActiveDueBefore(cutoff);
  }

  @Override
  public Optional<Loan> findActiveByDevice(Long deviceId) {
    return jpa.findByDeviceIdAndReturnedAtIsNull(deviceId);
  }

  @Override
  public List<TopDevice> findMostBorrowedDevices(Instant from, Instant to, int limit) {
    return jpa.findMostBorrowedDevices(from, to, Limit.of(limit)).stream()
        .map(row -> new TopDevice(row.getDeviceId(), row.getDeviceName(), row.getLoanCount()))
        .toList();
  }

  @Override
  public Loan save(Loan loan) {
    return jpa.save(loan);
  }

  @Override
  public Loan saveAndFlush(Loan loan) {
    return jpa.saveAndFlush(loan);
  }
}
