package com.lendingdesk.core.port;

import com.lendingdesk.core.domain.WaitlistEntry;
import com.lendingdesk.core.model.WaitlistPosition;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository {
  /** The queue of one device, first in line first. */
  List<WaitlistEntry> findByDevice(Long deviceId);

  List<WaitlistEntry> findByEmployee(Long employeeId);

  /** The employee's position in every waitlist they are on, in the order they joined. */
  List<WaitlistPosition> findPositionsOfEmployee(Long employeeId);

  Optional<WaitlistEntry> findByDeviceAndEmployee(Long deviceId, Long employeeId);

  WaitlistEntry save(WaitlistEntry entry);

  void delete(WaitlistEntry entry);
}
