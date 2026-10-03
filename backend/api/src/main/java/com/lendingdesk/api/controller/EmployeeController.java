package com.lendingdesk.api.controller;

import com.lendingdesk.api.ApiMapper;
import com.lendingdesk.api.dto.CreateEmployeeRequest;
import com.lendingdesk.api.dto.EmployeeResponse;
import com.lendingdesk.api.dto.EmployeeSummaryResponse;
import com.lendingdesk.api.dto.LoanHistoryEntryResponse;
import com.lendingdesk.api.dto.UpdateEmployeeRequest;
import com.lendingdesk.api.dto.WaitlistPositionResponse;
import com.lendingdesk.core.model.EmployeeSummary;
import com.lendingdesk.core.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

  private final EmployeeService employees;
  private final ApiMapper mapper;

  public EmployeeController(EmployeeService employees, ApiMapper mapper) {
    this.employees = employees;
    this.mapper = mapper;
  }

  @GetMapping
  public List<EmployeeResponse> list() {
    return employees.list().stream().map(mapper::toResponse).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EmployeeResponse create(@RequestBody @Valid CreateEmployeeRequest request) {
    return mapper.toResponse(employees.create(request.name(), request.email()));
  }

  @PutMapping("/{id}")
  public EmployeeResponse update(
      @PathVariable Long id, @RequestBody @Valid UpdateEmployeeRequest request) {
    return mapper.toResponse(
        employees.update(id, request.name(), request.email(), request.active()));
  }

  @GetMapping("/{id}/loan-history")
  public List<LoanHistoryEntryResponse> loanHistory(@PathVariable Long id) {
    return employees.loanHistory(id).stream()
        .map(
            loan ->
                new LoanHistoryEntryResponse(
                    loan.getId(),
                    loan.getDevice().getId(),
                    loan.getDevice().getName(),
                    loan.getBorrowedAt(),
                    loan.getReturnedAt()))
        .toList();
  }

  @GetMapping("/{id}/summary")
  public EmployeeSummaryResponse summary(@PathVariable Long id) {
    EmployeeSummary summary = employees.summary(id);
    return new EmployeeSummaryResponse(
        mapper.toResponse(summary.employee()),
        summary.activeLoans().stream().map(mapper::toResponse).toList(),
        summary.waitlist().stream()
            .map(p -> new WaitlistPositionResponse(p.deviceId(), p.deviceName(), p.position()))
            .toList());
  }
}
