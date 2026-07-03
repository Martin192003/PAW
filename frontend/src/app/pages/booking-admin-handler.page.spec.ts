import { convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { BookingAdminHandlerPage } from './booking-admin-handler.page';

describe('BookingAdminHandlerPage', () => {
  let bookingService: {
    getManagedClubBooking: ReturnType<typeof vi.fn>;
    confirmManagedClubBooking: ReturnType<typeof vi.fn>;
    cancelManagedClubBooking: ReturnType<typeof vi.fn>;
    getReceiptBlob: ReturnType<typeof vi.fn>;
    requireReceiptLink: ReturnType<typeof vi.fn>;
  };
  let clubService: {
    getClubById: ReturnType<typeof vi.fn>;
    getTables: ReturnType<typeof vi.fn>;
  };
  let userService: { getUserById: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let component: BookingAdminHandlerPage;

  const booking = {
    id: 'booking-1',
    clubId: 'club-1',
    userId: 'user-1',
    tableId: 'table-1',
    bookingStart: '2026-07-03T22:00:00Z',
    bookingEnd: '2026-07-04T02:00:00Z',
    status: 'PENDIENTE',
    createdAt: '2026-07-03T12:00:00Z',
    totalAmount: 100,
    hasReceipt: false,
  } as any;

  const club = {
    clubId: 'club-1',
    clubName: 'Club One',
    cbu: '123',
  } as any;

  const table = {
    tableId: 'table-1',
    tableNumber: 7,
    capacity: 4,
    price: 200,
    discount: 25,
    tableState: 'DISPONIBLE',
  } as any;

  const user = {
    userId: 'user-1',
    username: 'pilar',
    name: 'Pilar',
    surname: 'Frutos',
    email: 'pilar@example.com',
  } as any;

  beforeEach(() => {
    bookingService = {
      getManagedClubBooking: vi.fn().mockReturnValue(of(booking)),
      confirmManagedClubBooking: vi.fn().mockReturnValue(of(undefined)),
      cancelManagedClubBooking: vi.fn().mockReturnValue(of(undefined)),
      getReceiptBlob: vi.fn(),
      requireReceiptLink: vi.fn(),
    };
    clubService = {
      getClubById: vi.fn().mockReturnValue(of(club)),
      getTables: vi.fn().mockReturnValue(of([table])),
    };
    userService = { getUserById: vi.fn().mockReturnValue(of(user)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    component = new BookingAdminHandlerPage(
      { snapshot: { paramMap: convertToParamMap({ bookingId: 'booking-1' }) } } as any,
      router as any,
      bookingService as any,
      clubService as any,
      userService as any,
      { detectChanges: vi.fn() } as any,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any
    );
  });

  it('loads the admin booking view', () => {
    component.ngOnInit();

    expect(bookingService.getManagedClubBooking).toHaveBeenCalledWith('booking-1');
    expect(component.booking).toBe(booking);
    expect(component.club).toBe(club);
    expect(component.table).toBe(table);
    expect(component.user).toBe(user);
    expect(component.hasReceipt).toBe(false);
  });

  it('navigates back to managed bookings', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/admin/bookings']);
  });

  it('confirms a pending booking and reloads it', () => {
    component.booking = booking;

    component.confirmBooking();

    expect(bookingService.confirmManagedClubBooking).toHaveBeenCalledWith('booking-1');
    expect(component.success).toBe('bookingAdminHandler.success.confirmed');
    expect(bookingService.getManagedClubBooking).toHaveBeenCalledWith('booking-1');
  });

  it('cancels a booking, closes the modal and reloads it', () => {
    component.booking = booking;
    component.showCancelModal = true;

    component.cancelBooking();

    expect(bookingService.cancelManagedClubBooking).toHaveBeenCalledWith('booking-1');
    expect(component.showCancelModal).toBe(false);
    expect(component.success).toBe('bookingAdminHandler.success.cancelled');
    expect(bookingService.getManagedClubBooking).toHaveBeenCalledWith('booking-1');
  });

  it('shows an error when the admin booking fails to load', () => {
    bookingService.getManagedClubBooking.mockReturnValueOnce(throwError(() => new Error('boom')));

    component.ngOnInit();

    expect(component.error).toBe('bookingAdminHandler.errors.loadFailed');
  });

  it('shows an error when confirming fails', () => {
    component.booking = booking;
    bookingService.confirmManagedClubBooking.mockReturnValueOnce(throwError(() => new Error('boom')));

    component.confirmBooking();

    expect(component.error).toBe('bookingAdminHandler.errors.confirmFailed');
  });
});
