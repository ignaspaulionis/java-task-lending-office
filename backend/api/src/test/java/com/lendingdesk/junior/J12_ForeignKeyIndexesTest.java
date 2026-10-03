package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;

/** J12 - Index the foreign keys. See JuniorTasks.md. */
class J12_ForeignKeyIndexesTest extends ApiTest {

  /** First column of every index on the table that covers all rows (not partial). */
  private static final String LEADING_COLUMNS =
      """
      SELECT a.attname
      FROM pg_index ix
      JOIN pg_class t ON t.oid = ix.indrelid
      JOIN pg_attribute a ON a.attrelid = ix.indrelid AND a.attnum = ix.indkey[0]
      WHERE t.relname = ? AND ix.indpred IS NULL
      """;

  @ParameterizedTest(name = "{0}.{1}")
  @CsvSource({
    "loans, device_id",
    "loans, employee_id",
    "waitlist_entries, device_id",
    "waitlist_entries, employee_id"
  })
  void everyForeignKeyColumnStartsAnIndex(String table, String column) {
    List<String> leadingColumns = jdbc.queryForList(LEADING_COLUMNS, String.class, table);

    assertThat(leadingColumns).as("columns that start an index on %s", table).contains(column);
  }

  @Test
  void findingAnEmployeesLoansCanUseAnIndex() {
    List<String> plan =
        jdbc.execute(
            (ConnectionCallback<List<String>>)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    // Tables are tiny in tests; forbid sequential scans to see whether an index
                    // could be used at all.
                    statement.execute("SET enable_seqscan = off");
                    List<String> lines = new ArrayList<>();
                    try (ResultSet rows =
                        statement.executeQuery("EXPLAIN SELECT * FROM loans WHERE employee_id = 1")) {
                      while (rows.next()) {
                        lines.add(rows.getString(1));
                      }
                    }
                    statement.execute("RESET enable_seqscan");
                    return lines;
                  }
                });

    assertThat(String.join("\n", plan)).as("query plan").contains("Index");
  }
}
