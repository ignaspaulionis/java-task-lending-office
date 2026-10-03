package com.lendingdesk.api.dto;

import java.util.List;

public record ImportReportResponse(int imported, List<Error> errors) {

  public record Error(int line, String reason) {}
}
