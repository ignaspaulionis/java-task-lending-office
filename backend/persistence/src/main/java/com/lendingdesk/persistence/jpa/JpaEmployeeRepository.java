package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEmployeeRepository extends JpaRepository<Employee, Long> {}
