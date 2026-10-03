package com.lendingdesk.persistence.adapter;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.persistence.jpa.JpaDeviceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepositoryAdapter implements DeviceRepository {

  private final JpaDeviceRepository jpa;

  public DeviceRepositoryAdapter(JpaDeviceRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Device> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public List<Device> findAll() {
    return jpa.findAll();
  }

  @Override
  public boolean existsByInventoryTag(String inventoryTag) {
    return jpa.existsByInventoryTag(inventoryTag);
  }

  @Override
  public Device save(Device device) {
    return jpa.save(device);
  }
}
