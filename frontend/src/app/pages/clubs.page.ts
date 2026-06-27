import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EventService } from '../services/event.service';
import { ClubEvent } from '../models/ClubEvent.model';
import { ViewChild, ElementRef } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { normalizeLinks } from '../utils/api-url.util';
import { skip } from 'rxjs/operators';


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
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.applyQueryParams(this.route.snapshot.queryParamMap);
    this.loadClubs();

    this.route.queryParamMap.pipe(skip(1)).subscribe((params) => {
      this.applyQueryParams(params);
      this.loadClubs();
    });

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

  private applyQueryParams(params: import('@angular/router').ParamMap): void {
    this.currentPage = Number(params.get('page') ?? '1') || 1;
    this.pageSize = Number(params.get('size') ?? '10') || 10;
    this.filters = {
      name: params.get('name') ?? '',
      zone: params.get('zone') ?? '',
      genre: params.get('genre') ?? '',
      price: params.get('price') ?? '',
      isBar: params.get('isBar') ?? '',
      sort: params.get('sort') ?? ''
    };
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.updateQueryParams();
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
    this.updateQueryParams();
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
      this.updateQueryParams();
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updateQueryParams();
    }
  }


  goToPage(page: number) {
    this.currentPage = page;
    this.updateQueryParams();
  }

  private updateQueryParams(): void {
    const queryParams: Record<string, string | number | null> = {
      page: this.currentPage,
      size: this.pageSize,
      name: this.filters.name || null,
      zone: this.filters.zone || null,
      genre: this.filters.genre || null,
      price: this.filters.price || null,
      isBar: this.filters.isBar || null,
      sort: this.filters.sort || null
    };

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: '',
      replaceUrl: true
    });
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
    const links = normalizeLinks(event.links);
    const clubLink = links?.['club'];
    return clubLink ? this.extractIdFromLink(clubLink, 'clubs') : undefined;
  }

  private extractIdFromLink(link: string, segment: string): string | undefined {
    try {
      const path = link.startsWith('http://') || link.startsWith('https://')
        ? new URL(link).pathname
        : link;
      const parts = path.split('/').filter(Boolean);
      const index = parts.indexOf(segment);
      return index >= 0 ? parts[index + 1] : undefined;
    } catch {
      return undefined;
    }
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
