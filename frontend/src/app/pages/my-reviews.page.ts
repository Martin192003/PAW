import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';

import { ReviewService } from '../services/review.service';
import { ClubService } from '../services/club.service';
import { AuthStateService } from '../services/auth-state.service';
import { Review } from '../models/review.model';
import { Club } from '../models/club.model';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';

type ReviewItem = Review & {
  clubName?: string;
  clubLocation?: string;
};

@Component({
  standalone: true,
  selector: 'app-my-reviews-page',
  imports: [CommonModule, DatePipe, SideBarComponent],
  templateUrl: './my-reviews.page.html'
})
export class MyReviewsPage implements OnInit {

  reviews: ReviewItem[] = [];
  loading = false;
  errorMessage = '';
  deletingReviewId: string | null = null;

  currentPage = 1;
  pageSize = 5;
  totalPages = 1;

  private clubCache = new Map<string, Club>();

  constructor(
    private readonly reviewService: ReviewService,
    private readonly clubService: ClubService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadReviews();
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
    this.loadReviews();
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages) return;
    this.currentPage += 1;
    this.loadReviews();
  }

  loadReviews(): void {
    const userId = this.authState.getUser()?.userId;
    if (!userId) {
      this.errorMessage = 'Tenés que iniciar sesión para ver tus reseñas.';
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
        this.errorMessage = err?.error?.message || 'No se pudieron cargar tus reseñas.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  deleteReview(review: ReviewItem): void {
    if (!review.clubId || this.deletingReviewId) return;
    if (!window.confirm('¿Seguro que querés borrar esta reseña?')) return;

    this.deletingReviewId = review.reviewId;
    this.reviewService.delete(review.clubId).subscribe({
      next: () => {
        this.deletingReviewId = null;
        this.reviews = this.reviews.filter(r => r.reviewId !== review.reviewId);
        // Si la página queda vacía y no es la primera, retrocedo
        if (this.reviews.length === 0 && this.currentPage > 1) {
          this.currentPage -= 1;
        }
        this.loadReviews();
      },
      error: err => {
        this.deletingReviewId = null;
        this.errorMessage = err?.error?.message || 'No se pudo borrar la reseña.';
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
