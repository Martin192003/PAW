import { convertToParamMap } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
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
  let router: { navigate: ReturnType<typeof vi.fn> };
  let component: BookingAdminHandlerPage;

  const booking = {
    id: 'booking-1',
    clubId: 'club-1',
    clubName: 'Club One',
    userId: 'user-1',
    userUsername: 'pilar',
    userName: 'Pilar',
    userSurname: 'Frutos',
    tableId: 'table-1',
    tableNumber: 7,
    tablePrice: 200,
    tableDiscount: 25,
    bookingStart: '2026-07-03T22:00:00Z',
    bookingEnd: '2026-07-04T02:00:00Z',
    status: 'PENDIENTE',
    createdAt: '2026-07-03T12:00:00Z',
    totalAmount: 100,
    hasReceipt: false,
  } as any;

  beforeEach(() => {
    vi.useFakeTimers();
    bookingService = {
      getManagedClubBooking: vi.fn().mockReturnValue(of(booking)),
      confirmManagedClubBooking: vi.fn().mockReturnValue(of(undefined)),
      cancelManagedClubBooking: vi.fn().mockReturnValue(of(undefined)),
      getReceiptBlob: vi.fn(),
      requireReceiptLink: vi.fn(),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    component = new BookingAdminHandlerPage(
      { snapshot: { paramMap: convertToParamMap({ bookingId: 'booking-1' }) } } as any,
      router as any,
      bookingService as any,
      { detectChanges: vi.fn() } as any,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any
    );
  });

  afterEach(() => {
    component.ngOnDestroy();
    vi.useRealTimers();
  });

  it('loads the admin booking view', () => {
    component.ngOnInit();

    expect(bookingService.getManagedClubBooking).toHaveBeenCalledWith('booking-1');
    expect(component.booking).toBe(booking);
    expect(component.club).toEqual(expect.objectContaining({ clubId: 'club-1', clubName: 'Club One' }));
    expect(component.table).toEqual(expect.objectContaining({ tableId: 'table-1', tableNumber: 7, price: 200, discount: 25 }));
    expect(component.user).toEqual(expect.objectContaining({ userId: 'user-1', username: 'pilar', surname: 'Frutos' }));
    expect(component.hasReceipt).toBe(false);
  });

  it('does not download the receipt during the initial admin booking load', () => {
    bookingService.getManagedClubBooking.mockReturnValueOnce(of({ ...booking, hasReceipt: true }));

    component.ngOnInit();

    expect(component.hasReceipt).toBe(true);
    expect(component.receiptUrl).toBe('');
    expect(bookingService.getReceiptBlob).not.toHaveBeenCalled();
  });

  it('navigates back to managed bookings', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/admin/bookings']);
  });

  it('confirms a pending booking and reloads it', () => {
    component.booking = booking;

    component.confirmBooking();

    expect(bookingService.confirmManagedClubBooking).toHaveBeenCalledWith('booking-1', undefined);
    expect(component.success).toBe('bookingAdminHandler.success.confirmed');
    expect(bookingService.getManagedClubBooking).toHaveBeenCalledWith('booking-1');

    vi.advanceTimersByTime(4000);

    expect(component.success).toBe('');
  });

  it('cancels a booking, closes the modal and reloads it', () => {
    component.booking = booking;
    component.showCancelModal = true;

    component.cancelBooking();

    expect(bookingService.cancelManagedClubBooking).toHaveBeenCalledWith('booking-1', undefined);
    expect(component.showCancelModal).toBe(false);
    expect(component.success).toBe('bookingAdminHandler.success.cancelled');
    expect(bookingService.getManagedClubBooking).toHaveBeenCalledWith('booking-1');

    vi.advanceTimersByTime(4000);

    expect(component.success).toBe('');
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

    vi.advanceTimersByTime(4000);

    expect(component.error).toBe('');
  });

  it('shows a receipt-required message when confirming without a receipt', () => {
    component.booking = booking;
    bookingService.confirmManagedClubBooking.mockReturnValueOnce(
      throwError(() => ({ error: { message: 'receipt.required' } }))
    );

    component.confirmBooking();

    expect(component.error).toBe('bookingAdminHandler.errors.receiptRequired');
  });

  it('sets loading to true while the booking request is pending', () => {
    const subject = new Subject<any>();
    bookingService.getManagedClubBooking.mockReturnValueOnce(subject.asObservable());

    component.ngOnInit();

    expect(component.loading).toBe(true);

    subject.next(booking);
    subject.complete();

    expect(component.loading).toBe(false);
  });
});
