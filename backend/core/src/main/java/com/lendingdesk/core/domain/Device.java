package com.lendingdesk.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class Device {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "inventory_tag", nullable = false, unique = true)
  private String inventoryTag;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeviceStatus status = DeviceStatus.AVAILABLE;

  protected Device() {}

  public Device(String inventoryTag, String name, String category) {
    this.inventoryTag = inventoryTag;
    this.name = name;
    this.category = category;
  }

  public Long getId() {
    return id;
  }

  public String getInventoryTag() {
    return inventoryTag;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public DeviceStatus getStatus() {
    return status;
  }

  public void setStatus(DeviceStatus status) {
    this.status = status;
  }
}
