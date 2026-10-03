package com.lendingdesk.persistence.adapter;

import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.persistence.jpa.JpaEmployeeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepositoryAdapter implements EmployeeRepository {

  private final JpaEmployeeRepository jpa;

  public EmployeeRepositoryAdapter(JpaEmployeeRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Employee> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public List<Employee> findAll() {
    return jpa.findAll(Sort.by("id"));
  }

  @Override
  public Employee save(Employee employee) {
    return jpa.save(employee);
  }
}
