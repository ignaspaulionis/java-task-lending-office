package com.lendingdesk.api.controller;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  @GetMapping("/top-devices")
  public Object topDevices(@RequestParam LocalDate from, @RequestParam LocalDate to) {
    throw new UnsupportedOperationException("The top devices report is not implemented yet");
  }
}
