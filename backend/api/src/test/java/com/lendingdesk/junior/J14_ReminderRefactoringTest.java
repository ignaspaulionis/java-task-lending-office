package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * J14 - Refactor the reminder service. See JuniorTasks.md.
 *
 * <p>Loans are borrowed on Tuesday 10 March and due Tuesday 24 March 12:00 in Vilnius.
 */
class J14_ReminderRefactoringTest extends ApiTest {

  /** What the reminder service does today. These pass already and must keep passing. */
  @Nested
  class CurrentBehaviour {

    @Test
    void noReminderWhileTheDueDateIsFarAway() {
      assertThat(reminderOn("LAPTOP", "2026-03-20T10:00:00Z")).isEqualTo("No reminder needed.");
    }

    @Test
    void remindsThreeDaysBefore() {
      assertThat(reminderOn("LAPTOP", "2026-03-21T10:00:00Z"))
          .isEqualTo("Reminder: Dell XPS 13 is due in 3 days.");
    }

    @Test
    void remindsTheDayBefore() {
      assertThat(reminderOn("LAPTOP", "2026-03-23T10:00:00Z"))
          .isEqualTo("Reminder: Dell XPS 13 is due in 1 day.");
    }

    @Test
    void remindsOnTheDueDay() {
      assertThat(reminderOn("LAPTOP", "2026-03-24T20:00:00Z"))
          .isEqualTo("Reminder: Dell XPS 13 is due today.");
    }

    @Test
    void mentionsSomeoneWaiting() {
      long laptop = device("NTL-1", "Dell XPS 13", "LAPTOP");
      borrowOk(laptop, employee("Asta Demo"));
      joinWaitlist(laptop, employee("Jonas Demo")).expectStatus(201);

      clock.set(Instant.parse("2026-03-22T10:00:00Z"));

      assertThat(reminder(1)).isEqualTo("Reminder: Dell XPS 13 is due in 2 days. Someone is waiting for it.");
    }

    @Test
    void anOverdueMonitor() {
      assertThat(reminderOn("MONITOR", "2026-03-26T10:00:00Z"))
          .isEqualTo("Overdue: Dell XPS 13 was due 2 days ago. Please return it.");
    }

    @Test
    void anOverdueLaptopGetsAnExtraWarning() {
      assertThat(reminderOn("LAPTOP", "2026-03-25T10:00:00Z"))
          .isEqualTo(
              "Overdue: Dell XPS 13 was due 1 day ago. Please return it."
                  + " Laptops hold company data: return it to IT today.");
    }

    @Test
    void aReturnedLoanNeedsNoReminder() {
      long asta = employee("Asta Demo");
      long loan = borrowOk(device("NTL-1", "Dell XPS 13", "LAPTOP"), asta);
      returnLoan(loan, asta).expectStatus(200);

      assertThat(reminder(loan)).isEqualTo("No reminder: Dell XPS 13 was returned.");
    }
  }

  /** The new rule to add after refactoring. */
  @Nested
  class PhoneReminders {

    @Test
    void phonesAreRemindedFiveDaysBefore() {
      assertThat(reminderOn("PHONE", "2026-03-19T10:00:00Z"))
          .isEqualTo("Reminder: Dell XPS 13 is due in 5 days.");
    }

    @Test
    void otherDevicesAreStillRemindedThreeDaysBefore() {
      assertThat(reminderOn("LAPTOP", "2026-03-19T10:00:00Z")).isEqualTo("No reminder needed.");
    }
  }

  /** Borrows a device of the given category at {@link #NOW} and asks for a reminder later. */
  private String reminderOn(String category, String instant) {
    long loan = borrowOk(device("NTL-1", "Dell XPS 13", category), employee("Asta Demo"));
    clock.set(Instant.parse(instant));
    return reminder(loan);
  }

  private String reminder(long loanId) {
    return api.get("/api/loans/" + loanId + "/reminder").expectStatus(200).json("$.message");
  }
}
