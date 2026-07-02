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

@Component({
  standalone: true,
  selector: 'app-booking-user-handler-page',
  imports: [CommonModule, TranslateModule],
  templateUrl: './booking-user-handler.page.html'
})
export class BookingUserHandlerPage implements OnInit, OnDestroy {
  booking?: Booking;
  club?: Club;
  table?: ClubTable;
  user: User | null = null;
  receiptUrl = '';
  hasReceipt = false;
  error = '';
  success = '';
  showCancelModal = false;

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

    this.bookingService.getBooking(clubId, bookingId).pipe(
      switchMap(booking => {
        this.booking = booking;
        return forkJoin({
          club: this.clubService.getClubById(booking.clubId).pipe(catchError(() => of(undefined))),
          tables: this.clubService.getTables(booking.clubId).pipe(catchError(() => of([] as ClubTable[])))
        });
      })
    ).subscribe({
      next: ({ club, tables }) => {
        this.club = club;
        this.table = tables.find(table => table.tableId === this.booking?.tableId);
        this.loadReceipt();
        this.cdr.detectChanges();
      },
      error: () => {
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
      error: () => {
        this.error = this.i18n.instant('bookingUserHandler.errors.receiptUploadFailed');
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

  private revokeReceiptUrl(): void {
    if (this.receiptObjectUrl) {
      URL.revokeObjectURL(this.receiptObjectUrl);
      this.receiptObjectUrl = '';
    }
  }
}
