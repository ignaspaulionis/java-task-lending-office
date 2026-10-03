package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * J08 - Due dates skip weekends. See JuniorTasks.md.
 *
 * <p>Vilnius is UTC+2 until 2026-03-29 03:00 and UTC+3 after it.
 */
class J08_WeekendDueDateTest extends ApiTest {

  @Test
  void aWeekdayDueDateIsUnchanged() {
    // Tuesday 12:00 in Vilnius -> due Tuesday 24 March
    assertThat(dueAtWhenBorrowedAt("2026-03-10T10:00:00Z")).isEqualTo("2026-03-24T10:00:00Z");
  }

  @Test
  void aFridayDueDateIsUnchanged() {
    // Friday 22:00 in Vilnius -> due Friday 27 March 22:00
    assertThat(dueAtWhenBorrowedAt("2026-03-13T20:00:00Z")).isEqualTo("2026-03-27T20:00:00Z");
  }

  @Test
  void aSaturdayDueDateMovesToMondayAtTheSameLocalTime() {
    // Saturday 12:00 -> due Saturday 28 March 12:00 -> Monday 30 March 12:00 (now UTC+3)
    assertThat(dueAtWhenBorrowedAt("2026-03-14T10:00:00Z")).isEqualTo("2026-03-30T09:00:00Z");
  }

  @Test
  void aSundayDueDateMovesToMondayAtTheSameLocalTime() {
    // Sunday 12:00 -> 14 days later is Sunday 29 March 13:00 local -> Monday 30 March 13:00
    assertThat(dueAtWhenBorrowedAt("2026-03-15T10:00:00Z")).isEqualTo("2026-03-30T10:00:00Z");
  }

  @Test
  void theWeekendIsJudgedInVilniusTime() {
    // Friday 22:30 UTC is already Saturday 00:30 in Vilnius
    assertThat(dueAtWhenBorrowedAt("2026-03-13T22:30:00Z")).isEqualTo("2026-03-29T21:30:00Z");
  }

  private String dueAtWhenBorrowedAt(String instant) {
    clock.set(Instant.parse(instant));
    return borrow(device("Dell XPS 13"), employee("Asta Demo")).expectStatus(201).json("$.dueAt");
  }
}
