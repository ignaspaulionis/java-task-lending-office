package com.lendingdesk.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

/** Business constants of the lending desk. */
public final class LendingRules {

  public static final int MAX_ACTIVE_LOANS = 3;
  public static final Duration LOAN_PERIOD = Duration.ofDays(14);
  public static final Duration EXTENSION = Duration.ofDays(7);
  public static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("0.50");
  public static final BigDecimal LATE_FEE_CAP = new BigDecimal("20.00");
  public static final ZoneId LOCAL_ZONE = ZoneId.of("Europe/Vilnius");

  private LendingRules() {}
}
