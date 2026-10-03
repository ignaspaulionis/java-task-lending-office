package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** J13 - Unique email enforced by the database. See JuniorTasks.md. */
class J13_UniqueEmailTest extends ApiTest {

  @Test
  void aDuplicateEmailIsRejected() {
    create("Asta Demo", "asta@example.com").expectStatus(201);

    create("Asta Again", "asta@example.com").expectProblem(409, "DUPLICATE_EMAIL");
  }

  @Test
  void emailsAreComparedAndStoredInLowercase() {
    String stored = create("Asta Demo", "Asta@Example.COM").expectStatus(201).json("$.email");

    assertThat(stored).isEqualTo("asta@example.com");
    create("Asta Again", "asta@EXAMPLE.com").expectProblem(409, "DUPLICATE_EMAIL");
  }

  @Test
  void anEmployeeCannotTakeSomeoneElsesEmail() {
    create("Asta Demo", "asta@example.com").expectStatus(201);
    long jonas = create("Jonas Demo", "jonas@example.com").expectStatus(201).id();

    api.put(
            "/api/employees/" + jonas,
            Map.of("name", "Jonas Demo", "email", "ASTA@example.com", "active", true))
        .expectProblem(409, "DUPLICATE_EMAIL");
  }

  @Test
  void anEmployeeCanKeepTheirOwnEmailWhenUpdated() {
    long asta = create("Asta Demo", "asta@example.com").expectStatus(201).id();

    api.put(
            "/api/employees/" + asta,
            Map.of("name", "Asta Renamed", "email", "asta@example.com", "active", true))
        .expectStatus(200);
  }

  @Test
  void theDatabaseRejectsDuplicatesInAnyCase() {
    jdbc.update("INSERT INTO employees (name, email) VALUES ('Asta Demo', 'asta@example.com')");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO employees (name, email) VALUES ('Asta Again', 'ASTA@example.com')"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Response create(String name, String email) {
    return api.post("/api/employees", Map.of("name", name, "email", email));
  }
}
