import { convertToParamMap } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';
import { ManagedClubBookingsPage } from './managed-club-bookings.page';

describe('ManagedClubBookingsPage', () => {
  let bookingService: { listManagedClubBookings: ReturnType<typeof vi.fn> };
  let clubService: {
    getClubsByUser: ReturnType<typeof vi.fn>;
    getClubById: ReturnType<typeof vi.fn>;
    getTables: ReturnType<typeof vi.fn>;
  };
  let authState: { getUser: ReturnType<typeof vi.fn> };
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let route: {
    snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> };
    queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let component: ManagedClubBookingsPage;

  const booking = {
    id: 'booking-1',
    clubId: 'club-1',
    tableId: 'table-1',
    bookingStart: '2026-07-03T22:00:00Z',
    status: 'PENDIENTE',
  } as any;

  beforeEach(() => {
    bookingService = { listManagedClubBookings: vi.fn() };
    clubService = {
      getClubsByUser: vi.fn().mockReturnValue(of({
        content: [{ clubId: 'club-1', clubName: 'Club One' }],
      })),
      getClubById: vi.fn().mockReturnValue(of({ clubId: 'club-1', clubName: 'Club One' })),
      getTables: vi.fn().mockReturnValue(of([{ tableId: 'table-1', tableNumber: 9 }])),
    };
    authState = { getUser: vi.fn().mockReturnValue({ userId: 'admin-1' }) };
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    route = {
      snapshot: { queryParamMap: convertToParamMap({}) },
      queryParamMap,
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    bookingService.listManagedClubBookings.mockImplementation(
      (_filters, page) => of({ content: [booking], page, totalPages: 2, totalElements: 1 })
    );

    component = new ManagedClubBookingsPage(
      bookingService as any,
      clubService as any,
      authState as any,
      route as any,
      router as any,
      { detectChanges: vi.fn() } as any,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any
    );
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('loads managed bookings and admin clubs', () => {
    component.ngOnInit();

    expect(clubService.getClubsByUser).toHaveBeenCalledWith('admin-1', 1, 100);
    expect(bookingService.listManagedClubBookings).toHaveBeenCalledWith(
      component.filters,
      1,
      10
    );
    expect(component.bookings).toEqual([booking]);
    expect(component.clubNames['club-1']).toBe('Club One');
  });

  it('filter changes reset to the first page and update the URL', () => {
    component.currentPage = 2;
    component.filters.clubId = 'club-1';
    component.filters.hasReceipt = 'true';

    component.applyFilters();

    expect(component.currentPage).toBe(1);
    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({
      relativeTo: route,
      queryParams: expect.objectContaining({ page: 1, clubId: 'club-1', hasReceipt: 'true' }),
    }));
  });

  it('reloads when the URL query params change externally', () => {
    component.ngOnInit();
    bookingService.listManagedClubBookings.mockClear();

    queryParamMap.next(convertToParamMap({ page: '2', clubId: 'club-1' }));

    expect(component.currentPage).toBe(2);
    expect(component.filters.clubId).toBe('club-1');
    expect(bookingService.listManagedClubBookings).toHaveBeenCalledWith(
      expect.objectContaining({ clubId: 'club-1' }),
      2,
      10
    );
  });

  it('paginates managed bookings within bounds', () => {
    component.totalPages = 2;
    component.currentPage = 1;

    component.nextPage();
    expect(component.currentPage).toBe(2);
    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({
      queryParams: expect.objectContaining({ page: 2 }),
    }));

    component.nextPage();
    expect(component.currentPage).toBe(2);
  });

  it('navigates to the selected managed booking', () => {
    component.goToBooking(booking);

    expect(router.navigate).toHaveBeenCalledWith(['/admin/bookings', 'booking-1']);
  });
});
