import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BookingService } from './booking.service';
import { AuthStateService } from './auth-state.service';

describe('BookingService filters', () => {
  let service: BookingService;
  let httpMock: HttpTestingController;
  let authState: { getUser: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authState = {
      getUser: vi.fn().mockReturnValue({
        userId: 'user-1',
        links: [
          { rel: 'bookings', href: '/api/users/user-1/bookings' },
          { rel: 'managed', href: '/api/bookings?managed=true' },
        ],
      }),
    };

    TestBed.configureTestingModule({
      providers: [
        BookingService,
        { provide: AuthStateService, useValue: authState },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(BookingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('syncs user booking filters into query params', () => {
    service.listUserBookings({
      bookingDate: '2026-07-03',
      status: 'CONFIRMADA',
      sort: 'date_desc',
      hasReceipt: 'true',
    }, 2, 20).subscribe();

    const req = httpMock.expectOne(request => request.url.endsWith('/users/user-1/bookings'));
    // La UI usa paginas 1-based (2); la API espera 0-based (1).
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('bookingDate')).toBe('2026-07-03');
    expect(req.request.params.get('status')).toBe('CONFIRMADA');
    expect(req.request.params.get('sort')).toBe('date_desc');
    expect(req.request.params.get('hasReceipt')).toBe('true');
    req.flush({ content: [] });
  });

  it('falls back to the legacy user bookings endpoint when the REST collection is not deployed', () => {
    service.listUserBookings({
      status: 'PENDIENTE',
    }, 1, 10).subscribe();

    const first = httpMock.expectOne(request => request.url.endsWith('/users/user-1/bookings'));
    first.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });

    const fallback = httpMock.expectOne(request => request.url.endsWith('/bookings'));
    expect(fallback.request.params.get('page')).toBe('0');
    expect(fallback.request.params.get('size')).toBe('10');
    expect(fallback.request.params.get('status')).toBe('PENDIENTE');
    expect(fallback.request.params.has('managed')).toBe(false);
    fallback.flush({ content: [] });
  });

  it('syncs managed booking filters into query params', () => {
    service.listManagedClubBookings({
      clubId: 'club-1',
      bookingDate: '',
      status: 'PENDIENTE',
      sort: '',
      hasReceipt: 'false',
    }, 1, 10).subscribe();

    const req = httpMock.expectOne(request => request.urlWithParams.includes('/bookings?managed=true'));
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.has('managed')).toBe(false);
    expect(req.request.params.get('clubId')).toBe('club-1');
    expect(req.request.params.get('status')).toBe('PENDIENTE');
    expect(req.request.params.get('hasReceipt')).toBe('false');
    expect(req.request.params.has('bookingDate')).toBe(false);
    expect(req.request.params.has('sort')).toBe(false);
    req.flush({ content: [] });
  });

  it('falls back to the legacy managed bookings mode when the REST collection is not deployed', () => {
    service.listManagedClubBookings({
      status: 'PENDIENTE',
    }, 1, 10).subscribe();

    const first = httpMock.expectOne(request => request.urlWithParams.includes('/bookings?managed=true'));
    first.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });

    const fallback = httpMock.expectOne(request => request.url.endsWith('/bookings'));
    expect(fallback.request.params.get('managed')).toBe('true');
    expect(fallback.request.params.get('status')).toBe('PENDIENTE');
    fallback.flush({ content: [] });
  });

  it('getBooking reads the booking by its canonical flat URI', () => {
    service.getBooking('club-1', 'booking-1').subscribe();

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/bookings/booking-1'));
    expect(req.request.url).not.toContain('/clubs/club-1/bookings/booking-1');
    req.flush({ id: 'booking-1' });
  });

  it('cancelBooking uses the booking\'s own "cancel" link when available', () => {
    service.cancelBooking('club-1', 'booking-1', { cancel: '/api/bookings/booking-1' }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith('/bookings/booking-1'));
    req.flush(null);
  });

  it('cancelBooking falls back to a constructed URL when no links are given', () => {
    service.cancelBooking('club-1', 'booking-1').subscribe();

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith('/bookings/booking-1'));
    req.flush(null);
  });

  it('confirmManagedClubBooking PUTs a CONFIRMADA status to the "confirm" link', () => {
    service.confirmManagedClubBooking('booking-1', { confirm: '/api/bookings/booking-1' }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/bookings/booking-1'));
    expect(req.request.body).toEqual({ status: 'CONFIRMADA' });
    req.flush(null);
  });

  it('cancelManagedClubBooking PUTs a CANCELADA status to the "managed-cancel" link', () => {
    service.cancelManagedClubBooking('booking-1', { 'managed-cancel': '/api/bookings/booking-1' }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/bookings/booking-1'));
    expect(req.request.body).toEqual({ status: 'CANCELADA' });
    req.flush(null);
  });

  it('putReceipt uploads the file as multipart form data to the receipt sub-resource', () => {
    const file = new File(['fake'], 'recibo.jpg', { type: 'image/jpeg' });

    service.putReceipt('club-1', 'booking-1', file).subscribe();

    const req = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/clubs/club-1/bookings/booking-1/receipt'));
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toBe(file);
    req.flush({ id: 'receipt-1' });
  });

  it('getReceiptBlob requests a blob from the explicit receipt link when given', () => {
    let result: Blob | undefined;
    service.getReceiptBlob('club-1', 'booking-1', '/api/clubs/club-1/bookings/booking-1/receipt').subscribe(blob => { result = blob; });

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/clubs/club-1/bookings/booking-1/receipt'));
    expect(req.request.responseType).toBe('blob');
    const blob = new Blob(['fake-image']);
    req.flush(blob);

    expect(result).toBe(blob);
  });

  it('deleteReceipt DELETEs the constructed receipt URL when no link is given', () => {
    service.deleteReceipt('club-1', 'booking-1').subscribe();

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith('/clubs/club-1/bookings/booking-1/receipt'));
    req.flush(null);
  });

  it('requireReceiptLink resolves the booking\'s "receipt" link and throws when missing', () => {
    const url = service.requireReceiptLink({
      links: { receipt: '/api/clubs/club-1/bookings/booking-1/receipt' },
    } as never);
    expect(url).toContain('/clubs/club-1/bookings/booking-1/receipt');

    expect(() => service.requireReceiptLink({ links: {} } as never)).toThrow();
  });
});
