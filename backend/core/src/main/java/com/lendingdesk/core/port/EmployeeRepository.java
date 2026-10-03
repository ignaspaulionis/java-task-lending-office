package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
  Optional<Employee> findById(Long id);

  /** Reads the row and locks it until the current transaction ends. */
  Optional<Employee> findByIdForUpdate(Long id);

  List<Employee> findAll();

  Employee save(Employee employee);
}
