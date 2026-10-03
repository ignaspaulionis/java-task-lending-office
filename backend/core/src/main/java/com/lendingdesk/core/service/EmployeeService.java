package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.EmployeeSummary;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.util.List;

public class EmployeeService {

  private final EmployeeRepository employees;
  private final LoanRepository loans;
  private final WaitlistRepository waitlist;

  public EmployeeService(
      EmployeeRepository employees, LoanRepository loans, WaitlistRepository waitlist) {
    this.employees = employees;
    this.loans = loans;
    this.waitlist = waitlist;
  }

  @Transactional
  public List<Employee> list() {
    return employees.findAll();
  }

  @Transactional
  public Employee create(String name, String email) {
    return employees.save(new Employee(name, email));
  }

  @Transactional
  public Employee update(Long id, String name, String email, boolean active) {
    Employee employee = get(id);
    employee.setName(name);
    employee.setEmail(email);
    employee.setActive(active);
    return employees.save(employee);
  }

  /** The employee's active loans and their position in every waitlist they are on. */
  @Transactional
  public EmployeeSummary summary(Long id) {
    Employee employee = get(id);
    return new EmployeeSummary(
        employee, loans.findActiveByEmployee(id), waitlist.findPositionsOfEmployee(id));
  }

  private Employee get(Long id) {
    return employees.findById(id).orElseThrow(() -> new LendingException(ErrorCode.EMPLOYEE_NOT_FOUND));
  }
}
