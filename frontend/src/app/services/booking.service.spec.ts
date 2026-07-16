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
    expect(req.request.params.get('page')).toBe('2');
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
    expect(fallback.request.params.get('page')).toBe('1');
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
    expect(req.request.params.get('page')).toBe('1');
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
});
