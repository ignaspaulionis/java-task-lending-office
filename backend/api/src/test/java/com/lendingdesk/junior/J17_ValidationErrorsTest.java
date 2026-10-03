package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.api.dto.CreateDeviceRequest;
import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import jakarta.validation.Constraint;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** J17 - Validation errors per field. See JuniorTasks.md. */
class J17_ValidationErrorsTest extends ApiTest {

  @Test
  void everyInvalidFieldIsListedSortedByField() {
    Response response =
        api.post(
                "/api/devices",
                Map.of("inventoryTag", "ABC-1", "name", "", "category", "LAPTOP"))
            .expectProblem(400, "VALIDATION_FAILED");

    List<Map<String, Object>> errors = errors(response);
    assertThat(errors).extracting(e -> e.get("field")).containsExactly("inventoryTag", "name");
    assertThat(errors).allSatisfy(e -> assertThat((String) e.get("message")).isNotBlank());
  }

  @Test
  void theInventoryTagHasItsOwnMessage() {
    for (String tag : List.of("ABC-1", "NTL-")) {
      Response response =
          api.post(
                  "/api/devices",
                  Map.of("inventoryTag", tag, "name", "Dell XPS 13", "category", "LAPTOP"))
              .expectProblem(400, "VALIDATION_FAILED");

      assertThat(errors(response))
          .as("errors for tag %s", tag)
          .containsExactly(Map.of("field", "inventoryTag", "message", "must start with NTL-"));
    }
  }

  @Test
  void missingFieldsAreListedForEveryRequest() {
    Response response = api.post("/api/loans", Map.of()).expectProblem(400, "VALIDATION_FAILED");

    assertThat(errors(response))
        .extracting(e -> e.get("field"))
        .containsExactly("deviceId", "employeeId");
  }

  @Test
  void anUnreadableBodyHasNoFieldErrors() {
    Response response =
        api.post("/api/loans", "this is not json").expectProblem(400, "VALIDATION_FAILED");

    assertThat(errors(response)).isEmpty();
  }

  /** The {@code errors} list of a problem response; fails clearly if it is missing. */
  private static List<Map<String, Object>> errors(Response response) {
    Map<String, Object> body = response.json("$");
    assertThat(body).as("problem response body").containsKey("errors");
    return response.json("$.errors");
  }

  @Test
  void theInventoryTagRuleIsACustomConstraintAnnotation() throws Exception {
    // A constraint on a record component ends up on the field and/or the accessor method.
    List<Annotation> annotations =
        Stream.concat(
                Arrays.stream(
                    CreateDeviceRequest.class.getDeclaredField("inventoryTag").getAnnotations()),
                Arrays.stream(CreateDeviceRequest.class.getMethod("inventoryTag").getAnnotations()))
            .toList();

    assertThat(annotations)
        .as("annotations on CreateDeviceRequest.inventoryTag")
        .anySatisfy(
            annotation -> {
              assertThat(annotation.annotationType().getSimpleName()).isEqualTo("InventoryTag");
              assertThat(annotation.annotationType().isAnnotationPresent(Constraint.class)).isTrue();
            });
  }
}
