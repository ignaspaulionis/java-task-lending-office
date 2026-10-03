import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { Device, Employee, LendingApi, Loan, Page, Summary, errorCode } from './lending-api';

/** A single page for clicking through the lending desk API. */
@Component({
  selector: 'app-root',
  imports: [DatePipe, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly api = inject(LendingApi);

  protected readonly employees = signal<Employee[]>([]);
  protected readonly employeeId = signal<number | null>(null);
  protected readonly devices = signal<Page<Device> | null>(null);
  protected readonly summary = signal<Summary | null>(null);
  protected readonly overdue = signal<Loan[]>([]);
  protected readonly message = signal('');
  protected readonly failed = signal(false);
  protected readonly query = signal('');
  protected readonly page = signal(0);

  protected readonly employeeNames = computed(
    () => new Map(this.employees().map((e) => [e.id, e.name])),
  );

  ngOnInit(): void {
    this.api.employees().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.employeeId.set(employees[0]?.id ?? null);
        this.refresh();
      },
      error: (e) => this.show(`Backend not reachable: ${errorCode(e)}`, true),
    });
  }

  protected selectEmployee(id: number): void {
    this.employeeId.set(id);
    this.refresh();
  }

  protected search(q: string): void {
    this.query.set(q);
    this.page.set(0);
    this.loadDevices();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadDevices();
  }

  protected borrow(device: Device): void {
    this.run(this.api.borrow(device.id, this.currentEmployee()), `Borrowed ${device.name}`);
  }

  protected join(device: Device): void {
    this.run(
      this.api.joinWaitlist(device.id, this.currentEmployee()),
      `Joined the waitlist for ${device.name}`,
    );
  }

  protected returnLoan(loan: Loan): void {
    this.api.returnLoan(loan.id, this.currentEmployee()).subscribe({
      next: (result) => {
        const next = result.nextEmployeeId;
        this.show(
          `Returned ${loan.deviceName}` +
            (next ? `, handed to ${this.employeeNames().get(next)}` : ''),
        );
        this.refresh();
      },
      error: (e) => this.show(errorCode(e), true),
    });
  }

  protected leave(deviceId: number, deviceName: string): void {
    this.run(
      this.api.leaveWaitlist(deviceId, this.currentEmployee()),
      `Left the waitlist for ${deviceName}`,
    );
  }

  private run(action: Observable<unknown>, success: string): void {
    action.subscribe({
      next: () => {
        this.show(success);
        this.refresh();
      },
      error: (e) => this.show(errorCode(e), true),
    });
  }

  private refresh(): void {
    this.loadDevices();
    this.api.overdue().subscribe((loans) => this.overdue.set(loans));
    const id = this.employeeId();
    if (id !== null) {
      this.api.summary(id).subscribe((summary) => this.summary.set(summary));
    }
  }

  private loadDevices(): void {
    this.api.devices(this.query(), this.page()).subscribe({
      next: (page) => this.devices.set(page),
      error: (e) => this.show(errorCode(e), true),
    });
  }

  private currentEmployee(): number {
    const id = this.employeeId();
    if (id === null) {
      throw new Error('No employee selected');
    }
    return id;
  }

  private show(text: string, failed = false): void {
    this.message.set(text);
    this.failed.set(failed);
  }
}
