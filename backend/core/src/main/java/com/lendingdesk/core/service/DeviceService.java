package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.DeviceStatus;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.DeviceListItem;
import com.lendingdesk.core.model.DeviceSearch;
import com.lendingdesk.core.model.PageResult;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.util.Set;

public class DeviceService {

  private static final int MAX_PAGE_SIZE = 50;
  private static final Set<String> SORT_FIELDS = Set.of("name", "inventoryTag");

  private final DeviceRepository devices;
  private final LoanRepository loans;
  private final WaitlistRepository waitlist;

  public DeviceService(
      DeviceRepository devices, LoanRepository loans, WaitlistRepository waitlist) {
    this.devices = devices;
    this.loans = loans;
    this.waitlist = waitlist;
  }

  /** One page of devices matching the search; ties in the sort order are broken by id. */
  @Transactional
  public PageResult<DeviceListItem> search(DeviceSearch search) {
    if (!SORT_FIELDS.contains(search.sortField())) {
      throw new LendingException(ErrorCode.VALIDATION_FAILED, "Cannot sort by " + search.sortField());
    }
    String q = search.q() == null || search.q().isBlank() ? null : search.q().trim();
    int size = Math.clamp(search.size(), 1, MAX_PAGE_SIZE);
    int page = Math.max(search.page(), 0);
    return devices.search(
        new DeviceSearch(
            q, search.category(), search.available(), page, size, search.sortField(),
            search.ascending()));
  }

  @Transactional
  public DeviceListItem get(Long id) {
    return toListItem(find(id));
  }

  @Transactional
  public DeviceListItem create(String inventoryTag, String name, String category) {
    if (devices.existsByInventoryTag(inventoryTag)) {
      throw new LendingException(ErrorCode.DUPLICATE_INVENTORY_TAG);
    }
    return toListItem(devices.save(new Device(inventoryTag, name, category)));
  }

  /**
   * Updates the mutable fields; the inventory tag and category never change. A device on loan
   * cannot be retired, and retiring a device clears its waitlist.
   */
  @Transactional
  public DeviceListItem update(Long id, String name, DeviceStatus status) {
    Device device = find(id);
    if (status == DeviceStatus.RETIRED && device.getStatus() != DeviceStatus.RETIRED) {
      if (loans.findActiveByDevice(id).isPresent()) {
        throw new LendingException(ErrorCode.DEVICE_ON_LOAN);
      }
      waitlist.findByDevice(id).forEach(waitlist::delete);
    }
    device.setName(name);
    device.setStatus(status);
    return toListItem(devices.save(device));
  }

  private Device find(Long id) {
    return devices.findById(id).orElseThrow(() -> new LendingException(ErrorCode.DEVICE_NOT_FOUND));
  }

  private DeviceListItem toListItem(Device device) {
    Long loanedTo =
        loans.findActiveByDevice(device.getId()).map(loan -> loan.getEmployee().getId()).orElse(null);
    return new DeviceListItem(device, loanedTo);
  }
}
