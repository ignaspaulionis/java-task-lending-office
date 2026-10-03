package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.WaitlistEntry;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository {
  /** The queue of one device, first in line first. */
  List<WaitlistEntry> findByDevice(Long deviceId);

  List<WaitlistEntry> findByEmployee(Long employeeId);

  Optional<WaitlistEntry> findByDeviceAndEmployee(Long deviceId, Long employeeId);

  WaitlistEntry save(WaitlistEntry entry);

  void delete(WaitlistEntry entry);
}
