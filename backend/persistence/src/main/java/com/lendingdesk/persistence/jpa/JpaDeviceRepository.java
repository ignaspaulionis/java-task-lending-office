package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.Device;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface JpaDeviceRepository extends JpaRepository<Device, Long> {
  boolean existsByInventoryTag(String inventoryTag);

  @Query("select d.inventoryTag from Device d where d.inventoryTag in :tags")
  List<String> findExistingTags(Collection<String> tags);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Device d where d.id = :id")
  Optional<Device> findByIdForUpdate(Long id);
}
