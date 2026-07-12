import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { skip, takeUntil } from 'rxjs/operators';

import { ReviewService } from '../services/review.service';
import { ClubService } from '../services/club.service';
import { AuthStateService } from '../services/auth-state.service';
import { Review } from '../models/review.model';
import { Club } from '../models/club.model';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { I18nService } from '../services/i18n.service';

type ReviewItem = Review & {
  clubName?: string;
  clubLocation?: string;
};

@Component({
  standalone: true,
  selector: 'app-my-reviews-page',
  imports: [CommonModule, SideBarComponent, TranslateModule],
  templateUrl: './my-reviews.page.html'
})
export class MyReviewsPage implements OnInit, OnDestroy {

  reviews: ReviewItem[] = [];
  loading = false;
  errorMessage = '';
  deletingReviewId: string | null = null;

  currentPage = 1;
  pageSize = 5;
  totalPages = 1;

  private clubCache = new Map<string, Club>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly reviewService: ReviewService,
    private readonly clubService: ClubService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.applyQueryParams(this.route.snapshot.queryParamMap);
    this.loadReviews();

    this.route.queryParamMap.pipe(skip(1), takeUntil(this.destroy$)).subscribe((params) => {
      this.applyQueryParams(params);
      this.loadReviews();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goBack(): void {
    this.router.navigate(['/profile']);
  }

  goToClub(clubId?: string): void {
    if (!clubId) return;
    this.router.navigate(['/clubs', clubId]);
  }

  prevPage(): void {
    if (this.currentPage <= 1) return;
    this.currentPage -= 1;
    this.updateQueryParams();
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages) return;
    this.currentPage += 1;
    this.updateQueryParams();
  }

  private applyQueryParams(params: ParamMap): void {
    this.currentPage = this.parsePage(params.get('page'));
  }

  private updateQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: this.currentPage },
      queryParamsHandling: ''
    });
  }

  private parsePage(rawPage: string | null): number {
    const page = Number(rawPage ?? '1');
    return Number.isInteger(page) && page > 0 ? page : 1;
  }

  loadReviews(): void {
    const userId = this.authState.getUser()?.userId;
    if (!userId) {
      this.errorMessage = this.i18n.instant('myReviews.errors.loginRequired');
      this.reviews = [];
      this.totalPages = 1;
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.reviewService.listByUser(userId, this.currentPage - 1, this.pageSize).subscribe({
      next: ({ reviews, total }) => {
        this.reviews = reviews;
        this.totalPages = Math.max(1, Math.ceil(total / this.pageSize));
        this.loading = false;
        this.enrichWithClubs();
        this.cdr.detectChanges();
      },
      error: err => {
        console.warn('my-reviews load error', err);
        this.reviews = [];
        this.totalPages = 1;
        this.errorMessage = err?.error?.message || this.i18n.instant('myReviews.errors.loadFailed');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  deleteReview(review: ReviewItem): void {
    if (!review.clubId || this.deletingReviewId) return;
    if (!window.confirm(this.i18n.instant('myReviews.confirmDelete'))) return;

    this.deletingReviewId = review.reviewId;
    this.reviewService.delete(review.clubId).subscribe({
      next: () => {
        this.deletingReviewId = null;
        this.reviews = this.reviews.filter(r => r.reviewId !== review.reviewId);
        // Si la página queda vacía y no es la primera, retrocedo (actualizando la URL)
        if (this.reviews.length === 0 && this.currentPage > 1) {
          this.currentPage -= 1;
          this.updateQueryParams();
          return;
        }
        this.loadReviews();
      },
      error: err => {
        this.deletingReviewId = null;
        this.errorMessage = err?.error?.message || this.i18n.instant('myReviews.errors.deleteFailed');
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Pide los nombres de club de cada review en paralelo (con cache).
   * El DTO de review trae solo clubId.
   */
  private enrichWithClubs(): void {
    const ids = Array.from(new Set(
      this.reviews.map(r => r.clubId).filter((id): id is string => !!id)
    ));

    ids.forEach(id => {
      const cached = this.clubCache.get(id);
      if (cached) {
        this.applyClubInfo(id, cached);
        return;
      }

      this.clubService.getClubById(id).subscribe({
        next: club => {
          this.clubCache.set(id, club);
          this.applyClubInfo(id, club);
        },
        error: () => {
          // si falla, queda como "Club desconocido"
        }
      });
    });
  }

  private applyClubInfo(clubId: string, club: Club): void {
    this.reviews = this.reviews.map(r =>
      r.clubId === clubId
        ? { ...r, clubName: club.clubName, clubLocation: club.location }
        : r
    );
    this.cdr.detectChanges();
  }

  starsArray(rating: number | undefined): number[] {
    const r = Math.max(0, Math.min(5, Math.round(rating ?? 0)));
    return Array.from({ length: 5 }, (_, i) => (i < r ? 1 : 0));
  }
}
