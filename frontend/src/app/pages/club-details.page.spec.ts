import { convertToParamMap } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ClubDetailsPage } from './club-details.page';

describe('ClubDetailsPage reservation flow', () => {
  let route: {
    paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
    queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
    fragment: BehaviorSubject<string | null>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let clubService: {
    getTables: ReturnType<typeof vi.fn>;
    reserveTable: ReturnType<typeof vi.fn>;
  };
  let component: ClubDetailsPage;

  const table = {
    tableId: 'table-1',
    tableNumber: 1,
    capacity: 4,
    price: 100,
    discount: 0,
    tableState: 'DISPONIBLE',
  } as any;

  beforeEach(() => {
    route = {
      paramMap: new BehaviorSubject(convertToParamMap({ id: 'club-1' })),
      queryParamMap: new BehaviorSubject(convertToParamMap({})),
      fragment: new BehaviorSubject<string | null>(null),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    clubService = {
      getTables: vi.fn().mockReturnValue(of([table])),
      reserveTable: vi.fn(),
    };

    component = new ClubDetailsPage(
      route as any,
      clubService as any,
      {} as any,
      { getUser: vi.fn().mockReturnValue(null), isLoggedIn$: of(false) } as any,
      router as any,
      { detectChanges: vi.fn() } as any,
      'browser' as unknown as object,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any
    );
  });

  function setReservationState(): void {
    (component as any).currentClubId = 'club-1';
    component.selectedDate = '2026-07-03';
    component.selectedTableId = 'table-1';
    component.tables = [table];
  }

  it('loads available dates with available tables', () => {
    (component as any).currentClubId = 'club-1';

    (component as any).loadAvailableBookingDates('club-1');

    expect(component.bookingDates.length).toBeGreaterThan(0);
    expect(component.selectedDate).toBe(component.bookingDates[0]);
    expect(component.tables).toEqual([table]);
    expect(component.loadingBookingDates).toBe(false);
  });

  it('loads available tables when the booking date changes', () => {
    (component as any).currentClubId = 'club-1';

    component.onBookingDateChange('2026-07-03');

    expect(clubService.getTables).toHaveBeenCalledWith('club-1', '2026-07-03', undefined);
    expect(component.tables).toEqual([table]);
    expect(component.selectedDate).toBe('2026-07-03');
    expect(component.tablesOpen).toBe(true);
  });

  it('blocks reservation while table or date is invalid', () => {
    (component as any).currentClubId = 'club-1';
    component.selectedDate = '';
    component.selectedTableId = undefined;

    component.onReserveClick();

    expect(clubService.reserveTable).not.toHaveBeenCalled();
    expect(component.tablesError).toBe('clubDetails.selectTableAndDate');
  });

  it('creates a successful reservation', () => {
    setReservationState();
    clubService.reserveTable.mockReturnValue(of({ id: 'booking-1' }));

    component.onReserveClick();

    expect(clubService.reserveTable).toHaveBeenCalledWith(
      'club-1',
      'table-1',
      '2026-07-03',
      undefined
    );
    expect(component.tablesError).toBeUndefined();
    expect(component.reservationModalOpen).toBe(true);
    expect(component.reservedBookingId).toBe('booking-1');
  });

  it('shows reservation errors', () => {
    setReservationState();
    clubService.reserveTable.mockReturnValue(
      throwError(() => ({ error: { message: 'Table already booked' } }))
    );

    component.onReserveClick();

    expect(component.reservationModalOpen).toBe(false);
    expect(component.reservedBookingId).toBeUndefined();
    expect(component.tablesError).toBe('Table already booked');
  });

  it('navigates after reservation', () => {
    component.reservedBookingClubId = 'club-1';
    component.reservedBookingId = 'booking-1';

    component.goToReservedBooking();

    expect(router.navigate).toHaveBeenCalledWith(['/bookings', 'club-1', 'booking-1']);
  });
});
