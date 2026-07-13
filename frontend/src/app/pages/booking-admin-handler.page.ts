import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';

import { Booking } from '../models/booking.model';
import { Club } from '../models/club.model';
import { ClubTable } from '../models/club-table.model';
import { User } from '../models/user.model';
import { BookingService } from '../services/booking.service';
import { ClubService } from '../services/club.service';
import { UserService } from '../services/user.service';
import { I18nService } from '../services/i18n.service';
import { argentinaDateCode, formatArgentinaDateTime } from '../utils/argentina-date.util';
import { normalizeLinks } from '../utils/api-url.util';

@Component({
  standalone: true,
  selector: 'app-booking-admin-handler-page',
  imports: [CommonModule, TranslateModule],
  templateUrl: './booking-admin-handler.page.html'
})
export class BookingAdminHandlerPage implements OnInit, OnDestroy {
  booking?: Booking;
  club?: Club;
  table?: ClubTable;
  user?: User;
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
    private readonly bookingService: BookingService,
    private readonly clubService: ClubService,
    private readonly userService: UserService,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.revokeReceiptUrl();
  }

  load(): void {
    const bookingId = this.route.snapshot.paramMap.get('bookingId');
    if (!bookingId) {
      this.error = this.i18n.instant('bookingAdminHandler.errors.invalidBooking');
      return;
    }

    // Sin este flag la pantalla se queda en blanco (solo el header) mientras
    // espera la respuesta, lo que parece una pantalla rota en vez de una
    // carga en curso.
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.bookingService.getManagedClubBooking(bookingId).pipe(
      switchMap(booking => {
        this.booking = booking;
        const clubLink = this.extractLink(booking.links, 'club');
        return forkJoin({
          club: (clubLink ? this.clubService.getClubByLink(clubLink) : this.clubService.getClubById(booking.clubId))
            .pipe(catchError(() => of(undefined))),
          tables: this.clubService.getTables(booking.clubId).pipe(catchError(() => of([] as ClubTable[]))),
          user: this.userService.getUserById(booking.userId).pipe(catchError(() => of(undefined)))
        });
      })
    ).subscribe({
      next: ({ club, tables, user }) => {
        this.club = club;
        this.table = tables.find(table => table.tableId === this.booking?.tableId);
        this.user = user;
        this.loading = false;
        this.loadReceipt();
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.error = this.i18n.instant('bookingAdminHandler.errors.loadFailed');
        this.cdr.detectChanges();
      }
    });
  }

  confirmBooking(): void {
    if (!this.booking) return;
    this.bookingService.confirmManagedClubBooking(this.booking.id, this.booking.links).subscribe({
      next: () => {
        this.success = this.i18n.instant('bookingAdminHandler.success.confirmed');
        this.load();
      },
      error: error => {
        this.error = this.confirmErrorMessage(error);
        this.cdr.detectChanges();
      }
    });
  }

  cancelBooking(): void {
    if (!this.booking) return;
    this.bookingService.cancelManagedClubBooking(this.booking.id, this.booking.links).subscribe({
      next: () => {
        this.showCancelModal = false;
        this.success = this.i18n.instant('bookingAdminHandler.success.cancelled');
        this.load();
      },
      error: () => {
        this.showCancelModal = false;
        this.error = this.i18n.instant('bookingAdminHandler.errors.cancelFailed');
        this.cdr.detectChanges();
      }
    });
  }

  openReceipt(): void {
    if (this.receiptUrl) {
      window.open(this.receiptUrl, '_blank');
    }
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
