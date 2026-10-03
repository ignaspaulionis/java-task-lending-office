package com.lendingdesk.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;

/** An HTTP response with helpers for reading its JSON body. */
public record Response(int status, String body) {

  public <T> T json(String path) {
    return JsonPath.read(body, path);
  }

  public long longAt(String path) {
    Object value = JsonPath.read(body, path);
    assertThat(value).as("number at %s, body: %s", path, body).isInstanceOf(Number.class);
    return ((Number) value).longValue();
  }

  public long id() {
    return longAt("$.id");
  }

  public String code() {
    return json("$.code");
  }

  public Response expectStatus(int expected) {
    assertThat(status).as("HTTP status, body: %s", body).isEqualTo(expected);
    return this;
  }

  public Response expectProblem(int expectedStatus, String expectedCode) {
    expectStatus(expectedStatus);
    assertThat(code()).as("error code, body: %s", body).isEqualTo(expectedCode);
    return this;
  }
}
