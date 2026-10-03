package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDeviceRepository extends JpaRepository<Device, Long> {
  boolean existsByInventoryTag(String inventoryTag);
}
