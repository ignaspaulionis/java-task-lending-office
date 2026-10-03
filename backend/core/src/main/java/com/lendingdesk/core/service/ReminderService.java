package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Builds the reminder text shown to an employee for one loan. */
public class ReminderService {

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
    Loan l =
        loans.findById(loanId).orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    String msg = "";
    if (l.getReturnedAt() != null) {
      msg = "No reminder: " + l.getDevice().getName() + " was returned.";
    } else {
      long d =
          ChronoUnit.DAYS.between(
              LocalDate.ofInstant(clock.instant(), ZoneId.of("Europe/Vilnius")),
              LocalDate.ofInstant(l.getDueAt(), ZoneId.of("Europe/Vilnius")));
      if (d < 0) {
        if (d == -1) {
          msg = "Overdue: " + l.getDevice().getName() + " was due 1 day ago. Please return it.";
        } else {
          msg =
              "Overdue: "
                  + l.getDevice().getName()
                  + " was due "
                  + (-d)
                  + " days ago. Please return it.";
        }
        if (l.getDevice().getCategory().equals("LAPTOP")) {
          msg = msg + " Laptops hold company data: return it to IT today.";
        }
      } else {
        if (d == 0) {
          msg = "Reminder: " + l.getDevice().getName() + " is due today.";
          if (waitlist.findByDevice(l.getDevice().getId()).size() > 0) {
            msg = msg + " Someone is waiting for it.";
          }
        } else if (d <= 3) {
          if (d == 1) {
            msg = "Reminder: " + l.getDevice().getName() + " is due in 1 day.";
          } else {
            msg = "Reminder: " + l.getDevice().getName() + " is due in " + d + " days.";
          }
          if (waitlist.findByDevice(l.getDevice().getId()).size() > 0) {
            msg = msg + " Someone is waiting for it.";
          }
        } else {
          msg = "No reminder needed.";
        }
      }
    }
    return msg;
  }
}
