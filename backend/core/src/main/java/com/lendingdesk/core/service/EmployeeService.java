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
import java.util.Locale;

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
    String normalizedEmail = normalize(email);
    requireEmailFree(normalizedEmail, null);
    return employees.save(new Employee(name, normalizedEmail));
  }

  /**
   * Updates an employee. Deactivating requires that the employee holds no loans, and removes
   * them from every waitlist.
   */
  @Transactional
  public Employee update(Long id, String name, String email, boolean active) {
    Employee employee = get(id);
    String normalizedEmail = normalize(email);
    requireEmailFree(normalizedEmail, id);
    if (employee.isActive() && !active) {
      if (loans.countActiveByEmployee(id) > 0) {
        throw new LendingException(ErrorCode.EMPLOYEE_HAS_LOANS);
      }
      waitlist.findByEmployee(id).forEach(waitlist::delete);
    }
    employee.setName(name);
    employee.setEmail(normalizedEmail);
    employee.setActive(active);
    return employees.save(employee);
  }

  private void requireEmailFree(String email, Long ownerId) {
    employees
        .findByEmail(email)
        .filter(other -> !other.getId().equals(ownerId))
        .ifPresent(
            other -> {
              throw new LendingException(ErrorCode.DUPLICATE_EMAIL);
            });
  }

  private static String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
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
