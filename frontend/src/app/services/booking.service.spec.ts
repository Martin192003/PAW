import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BookingService } from './booking.service';

describe('BookingService filters', () => {
  let service: BookingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BookingService,
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

    const req = httpMock.expectOne(request => request.url.endsWith('/bookings'));
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('bookingDate')).toBe('2026-07-03');
    expect(req.request.params.get('status')).toBe('CONFIRMADA');
    expect(req.request.params.get('sort')).toBe('date_desc');
    expect(req.request.params.get('hasReceipt')).toBe('true');
    req.flush({ content: [] });
  });

  it('syncs managed booking filters into query params', () => {
    service.listManagedClubBookings({
      clubId: 'club-1',
      bookingDate: '',
      status: 'PENDIENTE',
      sort: '',
      hasReceipt: 'false',
    }, 1, 10).subscribe();

     const req = httpMock.expectOne(request => request.url.endsWith('/bookings'));
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('managed')).toBe('true');
    expect(req.request.params.get('clubId')).toBe('club-1');
    expect(req.request.params.get('status')).toBe('PENDIENTE');
    expect(req.request.params.get('hasReceipt')).toBe('false');
    expect(req.request.params.has('bookingDate')).toBe(false);
    expect(req.request.params.has('sort')).toBe(false);
    req.flush({ content: [] });
  });
});
