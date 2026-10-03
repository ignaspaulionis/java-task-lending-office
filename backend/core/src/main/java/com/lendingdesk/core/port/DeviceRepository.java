package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.Device;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository {
  Optional<Device> findById(Long id);

  List<Device> findAll();

  boolean existsByInventoryTag(String inventoryTag);

  Device save(Device device);
}
