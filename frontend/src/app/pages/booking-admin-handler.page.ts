import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { Booking } from '../models/booking.model';
import { Club } from '../models/club.model';
import { ClubTable } from '../models/club-table.model';
import { PublicUser } from '../models/user.model';
import { BookingService } from '../services/booking.service';
import { I18nService } from '../services/i18n.service';
import { argentinaDateCode, formatArgentinaDateTime } from '../utils/argentina-date.util';

@Component({
  standalone: true,
  selector: 'app-booking-admin-handler-page',
  imports: [CommonModule, TranslateModule],
  templateUrl: './booking-admin-handler.page.html',
  styles: [`
    :host button:not(:disabled) {
      cursor: pointer;
    }

    :host button:disabled {
      cursor: not-allowed;
    }
  `]
})
export class BookingAdminHandlerPage implements OnInit, OnDestroy {
  booking?: Booking;
  club?: Club;
  table?: ClubTable;
  user?: PublicUser;
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
    private readonly bookingService: BookingService,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.clearMessageTimeout();
    this.revokeReceiptUrl();
  }

  load(): void {
    const bookingId = this.route.snapshot.paramMap.get('bookingId');
    if (!bookingId) {
      this.clearMessageTimeout();
      this.error = this.i18n.instant('bookingAdminHandler.errors.invalidBooking');
      return;
    }

    // Sin este flag la pantalla se queda en blanco (solo el header) mientras
    // espera la respuesta, lo que parece una pantalla rota en vez de una
    // carga en curso.
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.bookingService.getManagedClubBooking(bookingId).subscribe({
      next: booking => {
        this.booking = booking;
        this.applyInlineDetails(booking);
        this.applyReceiptState(booking);
        // El comprobante NO se descarga en la carga inicial: es privado y solo
        // se pide bajo demanda cuando el admin toca "ver" (openReceipt()). Asi
        // se evita consumo de red y object URLs innecesarias en cada carga.
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.clearMessageTimeout();
        this.error = this.i18n.instant('bookingAdminHandler.errors.loadFailed');
        this.cdr.detectChanges();
      }
    });
  }

  confirmBooking(): void {
    if (!this.booking) return;
    this.bookingService.confirmManagedClubBooking(this.booking.id, this.booking.links).subscribe({
      next: () => {
        this.showSuccess(this.i18n.instant('bookingAdminHandler.success.confirmed'));
        this.load();
      },
      error: error => {
        this.showError(this.confirmErrorMessage(error));
      }
    });
  }

  cancelBooking(): void {
    if (!this.booking) return;
    this.bookingService.cancelManagedClubBooking(this.booking.id, this.booking.links).subscribe({
      next: () => {
        this.showCancelModal = false;
        this.showSuccess(this.i18n.instant('bookingAdminHandler.success.cancelled'));
        this.load();
      },
      error: () => {
        this.showCancelModal = false;
        this.showError(this.i18n.instant('bookingAdminHandler.errors.cancelFailed'));
      }
    });
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
    this.router.navigate(['/admin/bookings']);
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
    this.user = booking.userUsername
      ? ({
          userId: booking.userId,
          username: booking.userUsername,
          name: booking.userName ?? '',
          surname: booking.userSurname ?? ''
        } as PublicUser)
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

  private confirmErrorMessage(error: unknown): string {
    const message = this.extractErrorMessage(error);
    switch (message) {
      case 'receipt.required':
        return this.i18n.instant('bookingAdminHandler.errors.receiptRequired');
      default:
        return this.i18n.instant('bookingAdminHandler.errors.confirmFailed');
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
