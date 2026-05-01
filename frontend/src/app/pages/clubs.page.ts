import { Component, OnDestroy, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EventService } from '../services/event.service';
import { ClubEvent } from '../models/ClubEvent.model';
import { ViewChild, ElementRef } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';


type ClubListItem = Club & { _hasError?: boolean };

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule],
  templateUrl: './clubs.page.html',
})
export class ClubsPage implements OnInit, OnDestroy {
  clubs: ClubListItem[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;

  upcomingEvents: ClubEvent[] = [];
  currentEventIndex = 0;
  eventAvailabilityModalOpen = false;
  unavailableEventName = '';
  unavailableClubName = '';
  checkingEventId?: string;
  private previousBodyOverflow?: string;
  private previousDocumentOverflow?: string;

  readonly filterOptions = {
    zones: [
      { value: 'ZONA_NORTE', labelKey: 'clubs.filterOptions.zones.norte' },
      { value: 'ZONA_OESTE', labelKey: 'clubs.filterOptions.zones.oeste' },
      { value: 'ZONA_SUR', labelKey: 'clubs.filterOptions.zones.sur' },
      { value: 'ZONA_CABA', labelKey: 'clubs.filterOptions.zones.caba' }
    ],
    genres: [
      { value: 'ROCK', labelKey: 'clubs.filterOptions.genres.rock' },
      { value: 'POP', labelKey: 'clubs.filterOptions.genres.pop' },
      { value: 'CACHENGUE', labelKey: 'clubs.filterOptions.genres.cachengue' },
      { value: 'TECHNO', labelKey: 'clubs.filterOptions.genres.techno' },
      { value: 'REGGAETON', labelKey: 'clubs.filterOptions.genres.reggaeton' },
      { value: 'CUMBIA', labelKey: 'clubs.filterOptions.genres.cumbia' },
      { value: 'TRAP', labelKey: 'clubs.filterOptions.genres.trap' },
      { value: 'JAZZ', labelKey: 'clubs.filterOptions.genres.jazz' },
      { value: 'ELECTRONICA', labelKey: 'clubs.filterOptions.genres.electronica' },
      { value: 'AFROBEAT', labelKey: 'clubs.filterOptions.genres.afrobeat' },
      { value: 'OTRO', labelKey: 'clubs.filterOptions.genres.other' }
    ],
    prices: [
      { value: 'bajo', labelKey: 'clubs.filterOptions.prices.low' },
      { value: 'medio', labelKey: 'clubs.filterOptions.prices.medium' },
      { value: 'alto', labelKey: 'clubs.filterOptions.prices.high' }
    ],
    sorts: [
      { value: 'availability_d0_desc', labelKey: 'clubs.filterOptions.sorts.availableToday' },
      { value: 'availability_d1_desc', labelKey: 'clubs.filterOptions.sorts.availableTomorrow' },
      { value: 'availability_d2_desc', labelKey: 'clubs.filterOptions.sorts.availableDayAfter' },
      { value: 'rating_desc', labelKey: 'clubs.filterOptions.sorts.ratingHighToLow' },
      { value: 'rating_asc', labelKey: 'clubs.filterOptions.sorts.ratingLowToHigh' }
    ]
  };

  filters: { name: string; zone: string; genre: string; price: string; isBar: string; sort: string } = {
    name: '',
    zone: '',
    genre: '',
    price: '',
    isBar: '',
    sort: ''
  };

  constructor(
    private clubService: ClubService,
    private eventService: EventService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.loadClubs();
    this.loadUpcomingEvents();
  }

  ngOnDestroy(): void {
    this.setEventAvailabilityModalOpen(false);
  }

  loadClubs() {
    this.clubService.getClubs(this.currentPage, this.pageSize, this.filters)
      .subscribe({
        next: (response) => {
          this.clubs = response.content.map(club => ({
            ...club,
            _hasError: false
          }));
          this.totalPages = response.totalPages;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error loading clubs:', err);
        }
      });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadClubs();
  }

  resetFilters(): void {
    this.filters = {
      name: '',
      zone: '',
      genre: '',
      price: '',
      isBar: '',
      sort: ''
    };
    this.currentPage = 1;
    this.loadClubs();
  }

  get hasActiveFilters(): boolean {
    return Object.values(this.filters).some((value) => value.trim() !== '');
  }

  onImageError(event: Event, club: ClubListItem) {
    club._hasError = true;
    this.cdr.detectChanges();
  }

  onImageLoad() {
    this.cdr.detectChanges();
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadClubs();
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadClubs();
    }
  }


