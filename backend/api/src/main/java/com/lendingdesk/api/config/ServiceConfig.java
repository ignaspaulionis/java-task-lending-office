package com.lendingdesk.api.config;

import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import com.lendingdesk.core.service.DeviceService;
import com.lendingdesk.core.service.EmployeeService;
import com.lendingdesk.core.service.LoanService;
import com.lendingdesk.core.service.LoanTerms;
import com.lendingdesk.core.service.ReminderService;
import com.lendingdesk.core.service.WaitlistService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free core services. */
@Configuration
public class ServiceConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  LoanTerms loanTerms() {
    return new LoanTerms();
  }

  @Bean
  LoanService loanService(
      LoanRepository loans,
      DeviceRepository devices,
      EmployeeRepository employees,
      WaitlistRepository waitlist,
      LoanTerms loanTerms,
      Clock clock) {
    return new LoanService(loans, devices, employees, waitlist, loanTerms, clock);
  }

  @Bean
  WaitlistService waitlistService(
      WaitlistRepository waitlist,
      DeviceRepository devices,
      EmployeeRepository employees,
      Clock clock) {
    return new WaitlistService(waitlist, devices, employees, clock);
  }

  @Bean
  ReminderService reminderService(
      LoanRepository loans, WaitlistRepository waitlist, Clock clock) {
    return new ReminderService(loans, waitlist, clock);
  }

  @Bean
  EmployeeService employeeService(
      EmployeeRepository employees, LoanRepository loans, WaitlistRepository waitlist) {
    return new EmployeeService(employees, loans, waitlist);
  }

  @Bean
  DeviceService deviceService(DeviceRepository devices, LoanRepository loans) {
    return new DeviceService(devices, loans);
  }
}
