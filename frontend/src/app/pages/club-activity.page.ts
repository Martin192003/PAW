import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { skip, takeUntil } from 'rxjs/operators';

import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { AuthStateService } from '../services/auth-state.service';

type ClubActivityItem = Club & { _hasError?: boolean };

@Component({
  standalone: true,
  selector: 'app-club-activity-page',
  imports: [CommonModule, RouterLink, SideBarComponent, TranslateModule],
  templateUrl: './club-activity.page.html',
  styleUrl: './club-activity.page.css'
})
export class ClubActivityPage implements OnInit, OnDestroy {
  clubs: ClubActivityItem[] = [];

  currentPage = 1;
  pageSize = 3;
  totalPages = 0;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly clubService: ClubService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.applyQueryParams(this.route.snapshot.queryParamMap);
    this.loadClubs();

    this.route.queryParamMap.pipe(skip(1), takeUntil(this.destroy$)).subscribe((params) => {
      this.applyQueryParams(params);
      this.loadClubs();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private applyQueryParams(params: ParamMap): void {
    this.currentPage = this.parsePage(params.get('page'));
  }

  private updateQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: this.currentPage },
      queryParamsHandling: '',
      replaceUrl: true
    });
  }

  private parsePage(rawPage: string | null): number {
    const page = Number(rawPage ?? '1');
    return Number.isInteger(page) && page > 0 ? page : 1;
  }

  loadClubs(): void {
    const userId = this.authState.getUser()?.userId;
    if (!userId) {
      this.clubs = [];
      this.totalPages = 0;
      this.cdr.detectChanges();
      return;
    }

    this.clubService.getClubsByUser(userId, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.clubs = (response?.content ?? []).map((club) => ({
          ...club,
          _hasError: false
        }));

        const backendPage = response?.page;
        if (typeof backendPage === 'number' && backendPage > 0) {
          this.currentPage = backendPage;
        }

        this.totalPages = response?.totalPages ?? 0;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading activity clubs:', err);
        this.clubs = [];
        this.totalPages = 0;
        this.cdr.detectChanges();
      }
    });
  }

  onImageError(club: ClubActivityItem): void {
    club._hasError = true;
    this.cdr.detectChanges();
  }

  getGenreLabel(genre: string): string {
    const labels: Record<string, string> = {
      ROCK: 'Rock',
      POP: 'Pop',
      CACHENGUE: 'Cachengue',
      TECHNO: 'Techno',
      REGGAETON: 'Reggaeton',
      CUMBIA: 'Cumbia',
      TRAP: 'Trap',
      JAZZ: 'Jazz',
      ELECTRONICA: 'Electronica',
      AFROBEAT: 'Afrobeat',
      OTRO: 'Otro'
    };
    return labels[genre] || genre;
  }

  getPriceLabel(price: string): string {
    const labels: Record<string, string> = {
      BAJO: 'Bajo',
      MEDIO: 'Medio',
      ALTO: 'Alto'
    };
    return labels[price] || price;
  }

  hasRating(club: Club): boolean {
    return typeof club.rating === 'number' && club.rating > 0;
  }

  getFullStars(club: Club): number[] {
    const full = Math.floor(club.rating || 0);
    return Array.from({ length: Math.max(0, Math.min(5, full)) }, (_, i) => i);
  }

  hasHalfStar(club: Club): boolean {
    if (!this.hasRating(club)) {
      return false;
    }
    return (club.rating || 0) - Math.floor(club.rating || 0) > 0;
  }

  getEmptyStars(club: Club): number[] {
    const full = Math.floor(club.rating || 0);
    const half = this.hasHalfStar(club) ? 1 : 0;
    const empty = 5 - (full + half);
    return Array.from({ length: Math.max(0, empty) }, (_, i) => i);
  }

  truncate(text: string | undefined, maxLength: number): string {
    if (!text) {
      return '';
    }
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
  }

  goToClubHandler(clubId: string): void {
    this.router.navigate(['/clubs/handle'], {
      queryParams: { clubId }
    });
  }

  prevPage(): void {
    if (this.currentPage <= 1) {
      return;
    }
    this.currentPage -= 1;
    this.updateQueryParams();
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages) {
      return;
    }
    this.currentPage += 1;
    this.updateQueryParams();
  }
}

