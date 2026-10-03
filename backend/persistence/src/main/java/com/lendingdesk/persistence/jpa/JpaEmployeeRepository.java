package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Employee;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface JpaEmployeeRepository extends JpaRepository<Employee, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Employee e where e.id = :id")
  Optional<Employee> findByIdForUpdate(Long id);

  Optional<Employee> findByEmail(String email);
}
