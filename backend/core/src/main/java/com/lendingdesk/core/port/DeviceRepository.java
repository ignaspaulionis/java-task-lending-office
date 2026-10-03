package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.model.DeviceListItem;
import com.lendingdesk.core.model.DeviceSearch;
import com.lendingdesk.core.model.PageResult;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface DeviceRepository {
  Optional<Device> findById(Long id);

  /** Reads the row and locks it until the current transaction ends. */
  Optional<Device> findByIdForUpdate(Long id);

  boolean existsByInventoryTag(String inventoryTag);

  /** Those of the given tags that already exist, in one query. */
  Set<String> findExistingTags(Collection<String> inventoryTags);

  /**
   * One page of devices with their current holder. The search must already be validated: the
   * sort field is a {@link com.lendingdesk.core.domain.Device} property name.
   */
  PageResult<DeviceListItem> search(DeviceSearch search);

  Device save(Device device);
}
