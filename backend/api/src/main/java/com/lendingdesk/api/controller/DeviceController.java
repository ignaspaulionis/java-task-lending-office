package com.lendingdesk.api.controller;

import com.lendingdesk.api.ApiMapper;
import com.lendingdesk.api.dto.CreateDeviceRequest;
import com.lendingdesk.api.dto.DeviceResponse;
import com.lendingdesk.api.dto.ImportReportResponse;
import com.lendingdesk.api.dto.PageResponse;
import com.lendingdesk.api.dto.UpdateDeviceRequest;
import com.lendingdesk.core.model.DeviceListItem;
import com.lendingdesk.core.model.DeviceSearch;
import com.lendingdesk.core.model.ImportReport;
import com.lendingdesk.core.model.PageResult;
import com.lendingdesk.core.service.DeviceImportService;
import com.lendingdesk.core.service.DeviceService;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

  private final DeviceService devices;
  private final DeviceImportService imports;
  private final ApiMapper mapper;

  public DeviceController(DeviceService devices, DeviceImportService imports, ApiMapper mapper) {
    this.devices = devices;
    this.imports = imports;
    this.mapper = mapper;
  }

  /**
   * Searches devices.
   *
   * @param sort {@code field,direction}, for example {@code name,asc}
   */
  @GetMapping
  public PageResponse<DeviceResponse> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Boolean available,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "name,asc") String sort) {
    String[] sortParts = sort.split(",", 2);
    String direction = sortParts.length < 2 ? "asc" : sortParts[1].toLowerCase(Locale.ROOT);
    if (!direction.equals("asc") && !direction.equals("desc")) {
      throw new LendingException(ErrorCode.VALIDATION_FAILED, "Unknown sort direction " + direction);
    }
    boolean ascending = direction.equals("asc");
    PageResult<DeviceListItem> result =
        devices.search(
            new DeviceSearch(q, category, available, page, size, sortParts[0], ascending));
    return new PageResponse<>(
        result.items().stream().map(mapper::toResponse).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages());
  }

  @GetMapping("/{id}")
  public DeviceResponse get(@PathVariable Long id) {
    return mapper.toResponse(devices.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DeviceResponse create(@RequestBody @Valid CreateDeviceRequest request) {
    return mapper.toResponse(
        devices.create(request.inventoryTag(), request.name(), request.category()));
  }

  @PutMapping("/{id}")
  public DeviceResponse update(
      @PathVariable Long id, @RequestBody @Valid UpdateDeviceRequest request) {
    return mapper.toResponse(devices.update(id, request.name(), request.status()));
  }

  @PostMapping("/import")
  public ImportReportResponse importCsv(@RequestPart("file") MultipartFile file)
      throws IOException {
    ImportReport report = imports.importCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
    return new ImportReportResponse(
        report.imported(),
        report.errors().stream()
            .map(e -> new ImportReportResponse.Error(e.line(), e.reason().name()))
            .toList());
  }
}
