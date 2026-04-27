import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Booking, BookingFilters, BookingPage, Receipt } from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly apiUrl = 'http://localhost:8080/webapp_war_exploded/api';

  constructor(private readonly http: HttpClient) {}

  listManagedClubBookings(filters: BookingFilters, page: number, size: number): Observable<BookingPage> {
    return this.http.get<BookingPage>(`${this.apiUrl}/managed-club-bookings`, {
      params: this.params(filters, page, size)
    });
  }

  listUserBookings(filters: BookingFilters, page: number, size: number): Observable<BookingPage> {
    return this.http.get<BookingPage>(`${this.apiUrl}/bookings`, {
      params: this.params(filters, page, size)
    });
  }

  getBooking(clubId: string, bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}`
    );
  }

  cancelBooking(clubId: string, bookingId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}`
    );
  }

  getManagedClubBooking(bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      `${this.apiUrl}/managed-club-bookings/${encodeURIComponent(bookingId)}`
    );
  }

  confirmManagedClubBooking(bookingId: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/managed-club-bookings/${encodeURIComponent(bookingId)}/confirmation`,
      {}
    );
  }

  cancelManagedClubBooking(bookingId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/managed-club-bookings/${encodeURIComponent(bookingId)}`
    );
  }

  putReceipt(clubId: string, bookingId: string, file: File): Observable<Receipt> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.put<Receipt>(
      `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`,
      formData
    );
  }

  getReceiptBlob(clubId: string, bookingId: string): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`,
      { responseType: 'blob' }
    );
  }

  deleteReceipt(clubId: string, bookingId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`
    );
  }

  private params(filters: BookingFilters, page: number, size: number): HttpParams {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    });

    return params;
  }
}
