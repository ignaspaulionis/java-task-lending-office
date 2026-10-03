package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** M06 - CSV device import. See MidTasks.md. */
class M06_CsvImportTest extends ApiTest {

  private static final String HEADER = "inventoryTag,name,category\n";

  @Test
  void allValidRowsAreImported() {
    Response report =
        importCsv(HEADER + "NTL-100,Dell XPS 13,LAPTOP\nNTL-101,LG 27UL850,MONITOR\n")
            .expectStatus(200);

    assertThat(report.longAt("$.imported")).isEqualTo(2);
    assertThat((List<Object>) report.json("$.errors")).isEmpty();
    assertThat(deviceNames()).containsExactlyInAnyOrder("Dell XPS 13", "LG 27UL850");
  }

  @Test
  void invalidRowsAreReportedAndTheRestIsImported() {
    device("NTL-200", "Existing laptop", "LAPTOP");
    String csv =
        HEADER
            + "NTL-201,Dell XPS 13,LAPTOP\n" // line 2: ok
            + "NTL-202,,LAPTOP\n" // line 3: missing name
            + "XYZ-203,LG 27UL850,MONITOR\n" // line 4: tag must start with NTL-
            + "NTL-201,Dell XPS 15,LAPTOP\n" // line 5: same tag as line 2
            + "NTL-200,Another laptop,LAPTOP\n" // line 6: tag already in the database
            + "NTL-205,Only two columns\n" // line 7: missing category
            + "NTL-204,Pixel 9,PHONE\n"; // line 8: ok

    Response report = importCsv(csv).expectStatus(200);

    assertThat(report.longAt("$.imported")).isEqualTo(2);
    assertThat((List<Map<String, Object>>) report.json("$.errors"))
        .containsExactly(
            Map.of("line", 3, "reason", "MISSING_FIELD"),
            Map.of("line", 4, "reason", "INVALID_TAG"),
            Map.of("line", 5, "reason", "DUPLICATE_IN_FILE"),
            Map.of("line", 6, "reason", "TAG_EXISTS"),
            Map.of("line", 7, "reason", "MISSING_FIELD"));
    assertThat(deviceNames())
        .containsExactlyInAnyOrder("Existing laptop", "Dell XPS 13", "Pixel 9");
  }

  @Test
  void aFileWithAWrongHeaderIsRejectedAsAWhole() {
    importCsv("tag,name,category\nNTL-100,Dell XPS 13,LAPTOP\n")
        .expectProblem(400, "VALIDATION_FAILED");

    assertThat(deviceNames()).isEmpty();
  }

  @Test
  void anEmptyFileIsRejected() {
    importCsv("").expectProblem(400, "VALIDATION_FAILED");
  }

  @Test
  void nonAsciiNamesWindowsLineEndingsAndBlankLinesAreHandled() {
    Response report =
        importCsv(HEADER.replace("\n", "\r\n") + "NTL-300,Šarūno monitorius,MONITOR\r\n\r\n")
            .expectStatus(200);

    assertThat(report.longAt("$.imported")).isEqualTo(1);
    assertThat((List<Object>) report.json("$.errors")).isEmpty();
    assertThat(deviceNames()).containsExactly("Šarūno monitorius");
  }

  private Response importCsv(String content) {
    return api.postFile("/api/devices/import", "devices.csv", content);
  }

  private List<String> deviceNames() {
    return api.get("/api/devices?size=50").expectStatus(200).json("$.items[*].name");
  }
}
