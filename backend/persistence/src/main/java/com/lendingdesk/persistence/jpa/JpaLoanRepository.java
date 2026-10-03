package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Loan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaLoanRepository
    extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

  List<Loan> findByEmployeeIdAndReturnedAtIsNullOrderByIdAsc(Long employeeId);

  Optional<Loan> findByDeviceIdAndReturnedAtIsNull(Long deviceId);
}
