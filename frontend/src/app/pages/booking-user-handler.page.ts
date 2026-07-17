import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { Booking } from '../models/booking.model';
import { TranslateModule } from '@ngx-translate/core';
import { Club } from '../models/club.model';
import { ClubTable } from '../models/club-table.model';
import { User } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { BookingService } from '../services/booking.service';
import { I18nService } from '../services/i18n.service';
import { argentinaDateCode, formatArgentinaDateTime } from '../utils/argentina-date.util';

@Component({
  standalone: true,
  selector: 'app-booking-user-handler-page',
  imports: [CommonModule, TranslateModule],
  templateUrl: './booking-user-handler.page.html',
  styles: [`
    :host button:not(:disabled),
    :host label {
      cursor: pointer;
    }

    :host button:disabled {
      cursor: not-allowed;
    }
  `]
})
export class BookingUserHandlerPage implements OnInit, OnDestroy {
  private static readonly maxReceiptBytes = 5 * 1024 * 1024;
  private static readonly allowedReceiptTypes = new Set(['image/jpeg', 'image/jpg', 'image/pjpeg', 'image/png', 'image/x-png', 'image/webp']);
  private static readonly allowedReceiptExtensions = /\.(jpe?g|png|webp)$/i;

  booking?: Booking;
  club?: Club;
  table?: ClubTable;
  user: User | null = null;
  receiptUrl = '';
  hasReceipt = false;
  receiptLoading = false;
  error = '';
  success = '';
  showCancelModal = false;
  loading = false;

