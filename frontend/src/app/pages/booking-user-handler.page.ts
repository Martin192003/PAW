import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { Booking } from '../models/booking.model';
import { TranslateModule } from '@ngx-translate/core';
import { Club } from '../models/club.model';
import { ClubTable } from '../models/club-table.model';
import { User } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { BookingService } from '../services/booking.service';
import { ClubService } from '../services/club.service';
import { I18nService } from '../services/i18n.service';
import { argentinaDateCode, formatArgentinaDateTime } from '../utils/argentina-date.util';
import { normalizeLinks } from '../utils/api-url.util';

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
  error = '';
  success = '';
  showCancelModal = false;
  loading = false;

  private receiptObjectUrl = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authState: AuthStateService,
    private readonly bookingService: BookingService,
    private readonly clubService: ClubService,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.user = this.authState.getUser();
    this.load();
  }

  ngOnDestroy(): void {
    this.revokeReceiptUrl();
  }

  load(): void {
    const clubId = this.route.snapshot.paramMap.get('clubId');
    const bookingId = this.route.snapshot.paramMap.get('bookingId');
    if (!clubId || !bookingId) {
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

    this.bookingService.getBooking(clubId, bookingId).pipe(
      switchMap(booking => {
        this.booking = booking;
        const clubLink = this.extractLink(booking.links, 'club');
        return forkJoin({
          club: (clubLink ? this.clubService.getClubByLink(clubLink) : this.clubService.getClubById(booking.clubId))
            .pipe(catchError(() => of(undefined))),
          tables: this.clubService.getTables(booking.clubId).pipe(catchError(() => of([] as ClubTable[])))
        });
      })
    ).subscribe({
      next: ({ club, tables }) => {
        this.club = club;
        this.table = tables.find(table => table.tableId === this.booking?.tableId);
        this.loading = false;
        this.loadReceipt();
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
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
        this.success = this.i18n.instant('bookingUserHandler.success.cancelled');
        this.load();
      },
      error: () => {
        this.error = this.i18n.instant('bookingUserHandler.errors.cancelFailed');
        this.showCancelModal = false;
        this.cdr.detectChanges();
      }
    });
  }

  uploadReceipt(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.booking) return;

    const validationError = this.validateReceiptFile(file);
    if (validationError) {
      this.error = validationError;
      this.success = '';
      this.cdr.detectChanges();
      input.value = '';
      return;
    }

    let receiptLink = '';
    try {
      receiptLink = this.bookingService.requireReceiptLink(this.booking);
    } catch (err) {
      this.error = (err as Error).message;
      this.cdr.detectChanges();
      input.value = '';
      return;
    }

    this.bookingService.putReceipt(this.booking.clubId, this.booking.id, file, receiptLink).subscribe({
      next: () => {
        this.success = this.i18n.instant('bookingUserHandler.success.receiptUploaded');
        if (this.booking) {
          this.booking.hasReceipt = true;
        }
        this.loadReceipt();
      },
      error: error => {
        this.error = this.receiptUploadErrorMessage(error);
        this.success = '';
        this.cdr.detectChanges();
      }
    });
    input.value = '';
  }

  openReceipt(): void {
    if (this.receiptUrl) {
      window.open(this.receiptUrl, '_blank');
    }
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

  private loadReceipt(): void {
    if (!this.booking) return;

    if (!this.booking.hasReceipt) {
      this.revokeReceiptUrl();
      this.hasReceipt = false;
      this.receiptUrl = '';
      this.cdr.detectChanges();
      return;
    }

    let receiptLink = '';
    try {
      receiptLink = this.bookingService.requireReceiptLink(this.booking);
    } catch (err) {
      this.hasReceipt = false;
      this.receiptUrl = '';
      this.error = (err as Error).message;
      this.cdr.detectChanges();
      return;
    }
    this.bookingService.getReceiptBlob(this.booking.clubId, this.booking.id, receiptLink).subscribe({
      next: blob => {
        this.revokeReceiptUrl();
        this.receiptObjectUrl = URL.createObjectURL(blob);
        this.receiptUrl = this.receiptObjectUrl;
        this.hasReceipt = true;
        this.cdr.detectChanges();
      },
      error: () => {
        this.revokeReceiptUrl();
        this.hasReceipt = false;
        this.receiptUrl = '';
        this.cdr.detectChanges();
      }
    });
  }

  private extractLink(links: Booking['links'], rel: string): string | undefined {
    return normalizeLinks(links as any)?.[rel];
  }

  private revokeReceiptUrl(): void {
    if (this.receiptObjectUrl) {
      URL.revokeObjectURL(this.receiptObjectUrl);
      this.receiptObjectUrl = '';
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
