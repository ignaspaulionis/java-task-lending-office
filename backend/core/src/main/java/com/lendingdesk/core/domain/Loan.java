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
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "loans")
public class Loan {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "device_id")
  private Device device;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id")
  private Employee employee;

  @Column(name = "borrowed_at", nullable = false)
  private Instant borrowedAt;

  @Column(name = "due_at", nullable = false)
  private Instant dueAt;

  @Column(name = "returned_at")
  private Instant returnedAt;

  @Column(nullable = false)
  private boolean extended;

  protected Loan() {}

  public Loan(Device device, Employee employee, Instant borrowedAt, Instant dueAt) {
    this.device = device;
    this.employee = employee;
    this.borrowedAt = borrowedAt;
    this.dueAt = dueAt;
  }

  public boolean isActive() {
    return returnedAt == null;
  }

  public void markReturned(Instant when) {
    this.returnedAt = when;
  }

  /** Moves the due date later and remembers that the loan was extended. */
  public void extend(Duration by) {
    this.dueAt = dueAt.plus(by);
    this.extended = true;
  }

  public boolean isExtended() {
    return extended;
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

  public Instant getBorrowedAt() {
    return borrowedAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }
}
