package com.lendingdesk.api.controller;

import com.lendingdesk.api.ApiMapper;
import com.lendingdesk.api.dto.WaitlistJoinResponse;
import com.lendingdesk.api.dto.WaitlistRequest;
import com.lendingdesk.core.model.WaitlistJoinResult;
import com.lendingdesk.core.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices/{deviceId}/waitlist")
public class WaitlistController {

  private final WaitlistService waitlist;
  private final ApiMapper mapper;

  public WaitlistController(WaitlistService waitlist, ApiMapper mapper) {
    this.waitlist = waitlist;
    this.mapper = mapper;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WaitlistJoinResponse join(
      @PathVariable Long deviceId, @RequestBody @Valid WaitlistRequest request) {
    WaitlistJoinResult result = waitlist.join(deviceId, request.employeeId());
    return new WaitlistJoinResponse(
        result.position(), result.loan() == null ? null : mapper.toResponse(result.loan()));
  }

  @DeleteMapping("/{employeeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leave(@PathVariable Long deviceId, @PathVariable Long employeeId) {
    waitlist.leave(deviceId, employeeId);
  }
}
