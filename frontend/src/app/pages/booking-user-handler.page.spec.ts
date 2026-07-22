import { convertToParamMap } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { vi } from 'vitest';
import { BookingUserHandlerPage } from './booking-user-handler.page';

describe('BookingUserHandlerPage', () => {
  let bookingService: {
    getBooking: ReturnType<typeof vi.fn>;
    cancelBooking: ReturnType<typeof vi.fn>;
    putReceipt: ReturnType<typeof vi.fn>;
    getReceiptBlob: ReturnType<typeof vi.fn>;
    requireReceiptLink: ReturnType<typeof vi.fn>;
  };
  let authState: { getUser: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let component: BookingUserHandlerPage;

  const booking = {
    id: 'booking-1',
    clubId: 'club-1',
    clubName: 'Club One',
    userId: 'user-1',
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
      getBooking: vi.fn().mockReturnValue(of(booking)),
      cancelBooking: vi.fn().mockReturnValue(of(undefined)),
      putReceipt: vi.fn(),
      getReceiptBlob: vi.fn(),
      requireReceiptLink: vi.fn(),
    };
    authState = { getUser: vi.fn().mockReturnValue(null) };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    component = new BookingUserHandlerPage(
      {
        snapshot: {
          paramMap: convertToParamMap({ clubId: 'club-1', bookingId: 'booking-1' }),
        },
      } as any,
      router as any,
      authState as any,
      bookingService as any,
      { detectChanges: vi.fn() } as any,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any
    );
  });

  afterEach(() => {
    component.ngOnDestroy();
    vi.useRealTimers();
  });

  it('loads the booking view', () => {
    component.ngOnInit();

    expect(bookingService.getBooking).toHaveBeenCalledWith('club-1', 'booking-1');
    expect(component.booking).toBe(booking);
    expect(component.club).toEqual(expect.objectContaining({ clubId: 'club-1', clubName: 'Club One' }));
    expect(component.table).toEqual(expect.objectContaining({ tableId: 'table-1', tableNumber: 7, price: 200, discount: 25 }));
  });

  it('does not download the receipt during the initial booking load', () => {
    bookingService.getBooking.mockReturnValueOnce(of({ ...booking, hasReceipt: true }));

    component.ngOnInit();

    expect(component.hasReceipt).toBe(true);
    expect(component.receiptUrl).toBe('');
    expect(bookingService.getReceiptBlob).not.toHaveBeenCalled();
  });

  it('sets loading to true while the booking request is pending', () => {
    const subject = new Subject<any>();
    bookingService.getBooking.mockReturnValueOnce(subject.asObservable());

    component.ngOnInit();

    expect(component.loading).toBe(true);

    subject.next(booking);
    subject.complete();

    expect(component.loading).toBe(false);
  });

  it('shows an error when the booking fails to load', () => {
    bookingService.getBooking.mockReturnValueOnce(throwError(() => new Error('boom')));

    component.ngOnInit();

    expect(component.error).toBe('bookingUserHandler.errors.loadFailed');
    expect(component.loading).toBe(false);
  });

  it('cancels a booking, closes the modal and reloads it', () => {
    component.booking = booking;
    component.showCancelModal = true;

    component.cancelBooking();

    expect(bookingService.cancelBooking).toHaveBeenCalledWith('club-1', 'booking-1', undefined);
    expect(component.showCancelModal).toBe(false);
    expect(component.success).toBe('bookingUserHandler.success.cancelled');
  });

  it('allows editing the receipt only while the booking is pending', () => {
    component.booking = { ...booking, status: 'PENDIENTE' };
    expect(component.canEditReceipt()).toBe(true);

    component.booking = { ...booking, status: 'CONFIRMADA' };
    expect(component.canEditReceipt()).toBe(false);

    component.booking = { ...booking, status: 'CANCELADA' };
    expect(component.canEditReceipt()).toBe(false);
  });

  it('shows a specific message when uploading a receipt for an already-confirmed booking', () => {
    component.booking = { ...booking, status: 'CONFIRMADA' };
    bookingService.requireReceiptLink.mockReturnValue('/api/clubs/club-1/bookings/booking-1/receipt');
    bookingService.putReceipt.mockReturnValue(
      throwError(() => ({ error: { message: 'receipt.for.confirmed' } }))
    );

    const file = new File(['fake-image'], 'recibo.png', { type: 'image/png' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file], configurable: true });

    component.uploadReceipt({ target: input } as unknown as Event);

    expect(component.error).toBe('bookingUserHandler.errors.receiptForConfirmed');
  });
});
