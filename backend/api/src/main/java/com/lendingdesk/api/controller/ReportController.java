package com.lendingdesk.api.controller;

import com.lendingdesk.api.dto.TopDeviceResponse;
import com.lendingdesk.core.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  private final ReportService reports;

  public ReportController(ReportService reports) {
    this.reports = reports;
  }

  @GetMapping("/top-devices")
  public List<TopDeviceResponse> topDevices(
      @RequestParam LocalDate from, @RequestParam LocalDate to) {
    return reports.topDevices(from, to).stream()
        .map(top -> new TopDeviceResponse(top.deviceId(), top.deviceName(), top.loanCount()))
        .toList();
  }
}
