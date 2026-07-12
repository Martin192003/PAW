import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Booking, BookingFilters, BookingPage, Receipt } from '../models/booking.model';
import { environment } from '../../environments/environment';
import { requireAnyLink, requireLink, resolveApiUrl } from '../utils/api-url.util';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  listManagedClubBookings(filters: BookingFilters, page: number, size: number): Observable<BookingPage> {
    return this.http.get<BookingPage>(`${this.apiUrl}/bookings`, {
      params: this.params(filters, page, size).set('managed', 'true')
    });
  }

  listUserBookings(filters: BookingFilters, page: number, size: number): Observable<BookingPage> {
    return this.http.get<BookingPage>(`${this.apiUrl}/bookings`, {
      params: this.params(filters, page, size)
    });
  }

  /**
   * Carga una reserva a partir de clubId + bookingId.
   *
   * Es el punto de entrada legítimo para deep links (`/bookings/:clubId/:bookingId`
   * desde la ruta), donde todavía no tenemos ningún `Booking` con `links` a mano.
   * Una vez cargada, el resto de las operaciones sobre esa reserva (cancelar,
   * recibo, etc.) deben usar `booking.links` en vez de reconstruir la URL.
   */
  getBooking(clubId: string, bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  cancelBooking(clubId: string, bookingId: string, links?: Booking['links']): Observable<void> {
    return this.http.delete<void>(
      this.resolveBookingLink(
        links,
        ['cancel'],
        `booking ${bookingId} cancellation`,
        `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}`
      )
    );
  }

  /**
   * Igual que {@link getBooking}, pero para el detalle del admin: punto de
   * entrada legítimo desde `/admin/bookings/:bookingId`. A partir de ahí,
   * confirmar/cancelar usan `booking.links`.
   */
  getManagedClubBooking(bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      resolveApiUrl(`${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}?managed=true`, this.apiUrl)
    );
  }

  confirmManagedClubBooking(bookingId: string, links?: Booking['links']): Observable<void> {
    return this.http.put<void>(
      this.resolveBookingLink(
        links,
        ['confirm', 'managed-update'],
        `managed booking ${bookingId} confirmation`,
        `${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}?managed=true`
      ),
      { status: 'CONFIRMADA' }
    );
  }

  cancelManagedClubBooking(bookingId: string, links?: Booking['links']): Observable<void> {
    return this.http.put<void>(
      this.resolveBookingLink(
        links,
        ['managed-update'],
        `managed booking ${bookingId} cancellation`,
        `${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}?managed=true`
      ),
      { status: 'CANCELADA' }
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

  /**
   * Resuelve la URL de una acción de la reserva (cancelar, confirmar, etc.)
   * a partir de sus links HATEOAS. La URL construida a mano (`fallback`) es
   * un último recurso documentado para cuando el caller todavía no tiene la
   * reserva completa (por ejemplo, justo después de crearla). Los llamadores
   * que ya tienen el `Booking` cargado deben pasar siempre sus `links`.
   */
  private resolveBookingLink(links: Booking['links'] | undefined, rels: string[], context: string, fallback: string): string {
    if (!links) {
      return resolveApiUrl(fallback, this.apiUrl);
    }
    return resolveApiUrl(requireAnyLink(links, rels, context), this.apiUrl);
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
