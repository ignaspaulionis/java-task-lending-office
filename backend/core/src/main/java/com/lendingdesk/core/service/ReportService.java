package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.TopDevice;
import com.lendingdesk.core.port.LoanRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReportService {

  static final int TOP_DEVICES = 5;

  private final LoanRepository loans;

  public ReportService(LoanRepository loans) {
    this.loans = loans;
  }

  /**
   * The devices borrowed most often in a period of local (Vilnius) dates, ties by name.
   *
   * @param to first day after the period
   */
  @Transactional
  public List<TopDevice> topDevices(LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      throw new LendingException(ErrorCode.VALIDATION_FAILED, "from must not be after to");
    }
    return loans.findMostBorrowedDevices(startOf(from), startOf(to), TOP_DEVICES);
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(LendingRules.LOCAL_ZONE).toInstant();
  }
}
