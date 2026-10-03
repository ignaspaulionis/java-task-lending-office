import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Employee {
  id: number;
  name: string;
  email: string;
  active: boolean;
}

export interface Device {
  id: number;
  inventoryTag: string;
  name: string;
  category: string;
  status: 'AVAILABLE' | 'MAINTENANCE' | 'RETIRED';
  available: boolean;
  loanedTo: number | null;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Loan {
  id: number;
  deviceId: number;
  deviceName: string;
  employeeId: number;
  borrowedAt: string;
  dueAt: string;
  returnedAt: string | null;
  overdue: boolean;
  lateFee: string | null;
}

export interface Summary {
  employee: Employee;
  loans: Loan[];
  waitlist: { deviceId: number; deviceName: string; position: number }[];
}

/** Thin typed wrapper around the backend REST API. */
@Injectable({ providedIn: 'root' })
export class LendingApi {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api';

  employees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.base}/employees`);
  }

  devices(q: string, page: number): Observable<Page<Device>> {
    let params = new HttpParams().set('page', page).set('size', 10);
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<Page<Device>>(`${this.base}/devices`, { params });
  }

  summary(employeeId: number): Observable<Summary> {
    return this.http.get<Summary>(`${this.base}/employees/${employeeId}/summary`);
  }

  overdue(): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.base}/loans/overdue`);
  }

  borrow(deviceId: number, employeeId: number): Observable<Loan> {
    return this.http.post<Loan>(`${this.base}/loans`, { deviceId, employeeId });
  }

  returnLoan(loanId: number, employeeId: number): Observable<{ nextEmployeeId: number | null }> {
    return this.http.post<{ nextEmployeeId: number | null }>(
      `${this.base}/loans/${loanId}/return`,
      { employeeId },
    );
  }

  joinWaitlist(deviceId: number, employeeId: number): Observable<{ position: number | null }> {
    return this.http.post<{ position: number | null }>(
      `${this.base}/devices/${deviceId}/waitlist`,
      { employeeId },
    );
  }

  leaveWaitlist(deviceId: number, employeeId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/devices/${deviceId}/waitlist/${employeeId}`);
  }
}

/** The stable error code of a problem response, or a generic description. */
export function errorCode(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    return error.error?.code ?? `HTTP ${error.status || 'unreachable'}`;
  }
  return 'UNEXPECTED_ERROR';
}
