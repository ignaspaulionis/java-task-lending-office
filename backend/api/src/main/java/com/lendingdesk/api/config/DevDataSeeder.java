package com.lendingdesk.api.config;

import com.lendingdesk.core.service.DeviceService;
import com.lendingdesk.core.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adds synthetic demo data to an empty database. */
@Component
@ConditionalOnProperty(name = "lending.seed.enabled", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

  private final EmployeeService employees;
  private final DeviceService devices;

  public DevDataSeeder(EmployeeService employees, DeviceService devices) {
    this.employees = employees;
    this.devices = devices;
  }

  @Override
  public void run(String... args) {
    if (!employees.list().isEmpty()) {
      return;
    }
    employees.create("Asta Demo", "asta@example.com");
    employees.create("Jonas Demo", "jonas@example.com");
    employees.create("Rūta Demo", "ruta@example.com");
    employees.create("Tomas Demo", "tomas@example.com");

    devices.create("NTL-0001", "Dell XPS 13", "LAPTOP");
    devices.create("NTL-0002", "MacBook Pro 14", "LAPTOP");
    devices.create("NTL-0003", "Lenovo ThinkPad T14", "LAPTOP");
    devices.create("NTL-0101", "Dell UltraSharp 27", "MONITOR");
    devices.create("NTL-0102", "LG 32UN880", "MONITOR");
    devices.create("NTL-0201", "Pixel 9 test phone", "PHONE");
    devices.create("NTL-0202", "iPhone 16 test phone", "PHONE");
    devices.create("NTL-0301", "Logitech MX Keys", "ACCESSORY");
    log.info("Seeded demo employees and devices");
  }
}
