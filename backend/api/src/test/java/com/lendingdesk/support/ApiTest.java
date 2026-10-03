package com.lendingdesk.support;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.domain.WaitlistEntry;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for API tests: a running application on a random port, a clean database before
 * every test, a clock fixed at {@link #NOW}, and helpers for setting up data.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "lending.seed.enabled=false",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
    })
@Import(TestInfrastructure.class)
public abstract class ApiTest {

  /** Tuesday 2026-03-10, 12:00 in Vilnius. */
  protected static final Instant NOW = Instant.parse("2026-03-10T10:00:00Z");

  private static final AtomicInteger TAG_SEQUENCE = new AtomicInteger(1000);

  @LocalServerPort private int port;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected MutableClock clock;
  @Autowired private EntityManagerFactory entityManagerFactory;

  protected ApiClient api;

  @BeforeEach
  void resetState() {
    jdbc.execute(
        "TRUNCATE TABLE waitlist_entries, loans, devices, employees RESTART IDENTITY CASCADE");
    clock.set(NOW);
    api = new ApiClient("http://localhost:" + port);
  }

  // ---- setup through the API ----

  protected long employee(String name) {
    String email = name.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com";
    return api.post("/api/employees", Map.of("name", name, "email", email))
        .expectStatus(201)
        .id();
  }

  /** Deactivates an employee through the API, keeping their name and email. */
  protected void deactivate(long employeeId) {
    updateEmployee(employeeId, false).expectStatus(200);
  }

  protected Response updateEmployee(long employeeId, boolean active) {
    Response employees = api.get("/api/employees").expectStatus(200);
    List<String> names = employees.json("$[?(@.id == " + employeeId + ")].name");
    List<String> emails = employees.json("$[?(@.id == " + employeeId + ")].email");
    return api.put(
        "/api/employees/" + employeeId,
        Map.of("name", names.getFirst(), "email", emails.getFirst(), "active", active));
  }

  /** Marks an employee inactive directly in the database, bypassing every rule. */
  protected void markInactiveInDatabase(long employeeId) {
    jdbc.update("UPDATE employees SET active = FALSE WHERE id = ?", employeeId);
  }

  protected long device(String name) {
    return device("NTL-" + TAG_SEQUENCE.incrementAndGet(), name, "LAPTOP");
  }

  protected long device(String inventoryTag, String name, String category) {
    return api.post(
            "/api/devices",
            Map.of("inventoryTag", inventoryTag, "name", name, "category", category))
        .expectStatus(201)
        .id();
  }

  protected void setStatus(long deviceId, String status) {
    api.put("/api/devices/" + deviceId, Map.of("name", "Device " + deviceId, "status", status))
        .expectStatus(200);
  }

  protected Response borrow(long deviceId, long employeeId) {
    return api.post("/api/loans", Map.of("deviceId", deviceId, "employeeId", employeeId));
  }

  /** Borrows and returns the new loan id; fails the test if borrowing fails. */
  protected long borrowOk(long deviceId, long employeeId) {
    return borrow(deviceId, employeeId).expectStatus(201).id();
  }

  protected Response returnLoan(long loanId, long employeeId) {
    return api.post("/api/loans/" + loanId + "/return", Map.of("employeeId", employeeId));
  }

  protected Response joinWaitlist(long deviceId, long employeeId) {
    return api.post("/api/devices/" + deviceId + "/waitlist", Map.of("employeeId", employeeId));
  }

  protected Response leaveWaitlist(long deviceId, long employeeId) {
    return api.delete("/api/devices/" + deviceId + "/waitlist/" + employeeId);
  }

  protected Response summary(long employeeId) {
    return api.get("/api/employees/" + employeeId + "/summary");
  }

  protected Response getDevice(long deviceId) {
    return api.get("/api/devices/" + deviceId);
  }

  // ---- bulk setup directly in the database (fast, bypasses the rules) ----

  protected long insertEmployee(String name) {
    return jdbc.queryForObject(
        "INSERT INTO employees (name, email, active) VALUES (?, ?, TRUE) RETURNING id",
        Long.class,
        name,
        name.toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com");
  }

  protected long insertDevice(String inventoryTag, String name, String category, String status) {
    return jdbc.queryForObject(
        "INSERT INTO devices (inventory_tag, name, category, status) VALUES (?, ?, ?, ?)"
            + " RETURNING id",
        Long.class,
        inventoryTag,
        name,
        category,
        status);
  }

  protected long insertDevice(String name) {
    return insertDevice("NTL-" + TAG_SEQUENCE.incrementAndGet(), name, "LAPTOP", "AVAILABLE");
  }

  protected long insertLoan(
      long deviceId, long employeeId, Instant borrowedAt, Instant dueAt, Instant returnedAt) {
    return jdbc.queryForObject(
        "INSERT INTO loans (device_id, employee_id, borrowed_at, due_at, returned_at)"
            + " VALUES (?, ?, ?, ?, ?) RETURNING id",
        Long.class,
        deviceId,
        employeeId,
        borrowedAt.atOffset(ZoneOffset.UTC),
        dueAt.atOffset(ZoneOffset.UTC),
        returnedAt == null ? null : returnedAt.atOffset(ZoneOffset.UTC));
  }

  protected long insertActiveLoan(long deviceId, long employeeId, Instant dueAt) {
    return insertLoan(deviceId, employeeId, dueAt.minus(Duration.ofDays(14)), dueAt, null);
  }

  protected long insertWaitlistEntry(long deviceId, long employeeId, Instant createdAt) {
    return jdbc.queryForObject(
        "INSERT INTO waitlist_entries (device_id, employee_id, created_at) VALUES (?, ?, ?)"
            + " RETURNING id",
        Long.class,
        deviceId,
        employeeId,
        createdAt.atOffset(ZoneOffset.UTC));
  }

  protected int activeLoansOfDevice(long deviceId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM loans WHERE device_id = ? AND returned_at IS NULL",
        Integer.class,
        deviceId);
  }

  protected int activeLoansOfEmployee(long employeeId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM loans WHERE employee_id = ? AND returned_at IS NULL",
        Integer.class,
        employeeId);
  }

  // ---- measuring database work ----

  /** Runs one request and records how much database work the application did for it. */
  protected Measurement measure(Supplier<Response> request) {
    Statistics statistics =
        entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    Response response = request.get();
    return new Measurement(
        response,
        statistics.getPrepareStatementCount(),
        loads(statistics, Loan.class),
        loads(statistics, Device.class),
        loads(statistics, Employee.class),
        loads(statistics, WaitlistEntry.class));
  }

  private static long loads(Statistics statistics, Class<?> entity) {
    return statistics.getEntityStatistics(entity.getName()).getLoadCount();
  }

  /**
   * Database work done for one request.
   *
   * @param statements SQL statements executed
   * @param loansLoaded loan rows loaded into memory as entities (devicesLoaded etc. likewise)
   */
  public record Measurement(
      Response response,
      long statements,
      long loansLoaded,
      long devicesLoaded,
      long employeesLoaded,
      long waitlistEntriesLoaded) {}
}