  private receiptObjectUrl = '';
  private messageTimeoutId: ReturnType<typeof setTimeout> | undefined;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authState: AuthStateService,
    private readonly bookingService: BookingService,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.user = this.authState.getUser();
    this.load();
  }

  ngOnDestroy(): void {
    this.clearMessageTimeout();
    this.revokeReceiptUrl();
  }

  load(): void {
    const clubId = this.route.snapshot.paramMap.get('clubId');
    const bookingId = this.route.snapshot.paramMap.get('bookingId');
    if (!clubId || !bookingId) {
      this.clearMessageTimeout();
      this.error = this.i18n.instant('bookingUserHandler.errors.invalidBooking');
      return;
    }

    // Sin este flag la pantalla se queda completamente en blanco (solo el
    // header) mientras espera la respuesta, lo que parece un error/pantalla
    // rota en vez de una carga en curso, sobre todo justo despues de crear
    // la reserva.
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.bookingService.getBooking(clubId, bookingId).subscribe({
      next: booking => {
        this.booking = booking;
        this.applyInlineDetails(booking);
        this.applyReceiptState(booking);
        // Load receipt image automatically if it exists
        if (booking.hasReceipt) {
          this.loadReceipt();
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.clearMessageTimeout();
        this.error = this.i18n.instant('bookingUserHandler.errors.loadFailed');
        this.cdr.detectChanges();
      }
    });
  }

  cancelBooking(): void {
    if (!this.booking) return;
    this.bookingService.cancelBooking(this.booking.clubId, this.booking.id, this.booking.links).subscribe({
      next: () => {
        this.showCancelModal = false;
        this.showSuccess(this.i18n.instant('bookingUserHandler.success.cancelled'));
        this.load();
      },
      error: () => {
        this.showError(this.i18n.instant('bookingUserHandler.errors.cancelFailed'));
        this.showCancelModal = false;
      }
    });
  }

  uploadReceipt(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.booking) return;

    const validationError = this.validateReceiptFile(file);
    if (validationError) {
      this.showError(validationError);
      input.value = '';
      return;
    }

    let receiptLink = '';
    try {
      receiptLink = this.bookingService.requireReceiptLink(this.booking);
    } catch (err) {
      this.showError((err as Error).message);
      input.value = '';
      return;
    }

    this.bookingService.putReceipt(this.booking.clubId, this.booking.id, file, receiptLink).subscribe({
      next: () => {
        this.showSuccess(this.i18n.instant('bookingUserHandler.success.receiptUploaded'));
        if (this.booking) {
          this.booking.hasReceipt = true;
        }
        this.showReceiptBlob(file);
      },
      error: error => {
        this.showError(this.receiptUploadErrorMessage(error));
      }
    });
    input.value = '';
  }

  openReceipt(): void {
    if (!this.booking || !this.hasReceipt) return;
    if (this.receiptUrl) {
      window.open(this.receiptUrl, '_blank');
      return;
    }
    const receiptWindow = window.open('', '_blank');
    this.loadReceipt(receiptWindow);
  }

  goBack(): void {
    this.router.navigate(['/bookings']);
  }

  statusClasses(status: string): string {
    switch ((status || '').trim().toUpperCase()) {
      case 'PENDIENTE': return 'border-yellow-500/60 bg-yellow-950 text-yellow-200';
      case 'CONFIRMADA': return 'border-green-500/60 bg-green-950 text-green-200';
      case 'CANCELADA': return 'border-rose-500/60 bg-rose-950 text-rose-200';
      default: return 'border-white/20 bg-white/10 text-white';
    }
  }

  isCancelled(): boolean {
    return (this.booking?.status || '').trim().toUpperCase() === 'CANCELADA';
  }

  isConfirmed(): boolean {
    return (this.booking?.status || '').trim().toUpperCase() === 'CONFIRMADA';
  }

  // Una vez que el admin confirma la reserva, el comprobante queda fijo: ya
  // se uso para validar el pago, asi que no tendria sentido dejar que el
  // usuario lo reemplace o lo borre despues de esa validacion.
  canEditReceipt(): boolean {
    return !this.isCancelled() && !this.isConfirmed();
  }

  priceToPay(): number {
    const price = Number(this.table?.price ?? this.booking?.totalAmount ?? 0);
    const discount = Number(this.table?.discount ?? 0);
    return discount > 0 ? price * (1 - discount / 100) : price;
  }

  formatDateTime(value?: string): string {
    return formatArgentinaDateTime(value, this.i18n.getLanguage());
  }

  bookingCode(): string {
    if (!this.booking) return '-';
    const dateCode = argentinaDateCode(this.booking.bookingStart);
    if (!dateCode) return this.table?.tableNumber ? `#${this.table.tableNumber}` : this.booking.tableId;
    const tableNumber = this.table?.tableNumber ?? this.booking.tableId;
    return `${dateCode}-#${tableNumber}`;
  }

  private loadReceipt(receiptWindow?: Window | null): void {
    if (!this.booking) {
      receiptWindow?.close();
      return;
    }

    if (!this.booking.hasReceipt && !this.hasReceipt) {
      this.revokeReceiptUrl();
      this.hasReceipt = false;
      this.receiptUrl = '';
      this.receiptLoading = false;
      this.cdr.detectChanges();
      receiptWindow?.close();
      return;
    }

    let receiptLink = '';
    try {
      receiptLink = this.bookingService.requireReceiptLink(this.booking);
    } catch (err) {
      this.hasReceipt = false;
      this.receiptUrl = '';
      this.receiptLoading = false;
      this.clearMessageTimeout();
      this.error = (err as Error).message;
      this.cdr.detectChanges();
      receiptWindow?.close();
      return;
    }
    this.receiptLoading = true;
    this.cdr.detectChanges();
    this.bookingService.getReceiptBlob(this.booking.clubId, this.booking.id, receiptLink).subscribe({
      next: blob => {
        this.showReceiptBlob(blob);
        if (receiptWindow) {
          receiptWindow.location.href = this.receiptUrl;
        }
      },
      error: () => {
        this.revokeReceiptUrl();
        this.hasReceipt = false;
        this.receiptUrl = '';
        this.receiptLoading = false;
        this.cdr.detectChanges();
        receiptWindow?.close();
      }
    });
  }

  private applyInlineDetails(booking: Booking): void {
    this.club = booking.clubName
      ? ({ clubId: booking.clubId, clubName: booking.clubName } as Club)
      : undefined;
    this.table = booking.tableNumber !== undefined && booking.tableNumber !== null
      ? ({
          tableId: booking.tableId,
          tableNumber: booking.tableNumber,
          price: booking.tablePrice ?? booking.totalAmount ?? 0,
          discount: booking.tableDiscount ?? 0
        } as ClubTable)
      : undefined;
  }

  private applyReceiptState(booking: Booking): void {
    this.revokeReceiptUrl();
    this.receiptUrl = '';
    this.hasReceipt = !!booking.hasReceipt;
    this.receiptLoading = false;
  }

  private showReceiptBlob(blob: Blob): void {
    this.revokeReceiptUrl();
    this.receiptObjectUrl = URL.createObjectURL(blob);
    this.receiptUrl = this.receiptObjectUrl;
    this.hasReceipt = true;
    this.receiptLoading = false;
    this.cdr.detectChanges();
  }

  private revokeReceiptUrl(): void {
    if (this.receiptObjectUrl) {
      URL.revokeObjectURL(this.receiptObjectUrl);
      this.receiptObjectUrl = '';
    }
  }

  private showSuccess(message: string): void {
    this.success = message;
    this.error = '';
    this.scheduleMessageClear();
    this.cdr.detectChanges();
  }

  private showError(message: string): void {
    this.error = message;
    this.success = '';
    this.scheduleMessageClear();
    this.cdr.detectChanges();
  }

  private scheduleMessageClear(): void {
    this.clearMessageTimeout();
    this.messageTimeoutId = setTimeout(() => {
      this.success = '';
      this.error = '';
      this.messageTimeoutId = undefined;
      this.cdr.detectChanges();
    }, 4000);
  }

  private clearMessageTimeout(): void {
    if (this.messageTimeoutId) {
      clearTimeout(this.messageTimeoutId);
      this.messageTimeoutId = undefined;
    }
  }

  private validateReceiptFile(file: File): string | null {
    if (file.size <= 0) {
      return this.i18n.instant('bookingUserHandler.errors.receiptEmpty');
    }
    if (file.size > BookingUserHandlerPage.maxReceiptBytes) {
      return this.i18n.instant('bookingUserHandler.errors.receiptTooLarge');
    }

    const type = (file.type || '').trim().toLowerCase();
    if (type && BookingUserHandlerPage.allowedReceiptTypes.has(type)) {
      return null;
    }
    if ((!type || type === 'application/octet-stream') && BookingUserHandlerPage.allowedReceiptExtensions.test(file.name || '')) {
      return null;
    }
    return this.i18n.instant('bookingUserHandler.errors.receiptUnsupportedType');
  }

  private receiptUploadErrorMessage(error: unknown): string {
    const message = this.extractErrorMessage(error);
    switch (message) {
      case 'receipt.empty':
        return this.i18n.instant('bookingUserHandler.errors.receiptEmpty');
      case 'receipt.tooLarge':
        return this.i18n.instant('bookingUserHandler.errors.receiptTooLarge');
      case 'receipt.unsupportedType':
        return this.i18n.instant('bookingUserHandler.errors.receiptUnsupportedType');
      case 'receipt.for.cancelled':
        return this.i18n.instant('bookingUserHandler.errors.receiptForCancelled');
      case 'receipt.for.confirmed':
        return this.i18n.instant('bookingUserHandler.errors.receiptForConfirmed');
      default:
        return this.i18n.instant('bookingUserHandler.errors.receiptUploadFailed');
    }
  }

  private extractErrorMessage(error: unknown): string | null {
    const payload = (error as { error?: unknown })?.error;
    if (payload && typeof payload === 'object' && 'message' in payload) {
      const message = (payload as { message?: unknown }).message;
      return typeof message === 'string' ? message : null;
    }
    return typeof payload === 'string' && payload.trim() ? payload.trim() : null;
  }
}