  goToPage(page: number) {
    this.currentPage = page;
    this.loadClubs();
  }

  get pages(): number[] {
    if (this.totalPages <= 3) {
      return Array.from({ length: this.totalPages }, (_, i) => i + 1);
    }

    if (this.currentPage <= 2) {
      return [1, 2, 3];
    }

    if (this.currentPage >= this.totalPages - 1) {
      return [
        this.totalPages - 2,
        this.totalPages - 1,
        this.totalPages
      ];
    }

    return [
      this.currentPage - 1,
      this.currentPage,
      this.currentPage + 1
    ];
  }

  loadUpcomingEvents(): void {
    this.eventService.getUpcomingEvents(0, 10).subscribe({
      next: (res) => {
        this.upcomingEvents = res.content;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading events', err);
      }
    });
  }

  onEventClick(event: ClubEvent): void {
    const clubId = event.clubId ?? this.extractClubId(event);
    const eventDate = this.normalizeEventDate(event.date);

    if (!clubId || !eventDate) {
      this.openNoTablesModal(event);
      return;
    }

    this.checkingEventId = event.eventId;
    this.clubService.getTables(clubId, eventDate).subscribe({
      next: tables => {
        this.checkingEventId = undefined;
        if ((tables ?? []).length === 0) {
          this.openNoTablesModal(event);
          return;
        }

        this.router.navigate(['/clubs', clubId], {
          queryParams: {
            date: eventDate,
            fromEvent: 'true'
          },
          fragment: 'dd-tables'
        });
      },
      error: () => {
        this.checkingEventId = undefined;
        this.openNoTablesModal(event);
      }
    });
  }

  closeEventAvailabilityModal(): void {
    this.setEventAvailabilityModalOpen(false);
    this.unavailableEventName = '';
    this.unavailableClubName = '';
  }

  private openNoTablesModal(event: ClubEvent): void {
    this.unavailableEventName = event.name;
    this.unavailableClubName = event.clubName;
    this.setEventAvailabilityModalOpen(true);
    this.cdr.detectChanges();
  }

  private extractClubId(event: ClubEvent): string | undefined {
    return event.links?.['club']?.split('/clubs/')[1];
  }

  private normalizeEventDate(raw: string): string | undefined {
    const date = (raw || '').slice(0, 10);
    return /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : undefined;
  }

  private setEventAvailabilityModalOpen(open: boolean): void {
    this.eventAvailabilityModalOpen = open;

    if (typeof document === 'undefined') {
      return;
    }

    if (open) {
      if (this.previousBodyOverflow === undefined) {
        this.previousBodyOverflow = document.body.style.overflow;
      }
      if (this.previousDocumentOverflow === undefined) {
        this.previousDocumentOverflow = document.documentElement.style.overflow;
      }
      document.body.style.overflow = 'hidden';
      document.documentElement.style.overflow = 'hidden';
      return;
    }

    if (this.previousBodyOverflow !== undefined) {
      document.body.style.overflow = this.previousBodyOverflow;
      this.previousBodyOverflow = undefined;
    }
    if (this.previousDocumentOverflow !== undefined) {
      document.documentElement.style.overflow = this.previousDocumentOverflow;
      this.previousDocumentOverflow = undefined;
    }
  }

  @ViewChild('carouselTrack') carouselTrack!: ElementRef<HTMLDivElement>;

  nextEvent(): void {
    this.carouselTrack.nativeElement.scrollBy({ left: 280, behavior: 'smooth' });
  }

  prevEvent(): void {
    this.carouselTrack.nativeElement.scrollBy({ left: -280, behavior: 'smooth' });
  }


  onEventImageError(event: Event, clubEvent: ClubEvent): void {
    (clubEvent as any)._photoError = true;
    this.cdr.detectChanges();
  }
}
