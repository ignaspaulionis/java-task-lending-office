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
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DeviceService {

  private final DeviceRepository devices;
  private final LoanRepository loans;

  public DeviceService(DeviceRepository devices, LoanRepository loans) {
    this.devices = devices;
    this.loans = loans;
  }

  /** One page of devices matching the search. */
  @Transactional
  public PageResult<DeviceListItem> search(DeviceSearch search) {
    List<DeviceListItem> matching =
        devices.findAll().stream()
            .sorted(Comparator.comparing(Device::getId))
            .map(this::toListItem)
            .filter(item -> matches(item, search))
            .toList();
    int from = Math.min(search.page() * search.size(), matching.size());
    int to = Math.min(from + search.size(), matching.size());
    int totalPages = (matching.size() + search.size() - 1) / search.size();
    return new PageResult<>(
        matching.subList(from, to), search.page(), search.size(), matching.size(), totalPages);
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

  /** Updates the mutable fields; the inventory tag and category never change. */
  @Transactional
  public DeviceListItem update(Long id, String name, DeviceStatus status) {
    Device device = find(id);
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

  private static boolean matches(DeviceListItem item, DeviceSearch search) {
    Device device = item.device();
    if (search.q() != null) {
      String q = search.q().toLowerCase(Locale.ROOT);
      boolean found =
          device.getName().toLowerCase(Locale.ROOT).contains(q)
              || device.getInventoryTag().toLowerCase(Locale.ROOT).contains(q);
      if (!found) {
        return false;
      }
    }
    if (search.category() != null && !search.category().equals(device.getCategory())) {
      return false;
    }
    return search.available() == null || search.available() == item.available();
  }
}
