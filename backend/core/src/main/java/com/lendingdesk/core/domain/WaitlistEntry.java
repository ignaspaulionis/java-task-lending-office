package com.lendingdesk.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One employee waiting for one device. Queue order is (createdAt, id). */
@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "device_id")
  private Device device;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id")
  private Employee employee;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected WaitlistEntry() {}

  public WaitlistEntry(Device device, Employee employee, Instant createdAt) {
    this.device = device;
    this.employee = employee;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Device getDevice() {
    return device;
  }

  public Employee getEmployee() {
    return employee;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
