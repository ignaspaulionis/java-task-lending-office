package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Builds the reminder text shown to an employee for one loan. */
public class ReminderService {

  private static final int DEFAULT_REMINDER_DAYS = 3;
  private static final Map<String, Integer> REMINDER_DAYS_BY_CATEGORY = Map.of("PHONE", 5);
  private static final String LAPTOP = "LAPTOP";

  private final LoanRepository loans;
  private final WaitlistRepository waitlist;
  private final Clock clock;

  public ReminderService(LoanRepository loans, WaitlistRepository waitlist, Clock clock) {
    this.loans = loans;
    this.waitlist = waitlist;
    this.clock = clock;
  }

  @Transactional
  public String reminder(Long loanId) {
    Loan loan =
        loans.findById(loanId).orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    Device device = loan.getDevice();
    if (!loan.isActive()) {
      return "No reminder: %s was returned.".formatted(device.getName());
    }
    long daysLeft = ChronoUnit.DAYS.between(localDate(clock.instant()), localDate(loan.getDueAt()));
    if (daysLeft < 0) {
      return overdueReminder(device, -daysLeft);
    }
    if (daysLeft > reminderDays(device)) {
      return "No reminder needed.";
    }
    String reminder =
        daysLeft == 0
            ? "Reminder: %s is due today.".formatted(device.getName())
            : "Reminder: %s is due in %s.".formatted(device.getName(), days(daysLeft));
    return someoneIsWaiting(device) ? reminder + " Someone is waiting for it." : reminder;
  }

  private static String overdueReminder(Device device, long daysLate) {
    String reminder =
        "Overdue: %s was due %s ago. Please return it.".formatted(device.getName(), days(daysLate));
    if (LAPTOP.equals(device.getCategory())) {
      reminder += " Laptops hold company data: return it to IT today.";
    }
    return reminder;
  }

  private static int reminderDays(Device device) {
    return REMINDER_DAYS_BY_CATEGORY.getOrDefault(device.getCategory(), DEFAULT_REMINDER_DAYS);
  }

  private boolean someoneIsWaiting(Device device) {
    return !waitlist.findByDevice(device.getId()).isEmpty();
  }

  private static String days(long count) {
    return count == 1 ? "1 day" : count + " days";
  }

  private static LocalDate localDate(Instant instant) {
    return LocalDate.ofInstant(instant, LendingRules.LOCAL_ZONE);
  }
}
