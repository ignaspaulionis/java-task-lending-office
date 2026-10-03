package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.WaitlistEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaWaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

  List<WaitlistEntry> findByDeviceIdOrderByCreatedAtAscIdAsc(Long deviceId);

  List<WaitlistEntry> findByEmployeeIdOrderByIdAsc(Long employeeId);

  Optional<WaitlistEntry> findByDeviceIdAndEmployeeId(Long deviceId, Long employeeId);
}
