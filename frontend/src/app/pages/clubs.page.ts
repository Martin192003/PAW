import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { RouterModule } from '@angular/router';
import { EventService } from '../services/event.service';
import { ClubEvent } from '../models/ClubEvent.model';
import { ViewChild, ElementRef } from '@angular/core';


type ClubListItem = Club & { _hasError?: boolean };

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './clubs.page.html',
})
export class ClubsPage implements OnInit {
  clubs: ClubListItem[] = [];
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;

  upcomingEvents: ClubEvent[] = [];
  currentEventIndex = 0;

  constructor(
    private clubService: ClubService,
    private eventService: EventService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadClubs();
    this.loadUpcomingEvents();
  }

  loadClubs() {
    this.clubService.getClubs(this.currentPage, this.pageSize)
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
        console.log('Backend response:', res);
        console.log('Mapped events:', res.content);
        this.upcomingEvents = res.content;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading events', err);
      }
    });
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
