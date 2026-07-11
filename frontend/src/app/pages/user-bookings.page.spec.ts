import { convertToParamMap } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { UserBookingsPage } from './user-bookings.page';

describe('UserBookingsPage', () => {
  let bookingService: { listUserBookings: ReturnType<typeof vi.fn> };
  let clubService: {
    getClubById: ReturnType<typeof vi.fn>;
    getTables: ReturnType<typeof vi.fn>;
  };
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let route: {
    snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> };
    queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let cdr: { detectChanges: ReturnType<typeof vi.fn> };
  let component: UserBookingsPage;

  const booking = {
    id: 'booking-1',
    clubId: 'club-1',
    tableId: 'table-1',
    bookingStart: '2026-07-03T22:00:00Z',
    status: 'PENDIENTE',
  } as any;

  beforeEach(() => {
    bookingService = { listUserBookings: vi.fn() };
    clubService = {
      getClubById: vi.fn().mockReturnValue(of({ clubId: 'club-1', clubName: 'Club One' })),
      getTables: vi.fn().mockReturnValue(of([{ tableId: 'table-1', tableNumber: 7 }])),
    };
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    route = {
      snapshot: { queryParamMap: convertToParamMap({}) },
      queryParamMap,
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    cdr = { detectChanges: vi.fn() };

    bookingService.listUserBookings.mockImplementation(
      (_filters, page) => of({ content: [booking], page, totalPages: 3, totalElements: 1 })
    );

    component = new UserBookingsPage(
      bookingService as any,
      clubService as any,
      route as any,
      router as any,
      cdr as any,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any
    );
  });

  it('loads user bookings with default filters', () => {
    component.ngOnInit();

    expect(bookingService.listUserBookings).toHaveBeenCalledWith(
      component.filters,
      1,
      10
    );
    expect(component.bookings).toEqual([booking]);
    expect(component.totalPages).toBe(3);
  });

  it('filter changes reset to the first page and update the URL', () => {
    component.currentPage = 3;
    component.filters.status = 'CONFIRMADA';

    component.applyFilters();

    expect(component.currentPage).toBe(1);
    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({
      relativeTo: route,
      queryParams: expect.objectContaining({ page: 1, status: 'CONFIRMADA' }),
    }));
  });

  it('reloads when the URL query params change externally', () => {
    component.ngOnInit();
    bookingService.listUserBookings.mockClear();

    queryParamMap.next(convertToParamMap({ page: '2', status: 'CONFIRMADA' }));

    expect(component.currentPage).toBe(2);
    expect(component.filters.status).toBe('CONFIRMADA');
    expect(bookingService.listUserBookings).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'CONFIRMADA' }),
      2,
      10
    );
  });

  it('paginates within bounds', () => {
    component.totalPages = 3;
    component.currentPage = 1;

    component.prevPage();
    expect(component.currentPage).toBe(1);
    expect(router.navigate).not.toHaveBeenCalled();

    component.nextPage();
    expect(component.currentPage).toBe(2);
    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({
      queryParams: expect.objectContaining({ page: 2 }),
    }));
  });

  it('navigates to the selected user booking', () => {
    component.goToBooking(booking);

    expect(router.navigate).toHaveBeenCalledWith(['/bookings', 'club-1', 'booking-1']);
  });

  it('shows an error when user bookings fail to load', () => {
    bookingService.listUserBookings.mockReturnValue(throwError(() => new Error('boom')));

    component.loadBookings();

    expect(component.bookings).toEqual([]);
    expect(component.loading).toBe(false);
    expect(component.error).toBe('userBookings.errors.loadFailed');
  });

  afterEach(() => {
    component.ngOnDestroy();
  });
});
