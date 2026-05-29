import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Booking, BookingFilters, BookingPage, Receipt } from '../models/booking.model';
import { environment } from '../../environments/environment';
import { requireLink, resolveApiUrl } from '../utils/api-url.util';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly apiUrl = environment.apiBaseUrl;

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
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  cancelBooking(clubId: string, bookingId: string): Observable<void> {
    return this.http.delete<void>(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  getManagedClubBooking(bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      resolveApiUrl(`${this.apiUrl}/managed-club-bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  confirmManagedClubBooking(bookingId: string): Observable<void> {
    return this.http.put<void>(
      resolveApiUrl(`${this.apiUrl}/managed-club-bookings/${encodeURIComponent(bookingId)}/confirmation`, this.apiUrl),
      {}
    );
  }

  cancelManagedClubBooking(bookingId: string): Observable<void> {
    return this.http.delete<void>(
      resolveApiUrl(`${this.apiUrl}/managed-club-bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  putReceipt(clubId: string, bookingId: string, file: File, receiptLink?: string): Observable<Receipt> {
    const formData = new FormData();
    formData.append('file', file);
    if (receiptLink) {
      return this.http.put<Receipt>(resolveApiUrl(receiptLink, this.apiUrl), formData);
    }
    return this.http.put<Receipt>(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`, this.apiUrl),
      formData
    );
  }

  getReceiptBlob(clubId: string, bookingId: string, receiptLink?: string): Observable<Blob> {
    if (receiptLink) {
      return this.http.get(resolveApiUrl(receiptLink, this.apiUrl), { responseType: 'blob' });
    }
    return this.http.get(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`, this.apiUrl),
      { responseType: 'blob' }
    );
  }

  deleteReceipt(clubId: string, bookingId: string, receiptLink?: string): Observable<void> {
    if (receiptLink) {
      return this.http.delete<void>(resolveApiUrl(receiptLink, this.apiUrl));
    }
    return this.http.delete<void>(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`, this.apiUrl)
    );
  }

  requireReceiptLink(booking: Booking): string {
    return resolveApiUrl(requireLink(booking.links, 'receipt', 'booking receipt'), this.apiUrl);
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
