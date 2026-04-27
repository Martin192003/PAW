import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { Booking } from '../models/booking.model';
import { Club } from '../models/club.model';
import { ClubTable } from '../models/club-table.model';
import { User } from '../models/user.model';
import { BookingService } from '../services/booking.service';
import { ClubService } from '../services/club.service';
import { UserService } from '../services/user.service';

@Component({
  standalone: true,
  selector: 'app-booking-admin-handler-page',
  imports: [CommonModule],
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

  private receiptObjectUrl = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookingService: BookingService,
    private readonly clubService: ClubService,
    private readonly userService: UserService,
    private readonly cdr: ChangeDetectorRef
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
      this.error = 'Reserva inválida.';
      return;
    }

    this.bookingService.getManagedClubBooking(bookingId).pipe(
      switchMap(booking => {
        this.booking = booking;
        return forkJoin({
          club: this.clubService.getClubById(booking.clubId).pipe(catchError(() => of(undefined))),
          tables: this.clubService.getTables(booking.clubId).pipe(catchError(() => of([] as ClubTable[]))),
          user: this.userService.getUserById(booking.userId).pipe(catchError(() => of(undefined)))
        });
      })
    ).subscribe({
      next: ({ club, tables, user }) => {
        this.club = club;
        this.table = tables.find(table => table.tableId === this.booking?.tableId);
        this.user = user;
        this.loadReceipt();
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la reserva.';
        this.cdr.detectChanges();
      }
    });
  }

  confirmBooking(): void {
    if (!this.booking) return;
    this.bookingService.confirmManagedClubBooking(this.booking.id).subscribe({
      next: () => {
        this.success = 'Reserva confirmada correctamente.';
        this.load();
      },
      error: () => {
        this.error = 'No se pudo confirmar la reserva.';
        this.cdr.detectChanges();
      }
    });
  }

  cancelBooking(): void {
    if (!this.booking) return;
    this.bookingService.cancelManagedClubBooking(this.booking.id).subscribe({
      next: () => {
        this.showCancelModal = false;
        this.success = 'Reserva cancelada correctamente.';
        this.load();
      },
      error: () => {
        this.showCancelModal = false;
        this.error = 'No se pudo cancelar la reserva.';
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
    if (!value) return '-';
    return new Intl.DateTimeFormat('es-AR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  }

  bookingCode(): string {
    if (!this.booking) return '-';
    const date = new Date(this.booking.bookingStart);
    if (Number.isNaN(date.getTime())) return this.table?.tableNumber ? `#${this.table.tableNumber}` : this.booking.tableId;
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const tableNumber = this.table?.tableNumber ?? this.booking.tableId;
    return `${day}-${month}-${year}-#${tableNumber}`;
  }

  private loadReceipt(): void {
    if (!this.booking) return;
    this.bookingService.getReceiptBlob(this.booking.clubId, this.booking.id).subscribe({
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
