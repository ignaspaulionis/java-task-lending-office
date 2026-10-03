package com.lendingdesk.api.controller;

import com.lendingdesk.api.ApiMapper;
import com.lendingdesk.api.dto.BorrowRequest;
import com.lendingdesk.api.dto.CancelLoanRequest;
import com.lendingdesk.api.dto.ExtendRequest;
import com.lendingdesk.api.dto.LoanResponse;
import com.lendingdesk.api.dto.ReminderResponse;
import com.lendingdesk.api.dto.ReturnRequest;
import com.lendingdesk.api.dto.ReturnResponse;
import com.lendingdesk.api.dto.TransferRequest;
import com.lendingdesk.core.model.ReturnResult;
import com.lendingdesk.core.service.LoanCancellationService;
import com.lendingdesk.core.service.LoanService;
import com.lendingdesk.core.service.LoanTransferService;
import com.lendingdesk.core.service.ReminderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

  private final LoanService loans;
  private final ReminderService reminders;
  private final LoanCancellationService cancellations;
  private final LoanTransferService transfers;
  private final ApiMapper mapper;

  public LoanController(
      LoanService loans,
      ReminderService reminders,
      LoanCancellationService cancellations,
      LoanTransferService transfers,
      ApiMapper mapper) {
    this.loans = loans;
    this.reminders = reminders;
    this.cancellations = cancellations;
    this.transfers = transfers;
    this.mapper = mapper;
  }

  @GetMapping
  public List<LoanResponse> list(
      @RequestParam(required = false) Long employeeId,
      @RequestParam(required = false) Boolean active) {
    return loans.list(employeeId, active).stream().map(mapper::toResponse).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LoanResponse borrow(@RequestBody @Valid BorrowRequest request) {
    return mapper.toResponse(loans.borrow(request.deviceId(), request.employeeId()));
  }

  @PostMapping("/{id}/return")
  public ReturnResponse returnLoan(
      @PathVariable Long id, @RequestBody @Valid ReturnRequest request) {
    ReturnResult result = loans.returnLoan(id, request.employeeId());
    return new ReturnResponse(mapper.toResponse(result.loan()), result.nextEmployeeId());
  }

  @PostMapping("/{id}/extend")
  public LoanResponse extend(@PathVariable Long id, @RequestBody @Valid ExtendRequest request) {
    return mapper.toResponse(loans.extend(id, request.employeeId()));
  }

  @GetMapping("/{id}/reminder")
  public ReminderResponse reminder(@PathVariable Long id) {
    return new ReminderResponse(reminders.reminder(id));
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable Long id, @RequestBody @Valid CancelLoanRequest request) {
    cancellations.cancel(id, request.employeeId());
  }

  @PostMapping("/{id}/transfer")
  @ResponseStatus(HttpStatus.CREATED)
  public LoanResponse transfer(@PathVariable Long id, @RequestBody @Valid TransferRequest request) {
    return mapper.toResponse(
        transfers.transfer(id, request.fromEmployeeId(), request.toEmployeeId()));
  }

  @GetMapping("/overdue")
  public List<LoanResponse> overdue() {
    return loans.overdue().stream().map(mapper::toResponse).toList();
  }
}
