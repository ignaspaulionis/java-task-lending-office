package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lendingdesk.support.ApiTest;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** M01 - Indexes and database rules. See MidTasks.md. */
class M01_IndexesTest extends ApiTest {

  private static final String INDEXES_OF_TABLE =
      """
      SELECT ix.indisunique AS is_unique,
             coalesce(pg_get_expr(ix.indpred, ix.indrelid), '') AS predicate,
             array_to_string(ARRAY(
                 SELECT a.attname
                 FROM unnest(ix.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)
                 JOIN pg_attribute a ON a.attrelid = ix.indrelid AND a.attnum = k.attnum
                 ORDER BY k.ord), ',') AS columns
      FROM pg_index ix
      JOIN pg_class t ON t.oid = ix.indrelid
      WHERE t.relname = ?
      """;

  @Test
  void theDatabaseAllowsOnlyOneActiveLoanPerDevice() {
    assertThat(indexes("loans"))
        .as("a unique index on loans(device_id) limited to active loans")
        .anyMatch(
            ix ->
                ix.unique()
                    && ix.columns().equals(List.of("device_id"))
                    && ix.predicate().contains("returned_at IS NULL"));

    long asta = insertEmployee("Asta Demo");
    long jonas = insertEmployee("Jonas Demo");
    long laptop = insertDevice("Dell XPS 13");
    insertActiveLoan(laptop, asta, NOW.plus(Duration.ofDays(14)));
    assertThatThrownBy(() -> insertActiveLoan(laptop, jonas, NOW.plus(Duration.ofDays(14))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void returnedLoansDoNotBlockNewLoans() {
    long asta = insertEmployee("Asta Demo");
    long laptop = insertDevice("Dell XPS 13");

    assertThatCode(
            () -> {
              insertLoan(laptop, asta, NOW.minus(Duration.ofDays(9)), NOW, NOW);
              insertLoan(laptop, asta, NOW.minus(Duration.ofDays(8)), NOW, NOW);
              insertActiveLoan(laptop, asta, NOW.plus(Duration.ofDays(14)));
            })
        .doesNotThrowAnyException();
  }

  @Test
  void anEmployeesActiveLoansAreFoundThroughAnIndex() {
    assertThat(indexes("loans"))
        .as("an index on loans starting with employee_id")
        .anyMatch(ix -> ix.columns().getFirst().equals("employee_id"));
  }

  @Test
  void anEmployeeCanBeInADevicesWaitlistOnlyOnce() {
    assertThat(indexes("waitlist_entries"))
        .as("a unique index on waitlist_entries(device_id, employee_id)")
        .anyMatch(
            ix ->
                ix.unique()
                    && Set.copyOf(ix.columns()).equals(Set.of("device_id", "employee_id")));

    long asta = insertEmployee("Asta Demo");
    long laptop = insertDevice("Dell XPS 13");
    insertWaitlistEntry(laptop, asta, NOW);
    assertThatThrownBy(() -> insertWaitlistEntry(laptop, asta, NOW.plusSeconds(1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void aQueueIsReadInOrderThroughAnIndex() {
    assertThat(indexes("waitlist_entries"))
        .as("an index on waitlist_entries starting with (device_id, created_at)")
        .anyMatch(
            ix ->
                ix.columns().size() >= 2
                    && ix.columns().subList(0, 2).equals(List.of("device_id", "created_at")));
  }

  @Test
  void schemaChangesAreAddedAsANewMigration() {
    List<String> versions =
        jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL"
                + " ORDER BY installed_rank",
            String.class);

    assertThat(versions).first().isEqualTo("1");
    assertThat(versions).as("applied migrations").hasSizeGreaterThanOrEqualTo(2);
  }

  private List<IndexInfo> indexes(String table) {
    return jdbc.query(
        INDEXES_OF_TABLE,
        (rs, row) ->
            new IndexInfo(
                rs.getBoolean("is_unique"),
                rs.getString("predicate"),
                Arrays.asList(rs.getString("columns").split(","))),
        table);
  }

  private record IndexInfo(boolean unique, String predicate, List<String> columns) {}
}
