package com.lendingdesk.persistence.jpa;

import com.lendingdesk.core.domain.WaitlistEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaWaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

  List<WaitlistEntry> findByDeviceIdOrderByCreatedAtAscIdAsc(Long deviceId);

  Optional<WaitlistEntry> findByDeviceIdAndEmployeeId(Long deviceId, Long employeeId);

  /** Position = 1 + number of entries for the same device that are ahead in the queue. */
  @Query(
      """
      select d.id as deviceId, d.name as deviceName,
             (select count(ahead) from WaitlistEntry ahead
              where ahead.device = w.device
                and (ahead.createdAt < w.createdAt
                     or (ahead.createdAt = w.createdAt and ahead.id < w.id))) + 1 as position
      from WaitlistEntry w join w.device d
      where w.employee.id = :employeeId
      order by w.id
      """)
  List<PositionRow> findPositionsOfEmployee(Long employeeId);

  interface PositionRow {
    Long getDeviceId();

    String getDeviceName();

    Long getPosition();
  }
}
