package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** M03 - Device search and paging in SQL. See MidTasks.md. */
class M03_DeviceSearchTest extends ApiTest {

  @Test
  void filtersByTextCategoryAndAvailability() {
    long asta = insertEmployee("Asta Demo");
    insertDevice("NTL-1", "Dell XPS 13", "LAPTOP", "AVAILABLE");
    insertDevice("NTL-2", "Dell UltraSharp 27", "MONITOR", "AVAILABLE");
    long macbook = insertDevice("NTL-3", "MacBook Pro 14", "LAPTOP", "AVAILABLE");
    insertDevice("NTL-4", "Lenovo ThinkPad T14", "LAPTOP", "AVAILABLE");
    insertDevice("NTL-5", "LG 32UN880", "MONITOR", "MAINTENANCE");
    insertActiveLoan(macbook, asta, NOW.plus(Duration.ofDays(7)));

    assertThat(names("/api/devices?q=DELL"))
        .containsExactlyInAnyOrder("Dell XPS 13", "Dell UltraSharp 27");
    assertThat(names("/api/devices?q=ntl-3")).containsExactly("MacBook Pro 14");
    assertThat(names("/api/devices?category=LAPTOP"))
        .containsExactlyInAnyOrder("Dell XPS 13", "MacBook Pro 14", "Lenovo ThinkPad T14");
    assertThat(names("/api/devices?category=LAPTOP&available=true"))
        .containsExactlyInAnyOrder("Dell XPS 13", "Lenovo ThinkPad T14");
    assertThat(names("/api/devices?available=false"))
        .containsExactlyInAnyOrder("MacBook Pro 14", "LG 32UN880");
    assertThat(names("/api/devices?q=dell&category=MONITOR&available=true"))
        .containsExactly("Dell UltraSharp 27");
  }

  @Test
  void pagesThroughTheResults() {
    for (int i = 0; i < 25; i++) {
      insertDevice("Laptop " + i);
    }

    Response page = api.get("/api/devices?page=2&size=10").expectStatus(200);

    assertThat((List<Object>) page.json("$.items")).hasSize(5);
    assertThat(page.longAt("$.page")).isEqualTo(2);
    assertThat(page.longAt("$.size")).isEqualTo(10);
    assertThat(page.longAt("$.totalElements")).isEqualTo(25);
    assertThat(page.longAt("$.totalPages")).isEqualTo(3);
  }

  @Test
  void pageSizeIsCappedAt50() {
    for (int i = 0; i < 60; i++) {
      insertDevice("Laptop " + i);
    }

    Response page = api.get("/api/devices?size=200").expectStatus(200);

    assertThat((List<Object>) page.json("$.items")).hasSize(50);
    assertThat(page.longAt("$.size")).isEqualTo(50);
    assertThat(page.longAt("$.totalPages")).isEqualTo(2);
  }

  @Test
  void sortsByNameOrTagInAStableOrder() {
    long b1 = insertDevice("NTL-30", "Bravo", "LAPTOP", "AVAILABLE");
    long a = insertDevice("NTL-10", "Alpha", "LAPTOP", "AVAILABLE");
    long b2 = insertDevice("NTL-20", "Bravo", "LAPTOP", "AVAILABLE");
    long c = insertDevice("NTL-40", "Charlie", "LAPTOP", "AVAILABLE");

    assertThat(ids("/api/devices?sort=name,asc")).containsExactly(a, b1, b2, c);
    assertThat(ids("/api/devices?sort=name,desc")).containsExactly(c, b1, b2, a);
    assertThat(ids("/api/devices?sort=inventoryTag,asc")).containsExactly(a, b2, b1, c);
    assertThat(ids("/api/devices?sort=inventoryTag,desc")).containsExactly(c, b1, b2, a);
  }

  @Test
  void unknownSortFieldsAreRejected() {
    api.get("/api/devices?sort=password,asc").expectProblem(400, "VALIDATION_FAILED");
    api.get("/api/devices?sort=name,sideways").expectProblem(400, "VALIDATION_FAILED");
  }

  @Test
  void onlyOnePageIsLoadedFromTheDatabase() {
    long asta = insertEmployee("Asta Demo");
    for (int i = 0; i < 60; i++) {
      long device = insertDevice("Laptop " + i);
      if (i % 2 == 0) {
        insertActiveLoan(device, asta, NOW.plus(Duration.ofDays(7)));
      }
    }

    Measurement search = measure(() -> api.get("/api/devices?size=10&sort=name,asc"));

    search.response().expectStatus(200);
    assertThat((List<Object>) search.response().json("$.items")).hasSize(10);
    assertThat(search.statements()).as("SQL statements").isLessThanOrEqualTo(3);
    assertThat(search.devicesLoaded()).as("device entities loaded").isLessThanOrEqualTo(10);
    assertThat(search.loansLoaded()).as("loan entities loaded").isLessThanOrEqualTo(10);
  }

  private List<String> names(String url) {
    return api.get(url).expectStatus(200).json("$.items[*].name");
  }

  private List<Long> ids(String url) {
    List<Number> ids = api.get(url).expectStatus(200).json("$.items[*].id");
    return ids.stream().map(Number::longValue).toList();
  }
}
