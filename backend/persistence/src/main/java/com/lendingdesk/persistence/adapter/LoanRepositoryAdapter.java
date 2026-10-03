package com.lendingdesk.persistence.adapter;

import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.persistence.jpa.JpaLoanRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  public List<Loan> findAll() {
    return jpa.findAll();
  }

  @Override
  public List<Loan> find(Long employeeId, Boolean active) {
    Specification<Loan> filter =
        (root, query, cb) -> {
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
    return jpa.findByEmployeeIdAndReturnedAtIsNullOrderByIdAsc(employeeId);
  }

  @Override
  public long countActiveByEmployee(Long employeeId) {
    return jpa.countByEmployeeIdAndReturnedAtIsNull(employeeId);
  }

  @Override
  public Optional<Loan> findActiveByDevice(Long deviceId) {
    return jpa.findByDeviceIdAndReturnedAtIsNull(deviceId);
  }

  @Override
  public Loan save(Loan loan) {
    return jpa.save(loan);
  }
}
