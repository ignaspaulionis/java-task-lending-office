package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Device;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface JpaDeviceRepository extends JpaRepository<Device, Long> {
  boolean existsByInventoryTag(String inventoryTag);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Device d where d.id = :id")
  Optional<Device> findByIdForUpdate(Long id);
}
