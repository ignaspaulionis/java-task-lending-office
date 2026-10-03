package com.lendingdesk;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** What already works in the starter code. These tests must stay green. */
class BaselineApiTest extends ApiTest {

  @Test
  void healthEndpointReportsOk() {
    Response response = api.get("/api/health").expectStatus(200);
    assertThat((String) response.json("$.status")).isEqualTo("ok");
  }

  @Test
  void employeesCanBeCreatedListedAndUpdated() {
    long id = employee("Asta Demo");

    Response list = api.get("/api/employees").expectStatus(200);
    assertThat((List<String>) list.json("$[*].name")).containsExactly("Asta Demo");
    assertThat((Boolean) list.json("$[0].active")).isTrue();

    api.put(
            "/api/employees/" + id,
            Map.of("name", "Asta Renamed", "email", "asta@example.com", "active", false))
        .expectStatus(200);
    Response updated = api.get("/api/employees").expectStatus(200);
    assertThat((String) updated.json("$[0].name")).isEqualTo("Asta Renamed");
    assertThat((Boolean) updated.json("$[0].active")).isFalse();
  }

  @Test
  void devicesCanBeCreatedReadAndUpdated() {
    long id = device("NTL-0001", "Dell XPS 13", "LAPTOP");

    Response created = getDevice(id).expectStatus(200);
    assertThat((String) created.json("$.inventoryTag")).isEqualTo("NTL-0001");
    assertThat((String) created.json("$.status")).isEqualTo("AVAILABLE");
    assertThat((Boolean) created.json("$.available")).isTrue();
    assertThat((Object) created.json("$.loanedTo")).isNull();

    api.put("/api/devices/" + id, Map.of("name", "Dell XPS 15", "status", "MAINTENANCE"))
        .expectStatus(200);
    Response updated = getDevice(id).expectStatus(200);
    assertThat((String) updated.json("$.name")).isEqualTo("Dell XPS 15");
    assertThat((String) updated.json("$.status")).isEqualTo("MAINTENANCE");
    assertThat((Boolean) updated.json("$.available")).isFalse();
  }

  @Test
  void invalidOrDuplicateDevicesAreRejected() {
    device("NTL-0001", "Dell XPS 13", "LAPTOP");

    api.post(
            "/api/devices",
            Map.of("inventoryTag", "NTL-0001", "name", "Another", "category", "LAPTOP"))
        .expectProblem(409, "DUPLICATE_INVENTORY_TAG");
    api.post(
            "/api/devices", Map.of("inventoryTag", "ABC-1", "name", "Bad tag", "category", "LAPTOP"))
        .expectProblem(400, "VALIDATION_FAILED");
    getDevice(999).expectProblem(404, "DEVICE_NOT_FOUND");
  }

  @Test
  void deviceListIsReturnedAsAPage() {
    device("Dell XPS 13");
    device("MacBook Pro 14");
    device("Lenovo ThinkPad T14");

    Response page = api.get("/api/devices").expectStatus(200);

    assertThat((List<Object>) page.json("$.items")).hasSize(3);
    assertThat(page.longAt("$.totalElements")).isEqualTo(3);
    assertThat(page.longAt("$.totalPages")).isEqualTo(1);
    assertThat(page.longAt("$.page")).isZero();
    assertThat(page.longAt("$.size")).isEqualTo(20);
  }

  @Test
  void aDeviceCanBeBorrowedAndReturned() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");

    Response loan = borrow(laptop, asta).expectStatus(201);
    assertThat(loan.longAt("$.deviceId")).isEqualTo(laptop);
    assertThat(loan.longAt("$.employeeId")).isEqualTo(asta);
    assertThat((String) loan.json("$.deviceName")).isEqualTo("Dell XPS 13");
    assertThat((String) loan.json("$.borrowedAt")).isEqualTo("2026-03-10T10:00:00Z");
    assertThat((String) loan.json("$.dueAt")).isEqualTo("2026-03-24T10:00:00Z");
    assertThat((Object) loan.json("$.returnedAt")).isNull();
    assertThat((Boolean) loan.json("$.overdue")).isFalse();
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(asta);

    clock.advance(Duration.ofHours(1));
    Response returned = returnLoan(loan.id(), asta).expectStatus(200);
    assertThat((String) returned.json("$.loan.returnedAt")).isEqualTo("2026-03-10T11:00:00Z");
    assertThat((Object) returned.json("$.nextEmployeeId")).isNull();
    assertThat((Boolean) getDevice(laptop).json("$.available")).isTrue();
  }

  @Test
  void borrowingSomethingUnknownFails() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");

    borrow(laptop, 999).expectProblem(404, "EMPLOYEE_NOT_FOUND");
    borrow(999, asta).expectProblem(404, "DEVICE_NOT_FOUND");
    api.post("/api/loans", Map.of("deviceId", laptop)).expectProblem(400, "VALIDATION_FAILED");
  }

  @Test
  void loansCanBeFilteredByEmployeeAndState() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long first = borrowOk(device("Dell XPS 13"), asta);
    long second = borrowOk(device("MacBook Pro 14"), jonas);
    returnLoan(first, asta).expectStatus(200);

    assertThat(ids(api.get("/api/loans"))).containsExactly(first, second);
    assertThat(ids(api.get("/api/loans?active=true"))).containsExactly(second);
    assertThat(ids(api.get("/api/loans?active=false"))).containsExactly(first);
    assertThat(ids(api.get("/api/loans?employeeId=" + asta))).containsExactly(first);
  }

  @Test
  void anEmployeeCanJoinAndLeaveAWaitlist() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);

    Response joined = joinWaitlist(laptop, jonas).expectStatus(201);
    assertThat(joined.longAt("$.position")).isEqualTo(1);
    assertThat((List<Object>) summary(jonas).json("$.waitlist")).hasSize(1);

    leaveWaitlist(laptop, jonas).expectStatus(204);
    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
  }

  @Test
  void summaryShowsActiveLoans() {
    long asta = employee("Asta Demo");
    borrowOk(device("Dell XPS 13"), asta);

    Response summary = summary(asta).expectStatus(200);

    assertThat((String) summary.json("$.employee.name")).isEqualTo("Asta Demo");
    assertThat((List<String>) summary.json("$.loans[*].deviceName"))
        .containsExactly("Dell XPS 13");
  }

  @Test
  void loansPastTheirDueDateAreListedAsOverdue() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);

    clock.advance(Duration.ofDays(20));

    Response overdue = api.get("/api/loans/overdue").expectStatus(200);
    assertThat(ids(overdue)).containsExactly(loan);
    assertThat((Boolean) overdue.json("$[0].overdue")).isTrue();
  }

  private static List<Long> ids(Response response) {
    List<Number> ids = response.expectStatus(200).json("$[*].id");
    return ids.stream().map(Number::longValue).toList();
  }
}
