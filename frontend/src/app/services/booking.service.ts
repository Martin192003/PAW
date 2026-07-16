import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpErrorResponse, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { Booking, BookingFilters, BookingPage, Receipt } from '../models/booking.model';
import { environment } from '../../environments/environment';
import { itemLinksFromCollection, normalizeLinks, requireAnyLink, requireLink, resolveApiUrl, totalFromHeader } from '../utils/api-url.util';
import { SKIP_API_ERROR_REDIRECT } from './api-error.interceptor';
import { AuthStateService } from './auth-state.service';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(
    private readonly http: HttpClient,
    private readonly authState: AuthStateService,
  ) {}

  listManagedClubBookings(filters: BookingFilters, page: number, size: number): Observable<BookingPage> {
    const params = this.params(filters, page, size);
    return this.http.get<Booking[] | BookingPage>(this.currentUserLink(
      ['managed'],
      `${this.apiUrl}/bookings?managed=true`
    ), {
      params,
      context: this.localErrorContext(),
      observe: 'response'
    }).pipe(
      switchMap(response => this.bookingPageFromResponse(response, page, size)),
      catchError(error => this.retryLegacyManagedBookings(error, params))
    );
  }

  listUserBookings(filters: BookingFilters, page: number, size: number): Observable<BookingPage> {
    const user = this.authState.getUser();
    const fallback = user?.userId
      ? `${this.apiUrl}/users/${encodeURIComponent(user.userId)}/bookings`
      : '';
    const params = this.params(filters, page, size);
    return this.http.get<Booking[] | BookingPage>(this.currentUserLink(['bookings'], fallback), {
      params,
      context: this.localErrorContext(),
      observe: 'response'
    }).pipe(
      switchMap(response => this.bookingPageFromResponse(response, page, size)),
      catchError(error => this.retryLegacyUserBookings(error, params))
    );
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
        ['confirm'],
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
        ['managed-cancel'],
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

  private currentUserLink(rels: string[], fallback: string): string {
    const links = normalizeLinks(this.authState.getUser()?.links);
    if (!links) {
      if (!fallback) {
        throw new Error('Authenticated user link is required.');
      }
      return resolveApiUrl(fallback, this.apiUrl);
    }
    return resolveApiUrl(requireAnyLink(links, rels, 'authenticated user'), this.apiUrl);
  }

  private retryLegacyUserBookings(error: unknown, params: HttpParams): Observable<BookingPage> {
    if (!(error instanceof HttpErrorResponse) || error.status !== 404) {
      return throwError(() => error);
    }
    const page = Number(params.get('page') ?? '1') || 1;
    const size = Number(params.get('size') ?? '10') || 10;
    return this.http.get<Booking[] | BookingPage>(`${this.apiUrl}/bookings`, {
      params,
      context: this.localErrorContext(),
      observe: 'response'
    }).pipe(switchMap(response => this.bookingPageFromResponse(response, page, size)));
  }

  private retryLegacyManagedBookings(error: unknown, params: HttpParams): Observable<BookingPage> {
    if (!(error instanceof HttpErrorResponse) || error.status !== 404) {
      return throwError(() => error);
    }
    const page = Number(params.get('page') ?? '1') || 1;
    const size = Number(params.get('size') ?? '10') || 10;
    return this.http.get<Booking[] | BookingPage>(`${this.apiUrl}/bookings`, {
      params: params.set('managed', 'true'),
      context: this.localErrorContext(),
      observe: 'response'
    }).pipe(switchMap(response => this.bookingPageFromResponse(response, page, size)));
  }

  private localErrorContext(): HttpContext {
    return new HttpContext().set(SKIP_API_ERROR_REDIRECT, true);
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

  private bookingPageFromResponse(response: HttpResponse<Booking[] | BookingPage>, page: number, size: number): Observable<BookingPage> {
    const body = response.body ?? [];
    if (!Array.isArray(body) && 'content' in body) {
      return of({
        content: body.content ?? [],
        page: body.page ?? page,
        size: body.size ?? size,
        totalElements: body.totalElements ?? body.content?.length ?? 0,
        totalPages: body.totalPages ?? 0,
      });
    }
    const itemLinks = itemLinksFromCollection(body);
    const totalElements = totalFromHeader(response.headers.get('X-Total-Count'), itemLinks?.length ?? body.length);
    const totalPages = Math.ceil(totalElements / size);

    if (itemLinks) {
      const requests = itemLinks.map(link => this.http.get<Booking>(resolveApiUrl(link, this.apiUrl)));
      return (requests.length ? forkJoin(requests) : of([] as Booking[])).pipe(
        map(content => ({ content, page, size, totalElements, totalPages }))
      );
    }

    return of({ content: body, page, size, totalElements, totalPages });
  }
}
