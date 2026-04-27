import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { Booking, BookingFilters } from '../models/booking.model';
import { Club } from '../models/club.model';
import { ClubTable } from '../models/club-table.model';
import { BookingService } from '../services/booking.service';
import { ClubService } from '../services/club.service';
import { AuthStateService } from '../services/auth-state.service';

@Component({
  standalone: true,
  selector: 'app-managed-club-bookings-page',
  imports: [CommonModule, FormsModule, RouterLink, SideBarComponent],
  templateUrl: './managed-club-bookings.page.html'
})
export class ManagedClubBookingsPage implements OnInit {
  bookings: Booking[] = [];
  clubs: Club[] = [];
  clubNames: Record<string, string> = {};
  tableNumbers: Record<string, number> = {};

  filters: BookingFilters = {
    sort: 'created_desc'
  };

  currentPage = 1;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error = '';

  constructor(
    private readonly bookingService: BookingService,
    private readonly clubService: ClubService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadClubs();
    this.loadBookings();
  }

  loadClubs(): void {
    const userId = this.authState.getUser()?.userId;
    if (!userId) {
      return;
    }

    this.clubService.getClubsByUser(userId, 1, 100).subscribe({
      next: response => {
        this.clubs = response.content ?? [];
        this.clubs.forEach(club => {
          this.clubNames[club.clubId] = club.clubName;
        });
        this.cdr.detectChanges();
      }
    });
  }

  loadBookings(): void {
    this.loading = true;
    this.error = '';

    this.bookingService.listManagedClubBookings(this.filters, this.currentPage, this.pageSize).subscribe({
      next: response => {
        this.bookings = response.content ?? [];
        this.currentPage = response.page || this.currentPage;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || 0;
        this.loading = false;
        this.ensureBookingMetadata();
        this.cdr.detectChanges();
      },
      error: () => {
        this.bookings = [];
        this.loading = false;
        this.error = 'No se pudieron cargar las reservas.';
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadBookings();
  }

  resetFilters(): void {
    this.filters = { sort: 'created_desc' };
    this.currentPage = 1;
    this.loadBookings();
  }

  goToBooking(booking: Booking): void {
    this.router.navigate(['/admin/bookings', booking.id]);
  }

  prevPage(): void {
    if (this.currentPage <= 1) return;
    this.currentPage -= 1;
    this.loadBookings();
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages) return;
    this.currentPage += 1;
    this.loadBookings();
  }

  hasActiveFilters(): boolean {
    return !!(
      this.filters.clubId ||
      this.filters.status ||
      this.filters.bookingDate ||
      this.filters.hasReceipt ||
      (this.filters.sort && this.filters.sort !== 'created_desc')
    );
  }

  statusClasses(status: string): string {
    switch ((status || '').trim().toUpperCase()) {
      case 'PENDIENTE':
        return 'border-yellow-500/60 bg-yellow-950 text-yellow-200';
      case 'CONFIRMADA':
        return 'border-green-500/60 bg-green-950 text-green-200';
      case 'CANCELADA':
        return 'border-rose-500/60 bg-rose-950 text-rose-200';
      default:
        return 'border-white/20 bg-white/10 text-white';
    }
  }

  statusLabel(status: string): string {
    const normalized = (status || '').trim().toUpperCase();
    if (normalized === 'PENDIENTE') return 'Pendiente';
    if (normalized === 'CONFIRMADA') return 'Confirmada';
    if (normalized === 'CANCELADA') return 'Cancelada';
    return status;
  }

  formatDate(value: string): string {
    if (!value) return '-';
    return new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'long', year: 'numeric' })
      .format(new Date(value));
  }

  bookingCode(booking: Booking): string {
    const date = new Date(booking.bookingStart);
    const day = `${date.getDate()}`.padStart(2, '0');
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const year = date.getFullYear();
    const tableNumber = this.tableNumbers[booking.tableId] ?? booking.tableId;
    return `${day}-${month}-${year}-#${tableNumber}`;
  }

  private ensureBookingMetadata(): void {
    const missingClubIds = Array.from(new Set(this.bookings.map(b => b.clubId)))
      .filter(clubId => clubId && !this.clubNames[clubId]);
    const missingTableClubIds = Array.from(new Set(this.bookings.map(b => b.clubId)))
      .filter(clubId => clubId && this.bookings.some(booking => booking.clubId === clubId && !this.tableNumbers[booking.tableId]));

    if (missingClubIds.length === 0 && missingTableClubIds.length === 0) {
      return;
    }

    forkJoin({
      clubs: missingClubIds.length
        ? forkJoin(missingClubIds.map(clubId => this.clubService.getClubById(clubId).pipe(catchError(() => of(null)))))
        : of([] as Array<Club | null>),
      tableGroups: missingTableClubIds.length
        ? forkJoin(missingTableClubIds.map(clubId => this.clubService.getTables(clubId).pipe(catchError(() => of([] as ClubTable[])))))
        : of([] as ClubTable[][])
    }).subscribe(({ clubs, tableGroups }) => {
      clubs.forEach(club => {
        if (club) {
          this.clubNames[club.clubId] = club.clubName;
        }
      });
      tableGroups.flat().forEach(table => {
        this.tableNumbers[table.tableId] = table.tableNumber;
      });
      this.cdr.detectChanges();
    });
  }
}
