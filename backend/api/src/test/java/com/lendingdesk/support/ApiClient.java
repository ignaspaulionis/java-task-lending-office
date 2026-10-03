package com.lendingdesk.support;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Calls the running application over HTTP and never throws on error statuses. */
public class ApiClient {

  private final RestClient rest;

  public ApiClient(String baseUrl) {
    this.rest = RestClient.builder().baseUrl(baseUrl).build();
  }

  /** GET with optional URI template variables, e.g. {@code get("/api/devices?q={q}", "50%")}. */
  public Response get(String path, Object... uriVariables) {
    return exchange(rest.get().uri(path, uriVariables));
  }

  public Response post(String path, Object body) {
    return exchange(rest.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body));
  }

  public Response put(String path, Object body) {
    return exchange(rest.put().uri(path).contentType(MediaType.APPLICATION_JSON).body(body));
  }

  public Response delete(String path) {
    return exchange(rest.delete().uri(path));
  }

  /** Uploads {@code content} as a multipart part named {@code file}. */
  public Response postFile(String path, String filename, String content) {
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    partHeaders.setContentDisposition(
        ContentDisposition.formData().name("file").filename(filename).build());
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new HttpEntity<>(content.getBytes(StandardCharsets.UTF_8), partHeaders));
    return exchange(rest.post().uri(path).contentType(MediaType.MULTIPART_FORM_DATA).body(parts));
  }

  private Response exchange(RestClient.RequestHeadersSpec<?> request) {
    return request.exchange(
        (req, res) ->
            new Response(
                res.getStatusCode().value(),
                new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
  }
}
