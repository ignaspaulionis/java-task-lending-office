package com.lendingdesk.persistence.adapter;

import com.lendingdesk.core.domain.WaitlistEntry;
import com.lendingdesk.core.model.WaitlistPosition;
import com.lendingdesk.core.port.WaitlistRepository;
import com.lendingdesk.persistence.jpa.JpaWaitlistRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WaitlistRepositoryAdapter implements WaitlistRepository {

  private final JpaWaitlistRepository jpa;

  public WaitlistRepositoryAdapter(JpaWaitlistRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<WaitlistEntry> findByDevice(Long deviceId) {
    return jpa.findByDeviceIdOrderByCreatedAtAscIdAsc(deviceId);
  }

  @Override
  public List<WaitlistEntry> findByEmployee(Long employeeId) {
    return jpa.findByEmployeeIdOrderByIdAsc(employeeId);
  }

  @Override
  public List<WaitlistPosition> findPositionsOfEmployee(Long employeeId) {
    return jpa.findPositionsOfEmployee(employeeId).stream()
        .map(
            row ->
                new WaitlistPosition(
                    row.getDeviceId(), row.getDeviceName(), row.getPosition().intValue()))
        .toList();
  }

  @Override
  public Optional<WaitlistEntry> findByDeviceAndEmployee(Long deviceId, Long employeeId) {
    return jpa.findByDeviceIdAndEmployeeId(deviceId, employeeId);
  }

  @Override
  public WaitlistEntry save(WaitlistEntry entry) {
    return jpa.save(entry);
  }

  @Override
  public void delete(WaitlistEntry entry) {
    jpa.delete(entry);
  }
}
