import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Booking, BookingFilters, BookingPage, Receipt } from '../models/booking.model';
import { environment } from '../../environments/environment';
import { requireAnyLink, requireLink, resolveApiUrl } from '../utils/api-url.util';
import { SKIP_API_ERROR_REDIRECT } from './api-error.interceptor';

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

  getBooking(_clubId: string, bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      resolveApiUrl(`${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  cancelBooking(_clubId: string, bookingId: string, links?: Booking['links']): Observable<void> {
    // Sin este flag, un 409 (InvalidStatusTransitionException, ej. cancelar
    // una reserva que ya no admite ese cambio de estado) lo agarra el
    // interceptor global y redirige a /error en vez de dejar que la pantalla
    // muestre su propio mensaje inline (mismo problema que tenian las mesas).
    return this.http.delete<void>(
      this.resolveBookingLink(
        links,
        ['cancel'],
        `booking ${bookingId} cancellation`,
        `${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}`
      ),
      { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) }
    );
  }

  /**
   * Igual que {@link getBooking}, pero para el detalle del admin: punto de
   * entrada legítimo desde `/admin/bookings/:bookingId`. A partir de ahí,
   * confirmar/cancelar usan `booking.links`.
   */
  getManagedClubBooking(bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(
      resolveApiUrl(`${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}`, this.apiUrl)
    );
  }

  confirmManagedClubBooking(bookingId: string, links?: Booking['links']): Observable<void> {
    // Mismo motivo que cancelBooking: confirmar tambien puede pegar contra
    // InvalidStatusTransitionException (409).
    return this.http.put<void>(
      this.resolveBookingLink(
        links,
        ['confirm', 'managed-update'],
        `managed booking ${bookingId} confirmation`,
        `${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}`
      ),
      { status: 'CONFIRMADA' },
      { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) }
    );
  }

  cancelManagedClubBooking(bookingId: string, links?: Booking['links']): Observable<void> {
    return this.http.put<void>(
      this.resolveBookingLink(
        links,
        ['managed-update'],
        `managed booking ${bookingId} cancellation`,
        `${this.apiUrl}/bookings/${encodeURIComponent(bookingId)}`
      ),
      { status: 'CANCELADA' },
      { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) }
    );
  }

  putReceipt(clubId: string, bookingId: string, file: File, receiptLink?: string): Observable<Receipt> {
    const formData = new FormData();
    formData.append('file', file);
    // InvalidReceiptException mapea a 400, pero se agrega el flag igual para
    // que la pantalla siga mostrando su propio error inline en vez de
    // depender de que el interceptor no la intercepte por casualidad.
    const options = { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) };
    if (receiptLink) {
      return this.http.put<Receipt>(resolveApiUrl(receiptLink, this.apiUrl), formData, options);
    }
    return this.http.put<Receipt>(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`, this.apiUrl),
      formData,
      options
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
    const options = { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) };
    if (receiptLink) {
      return this.http.delete<void>(resolveApiUrl(receiptLink, this.apiUrl), options);
    }
    return this.http.delete<void>(
      resolveApiUrl(`${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings/${encodeURIComponent(bookingId)}/receipt`, this.apiUrl),
      options
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
