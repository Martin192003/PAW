import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { catchError, combineLatest, forkJoin, map, of, Subject, takeUntil } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ClubService } from '../services/club.service';
import { ReviewService } from '../services/review.service';
import { AuthStateService } from '../services/auth-state.service';
import { Club } from '../models/club.model';
import { ClubPhoto } from '../models/club-photo.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Review } from '../models/review.model';
import { Event } from '../models/event.model';
import { User } from '../models/user.model';
import { I18nService } from '../services/i18n.service';


@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './club-details.page.html',
  styleUrl: './club-details.page.css'
})
export class ClubDetailsPage implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('clubMap') clubMapRef?: ElementRef<HTMLDivElement>;

  club?: Club;
  coverUrl?: string;
  layoutUrl?: string;
  carouselPhotos: ClubPhoto[] = [];
  activeCarouselIndex = 0;
  reviewPhotos: ClubPhoto[] = [];
  activeReviewPhotoIndex = 0;

  menu: Product[] = [];
  tables: ClubTable[] = [];
  reviews: Review[] = [];
  events: Event[] = [];
  bookingDates: string[] = [];
  loadingBookingDates = false;
  selectedDate = '';
  tablesError?: string;
  reservationModalOpen = false;
  reservedBookingId?: string;
  reservedBookingClubId?: string;
  fromEvent = false;
  selectedTableId?: string;

  reviewsCurrentPage = 1;
  reviewsTotalPages = 1;
  private reviewsPageSize = 3;

  // Estado del usuario actual y de su review (si tiene una)
  currentUser: User | null = null;
  isLoggedIn = false;
  myReview?: Review;

  // Estado del formulario de review (crear / editar)
  reviewFormOpen = false;
  reviewFormMode: 'create' | 'edit' = 'create';
  reviewFormRating = 0;
  reviewFormDescription = '';
  reviewFormPhoto?: File;
  reviewFormPhotoName = '';
  reviewFormSubmitting = false;
  reviewFormError?: string;
  reviewDeleting = false;
  deleteReviewModalOpen = false;

  private currentClubId?: string;
  private clubLinks?: unknown;
  private availabilityRequestId = 0;
  private previousBodyOverflow?: string;
  private previousDocumentOverflow?: string;
  private static readonly maxReviewPhotoBytes = 5 * 1024 * 1024;
  private static readonly allowedReviewPhotoTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
  private static readonly allowedReviewPhotoExtensions = /\.(jpe?g|png|webp)$/i;
  private readonly destroy$ = new Subject<void>();

  private readonly defaultCoords = {
    lat: -34.5891429,
    lng: -58.3961641
  };
  private viewReady = false;
  private mapInstance?: any;
  private mapInteractive = false;

  constructor(
    private route: ActivatedRoute,
    private clubService: ClubService,
    private reviewService: ReviewService,
    private authState: AuthStateService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: object,
    private i18n: I18nService
  ) {}

  goToClubs() {
    this.router.navigate(['/clubs']);
  }

  menuOpen = true;
  tablesOpen = true;

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  toggleTables() {
    this.tablesOpen = !this.tablesOpen;
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.viewReady = true;
      this.tryInitMap();
    }
  }

  ngOnDestroy(): void {
    this.setReservationModalOpen(false);
    if (this.mapInstance) {
      this.mapInstance.remove();
      this.mapInstance = undefined;
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnInit() {
    // Estado del usuario logueado para condicionar UI de reviews
    this.currentUser = this.authState.getUser();
    this.authState.isLoggedIn$.pipe(takeUntil(this.destroy$)).subscribe(logged => {
      this.isLoggedIn = logged;
      this.currentUser = logged ? this.authState.getUser() : null;
      if (!logged) {
        this.myReview = undefined;
        this.closeReviewForm();
      } else if (this.currentClubId) {
        this.refreshMyReview(this.currentClubId);
      }
      this.cdr.detectChanges();
    });

    combineLatest([
      this.route.paramMap,
      this.route.queryParamMap,
      this.route.fragment
    ]).pipe(takeUntil(this.destroy$)).subscribe(([params, queryParams, fragment]) => {
      const id = params.get('id');
      if (!id) return;

      const rawDate = queryParams.get('date');
      const queryDate = this.normalizeDate(rawDate);
      this.fromEvent = queryParams.get('fromEvent') === 'true';

      if (fragment === 'dd-tables') {
        this.tablesOpen = true;
      }

      const changedClub = this.currentClubId !== id;
      this.currentClubId = id;

      if (changedClub) {
        this.loadClubData(id, queryDate);
        return;
      }

      this.loadAvailableBookingDates(id, queryDate, this.clubLinks);
    });
  }

  private loadClubData(id: string, queryDate?: string) {
    this.coverUrl = undefined;
    this.carouselPhotos = [];
    this.activeCarouselIndex = 0;
    this.reviewPhotos = [];
    this.activeReviewPhotoIndex = 0;

    this.clubService.getClubById(id).subscribe({
      next: c => {
        this.club = c;
        this.clubLinks = c.links;
        this.coverUrl = this.resolveCoverUrl(id, c.links?.['cover'] as string | undefined);
        this.cdr.detectChanges();
        queueMicrotask(() => this.tryInitMap());

        this.clubService.getClubPhotos(id, this.clubLinks).subscribe({
          next: photos => {
            this.carouselPhotos = photos
              .map(photo => ({
                ...photo,
                url: this.clubService.toAbsoluteUrl(photo.url, id)
              }))
              .filter(photo => !!photo.url);
            this.activeCarouselIndex = 0;
            this.cdr.detectChanges();
          },
          error: () => {
            this.carouselPhotos = [];
            this.activeCarouselIndex = 0;
            this.cdr.detectChanges();
          }
        });

        this.clubService.getLayout(id, this.clubLinks).subscribe({
          next: l => {
            this.layoutUrl = this.resolveLayoutUrl(id, l);
            this.cdr.detectChanges();
          },
          error: () => {
            this.layoutUrl = undefined;
            this.cdr.detectChanges();
          }
        });

        this.loadReviewPhotos(id);

        this.clubService.getMenu(id, this.clubLinks).subscribe({
          next: m => {
            this.menu = m;
            this.cdr.detectChanges();
          }
        });

        this.reviewsCurrentPage = 1;
        this.reviewsTotalPages = 1;
        this.loadReviews();

        this.clubService.getEvents(id, this.clubLinks).subscribe({
          next: e => {
            this.events = e;
            this.cdr.detectChanges();
          },
          error: err => console.warn('events error', err)
        });

        this.loadAvailableBookingDates(id, queryDate, this.clubLinks);
      },
      error: err => console.error('club error', err)
    });
  }

  onBookingDateChange(date: string): void {
    const nextDate = this.normalizeDate(date);
    if (!nextDate || !this.currentClubId || nextDate === this.selectedDate) {
      return;
    }

    this.selectedDate = nextDate;
    this.selectedTableId = undefined;
    this.tablesOpen = true;
    this.loadTables(this.currentClubId, nextDate);
  }

  formatBookingDate(date: string): string {
    const parsed = new Date(`${date}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) {
      return date;
    }

    const locale = this.i18n.getLanguage() === 'en' ? 'en-US' : 'es-AR';
    return parsed.toLocaleDateString(locale, {
      day: 'numeric',
      month: 'long'
    });
  }

  private loadTables(id: string, date: string): void {
    if (!date) {
      this.tables = [];
      this.selectedTableId = undefined;
      this.cdr.detectChanges();
      return;
    }

    this.tablesError = undefined;
    this.setReservationModalOpen(false);
    this.reservedBookingId = undefined;
    this.reservedBookingClubId = undefined;
    this.clubService.getTables(id, date, this.clubLinks).subscribe({
      next: t => {
        this.tables = t;
        if (!this.tables.some(table => table.tableId === this.selectedTableId)) {
          this.selectedTableId = undefined;
        }
        this.cdr.detectChanges();
      },
      error: err => {
        this.tables = [];
        this.selectedTableId = undefined;
        this.tablesError = err?.error?.message || this.i18n.instant('clubDetails.tablesLoadFailed');
        this.cdr.detectChanges();
      }
    });
  }

  private resolveLayoutUrl(clubId: string, layout?: { url?: string; image?: string } | null): string | undefined {
    const raw = layout?.url || layout?.image;
    if (!raw) {
      return undefined;
    }
    if (/^https?:\/\//i.test(raw)) {
      return raw;
    }
    return this.clubService.toAbsoluteUrl(raw, clubId);
  }

  setActiveCarouselIndex(index: number): void {
    if (!this.carouselPhotos.length) {
      return;
    }

    this.activeCarouselIndex = ((index % this.carouselPhotos.length) + this.carouselPhotos.length) % this.carouselPhotos.length;
    this.cdr.detectChanges();
  }

  nextCarouselPhoto(): void {
    this.setActiveCarouselIndex(this.activeCarouselIndex + 1);
  }

  prevCarouselPhoto(): void {
    this.setActiveCarouselIndex(this.activeCarouselIndex - 1);
  }

  onCarouselPhotoError(index: number): void {
    if (index < 0 || index >= this.carouselPhotos.length) {
      return;
    }

    this.carouselPhotos = this.carouselPhotos.filter((_, i) => i !== index);
    if (!this.carouselPhotos.length) {
      this.activeCarouselIndex = 0;
    } else if (this.activeCarouselIndex >= this.carouselPhotos.length) {
      this.activeCarouselIndex = this.carouselPhotos.length - 1;
    } else if (index <= this.activeCarouselIndex && this.activeCarouselIndex > 0) {
      this.activeCarouselIndex--;
    }

    this.cdr.detectChanges();
  }

  setActiveReviewPhotoIndex(index: number): void {
    if (!this.reviewPhotos.length) {
      return;
    }
    this.activeReviewPhotoIndex = ((index % this.reviewPhotos.length) + this.reviewPhotos.length) % this.reviewPhotos.length;
    this.cdr.detectChanges();
  }

  nextReviewPhoto(): void {
    this.setActiveReviewPhotoIndex(this.activeReviewPhotoIndex + 1);
  }

  prevReviewPhoto(): void {
    this.setActiveReviewPhotoIndex(this.activeReviewPhotoIndex - 1);
  }

  onReviewCarouselPhotoError(index: number): void {
    if (index < 0 || index >= this.reviewPhotos.length) {
      return;
    }

    this.reviewPhotos = this.reviewPhotos.filter((_, i) => i !== index);
    if (!this.reviewPhotos.length) {
      this.activeReviewPhotoIndex = 0;
    } else if (this.activeReviewPhotoIndex >= this.reviewPhotos.length) {
      this.activeReviewPhotoIndex = this.reviewPhotos.length - 1;
    } else if (index <= this.activeReviewPhotoIndex && this.activeReviewPhotoIndex > 0) {
      this.activeReviewPhotoIndex--;
    }

    this.cdr.detectChanges();
  }

  private resolveCoverUrl(clubId: string, coverPath?: string): string {
    if (coverPath) {
      return /^https?:\/\//i.test(coverPath)
        ? coverPath
        : this.clubService.toAbsoluteUrl(coverPath, clubId);
    }

    return this.clubService.getCoverUrl(clubId);
  }

  private normalizeDate(raw: string | null): string | undefined {
    if (!raw) {
      return undefined;
    }
    return /^\d{4}-\d{2}-\d{2}$/.test(raw) ? raw : undefined;
  }

  private buildAllowedBookingDates(days: number): string[] {
    const dates: string[] = [];
    const base = new Date();
    base.setHours(0, 0, 0, 0);

    for (let i = 0; i < days; i++) {
      const d = new Date(base);
      d.setDate(base.getDate() + i);
      const day = d.getDay();
      // 4=Thu, 5=Fri, 6=Sat (mismo criterio que JSP)
      if (day === 4 || day === 5 || day === 6) {
        dates.push(this.toIsoDate(d));
      }
    }

    return dates;
  }

  private loadAvailableBookingDates(id: string, queryDate?: string, links?: unknown): void {
    const requestId = ++this.availabilityRequestId;
    const candidateDates = this.fromEvent && queryDate
      ? [queryDate]
      : this.buildAllowedBookingDates(15);

    this.loadingBookingDates = true;
    this.tablesError = undefined;
    this.bookingDates = [];
    this.selectedDate = '';
    this.tables = [];
    this.selectedTableId = undefined;
    this.cdr.detectChanges();

    if (!candidateDates.length) {
      this.loadingBookingDates = false;
      this.cdr.detectChanges();
      return;
    }

    forkJoin(candidateDates.map(date =>
      this.clubService.getTables(id, date, links).pipe(
        map(tables => ({ date, tables })),
        catchError(() => of({ date, tables: [] as ClubTable[] }))
      )
    )).subscribe(results => {
      if (requestId !== this.availabilityRequestId || id !== this.currentClubId) {
        return;
      }

      const availableByDate = new Map(
        results
          .filter(result => result.tables.length > 0)
          .map(result => [result.date, result.tables] as const)
      );

      this.bookingDates = candidateDates.filter(date => availableByDate.has(date));
      this.selectedDate = queryDate && this.bookingDates.includes(queryDate)
        ? queryDate
        : this.bookingDates[0] ?? '';
      this.tables = this.selectedDate ? (availableByDate.get(this.selectedDate) ?? []) : [];
      this.selectedTableId = undefined;
      this.loadingBookingDates = false;
      this.cdr.detectChanges();
    });
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  onReserveClick(): void {
    if (!this.currentClubId || !this.selectedTableId || !this.selectedDate) {
      this.tablesError = this.i18n.instant('clubDetails.selectTableAndDate');
      this.cdr.detectChanges();
      return;
    }

    const tableExists = this.tables.some(table => table.tableId === this.selectedTableId);
    if (!tableExists) {
      this.tablesError = this.i18n.instant('clubDetails.tableUnavailable');
      this.selectedTableId = undefined;
      this.cdr.detectChanges();
      return;
    }

    this.clubService.reserveTable(
      this.currentClubId,
      this.selectedTableId,
      this.selectedDate,
      this.clubLinks
    ).subscribe({
      next: booking => {
        this.tablesError = undefined;
        this.reservedBookingId = booking.id;
        this.reservedBookingClubId = this.currentClubId;
        this.setReservationModalOpen(true);
        this.selectedTableId = undefined;
        this.cdr.detectChanges();
      },
      error: err => {
        if (err?.status === 401) {
          this.tablesError = this.i18n.instant('clubDetails.reservationRequiresLogin');
          this.cdr.detectChanges();
          return;
        }
        this.tablesError = err?.error?.message || this.i18n.instant('clubDetails.reservationFailed');
        this.cdr.detectChanges();
      }
    });
  }

  goToReservedBooking(): void {
    if (!this.reservedBookingClubId || !this.reservedBookingId) {
      return;
    }

    this.setReservationModalOpen(false);
    this.router.navigate(['/bookings', this.reservedBookingClubId, this.reservedBookingId]);
  }

  refreshAfterReservation(): void {
    this.setReservationModalOpen(false);
    if (this.currentClubId) {
      // Refrescamos solo la disponibilidad de mesas vía el service (sin
      // recargar toda la página) para no perder el estado de la SPA.
      this.loadAvailableBookingDates(this.currentClubId, this.selectedDate, this.clubLinks);
    }
  }

  private setReservationModalOpen(open: boolean): void {
    this.reservationModalOpen = open;

    if (!isPlatformBrowser(this.platformId)) {
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

  previousReviewsPage(): void {
    if (this.reviewsCurrentPage > 1) {
      this.reviewsCurrentPage--;
      this.loadReviews();
    }
  }

  nextReviewsPage(): void {
    if (this.reviewsCurrentPage < this.reviewsTotalPages) {
      this.reviewsCurrentPage++;
      this.loadReviews();
    }
  }

  onReviewPhotoError(event: any): void {
    const img = event.target as HTMLImageElement;
    if (img) {
      img.style.display = 'none';
    }
  }

  get visibleReviews(): Review[] {
    if (this.isEditingMyReview()) {
      return this.reviews.filter(review => !this.isMyReview(review));
    }
    return this.reviews;
  }

  isEditingMyReview(): boolean {
    return this.reviewFormOpen && this.reviewFormMode === 'edit';
  }

  private loadReviews(): void {
    if (!this.currentClubId) return;

    const clubId = this.currentClubId;
    this.reviewService.listByClub(clubId, this.reviewsCurrentPage - 1, this.reviewsPageSize).subscribe({
      next: ({ reviews, total }) => {
        this.reviews = this.prioritizeMyReview(reviews);
        const pages = Math.max(1, Math.ceil(total / this.reviewsPageSize));
        this.reviewsTotalPages = pages;
        this.refreshMyReview(clubId);
        this.cdr.detectChanges();
      },
      error: err => {
        console.warn('reviews error', err);
        this.reviews = [];
        this.reviewsTotalPages = 1;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Refresca el rating/quantityRatings del club tras una mutación de review.
   * El backend recalcula el promedio en updateClubRating, así que basta con
   * pedir el club fresco.
   */
  private refreshClubRating(): void {
    if (!this.currentClubId) return;
    const id = this.currentClubId;

    this.clubService.getClubById(id).subscribe({
      next: c => {
        if (this.club && id === this.currentClubId) {
          // Actualizo solo los campos relevantes para no romper otras refs
          this.club = {
            ...this.club,
            rating: c.rating,
            quantityRatings: c.quantityRatings
          };
          this.cdr.detectChanges();
        }
      },
      error: err => console.warn('club rating refresh error', err)
    });
  }

  /**
   * Detecta si el usuario logueado ya tiene una review en este club.
   * Como el endpoint REST de reviews devuelve userId, comparamos por id.
   * Si no encontramos en la pagina actual, buscamos en todas las paginas.
   */
  private refreshMyReview(clubId: string): void {
    const user = this.currentUser;
    if (!user) {
      this.myReview = undefined;
      return;
    }

    // Primero buscamos en la pagina cargada
    const inCurrentPage = this.reviews.find(r => r.userId === user.userId);
    if (inCurrentPage) {
      this.myReview = inCurrentPage;
      this.reviews = this.prioritizeMyReview(this.reviews);
      return;
    }

    this.findMyReviewInClub(clubId, user.userId);
  }

  private loadReviewPhotos(clubId: string): void {
    this.clubService.getReviewPhotosByClub(clubId, this.clubLinks).subscribe({
      next: photos => {
        this.reviewPhotos = photos;
        this.activeReviewPhotoIndex = 0;
        this.cdr.detectChanges();
      },
      error: () => {
        this.reviewPhotos = [];
        this.activeReviewPhotoIndex = 0;
        this.cdr.detectChanges();
      }
    });
  }

  private findMyReviewInClub(clubId: string, userId: string, page = 0, pageSize = 50): void {
    this.reviewService.listByClub(clubId, page, pageSize).subscribe({
      next: ({ reviews, total }) => {
        if (clubId !== this.currentClubId || this.currentUser?.userId !== userId) {
          return;
        }

        const found = reviews.find(r => r.userId === userId);
        if (found) {
          this.myReview = found;
          this.reviews = this.prioritizeMyReview([found, ...this.reviews]);
          this.cdr.detectChanges();
          return;
        }

        if ((page + 1) * pageSize < total) {
          this.findMyReviewInClub(clubId, userId, page + 1, pageSize);
          return;
        }

        this.myReview = undefined;
        this.cdr.detectChanges();
      },
      error: () => {
        // mantenemos el valor previo
      }
    });
  }

  private prioritizeMyReview(reviews: Review[]): Review[] {
    const userId = this.currentUser?.userId;
    if (!userId) {
      return reviews;
    }

    const ownReview = reviews.find(review => review.userId === userId);
    if (!ownReview) {
      return reviews;
    }

    return [
      ownReview,
      ...reviews.filter(review => review.userId !== userId)
    ];
  }

  isMyReview(review: Review): boolean {
    return !!this.currentUser && review.userId === this.currentUser.userId;
  }

  openCreateReviewForm(): void {
    if (!this.isLoggedIn || this.myReview) return;
    this.reviewFormMode = 'create';
    this.reviewFormRating = 0;
    this.reviewFormDescription = '';
    this.clearReviewPhoto();
    this.reviewFormError = undefined;
    this.reviewFormOpen = true;
  }

  openEditReviewForm(review: Review): void {
    if (!this.isMyReview(review)) return;
    this.reviewFormMode = 'edit';
    this.reviewFormRating = review.rating ?? 0;
    this.reviewFormDescription = review.description ?? '';
    this.clearReviewPhoto();
    this.reviewFormError = undefined;
    this.reviewFormOpen = true;
  }

  closeReviewForm(): void {
    this.reviewFormOpen = false;
    this.reviewFormError = undefined;
    this.clearReviewPhoto();
  }

  openDeleteReviewModal(): void {
    if (!this.currentClubId || !this.myReview || this.reviewDeleting) {
      return;
    }
    this.deleteReviewModalOpen = true;
  }

  closeDeleteReviewModal(): void {
    if (this.reviewDeleting) {
      return;
    }
    this.deleteReviewModalOpen = false;
  }

  setReviewFormRating(rating: number): void {
    if (rating < 1 || rating > 5) return;
    this.reviewFormRating = rating;
  }

  onReviewPhotoSelected(event: globalThis.Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.reviewFormError = undefined;
    this.clearReviewPhoto();

    if (!file) {
      return;
    }

    const validationError = this.validateReviewPhoto(file);
    if (validationError) {
      this.reviewFormError = validationError;
      input.value = '';
      this.cdr.detectChanges();
      return;
    }

    this.reviewFormPhoto = file;
    this.reviewFormPhotoName = file.name;
    this.cdr.detectChanges();
  }

  clearReviewPhoto(): void {
    this.reviewFormPhoto = undefined;
    this.reviewFormPhotoName = '';
  }

  private validateReviewPhoto(file: File): string | null {
    if (file.size <= 0) {
      return this.i18n.instant('clubDetails.reviewMessages.invalidPhoto');
    }

    if (file.size > ClubDetailsPage.maxReviewPhotoBytes) {
      return this.i18n.instant('clubDetails.reviewMessages.photoTooLarge');
    }

    const type = (file.type || '').trim().toLowerCase();
    if (ClubDetailsPage.allowedReviewPhotoTypes.has(type)) {
      return null;
    }

    if ((!type || type === 'application/octet-stream') && ClubDetailsPage.allowedReviewPhotoExtensions.test(file.name || '')) {
      return null;
    }

    return this.i18n.instant('clubDetails.reviewMessages.invalidPhoto');
  }

  submitReviewForm(): void {
    if (!this.currentClubId || this.reviewFormSubmitting) return;

    if (this.reviewFormRating < 1 || this.reviewFormRating > 5) {
      this.reviewFormError = this.i18n.instant('clubDetails.reviewMessages.invalidRating');
      return;
    }

    const payload = {
      rating: this.reviewFormRating,
      description: this.reviewFormDescription?.trim() || undefined,
      fromTableGuest: false,
      photo: this.reviewFormPhoto
    };

    this.reviewFormSubmitting = true;
    this.reviewFormError = undefined;

    const request$ = this.reviewFormMode === 'create'
      ? this.reviewService.create(this.currentClubId, payload)
      : this.myReview
        ? this.reviewService.update(this.currentClubId, this.myReview.reviewId, payload)
        : this.reviewService.create(this.currentClubId, payload);

    request$.subscribe({
      next: () => {
        this.reviewFormSubmitting = false;
        this.reviewFormOpen = false;
        this.clearReviewPhoto();
        this.reviewsCurrentPage = 1;
        this.loadReviews();
        this.loadReviewPhotos(this.currentClubId!);
        this.refreshClubRating();
      },
      error: err => {
        this.reviewFormSubmitting = false;
        if (err?.status === 401) {
          this.reviewFormError = this.i18n.instant('clubDetails.reviewMessages.loginRequired');
        } else if (err?.status === 409) {
          this.reviewFormError = this.i18n.instant('clubDetails.reviewMessages.alreadyExists');
        } else {
          this.reviewFormError = err?.error?.message || this.i18n.instant('clubDetails.reviewMessages.saveFailed');
        }
        this.cdr.detectChanges();
      }
    });
  }

  deleteMyReview(): void {
    if (!this.currentClubId || !this.myReview || this.reviewDeleting) return;

    this.reviewDeleting = true;
    this.reviewService.delete(this.currentClubId, this.myReview.reviewId).subscribe({
      next: () => {
        this.reviewDeleting = false;
        this.deleteReviewModalOpen = false;
        this.myReview = undefined;
        this.reviewsCurrentPage = 1;
        this.loadReviews();
        this.loadReviewPhotos(this.currentClubId!);
        this.refreshClubRating();
      },
      error: err => {
        this.reviewDeleting = false;
        this.reviewFormError = err?.error?.message || this.i18n.instant('clubDetails.reviewMessages.deleteFailed');
        this.cdr.detectChanges();
      }
    });
  }



  private async tryInitMap(): Promise<void> {
    if (!isPlatformBrowser(this.platformId) || !this.viewReady || !this.club || this.mapInstance) {
      return;
    }

    const mapContainer = this.clubMapRef?.nativeElement;
    if (!mapContainer) {
      return;
    }

    try {
      const L = await this.loadLeaflet();
      const { lat, lng } = await this.geocodeAddress(this.club.location || '');

      this.mapInstance = L.map(mapContainer, {
        center: [lat, lng],
        zoom: 16,
        zoomControl: false,
        dragging: false,
        scrollWheelZoom: false,
        doubleClickZoom: false,
        boxZoom: false,
        keyboard: false
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(this.mapInstance);

      L.marker([lat, lng])
        .addTo(this.mapInstance)
        .bindPopup(`<b>${this.escapeHtml(this.club.clubName)}</b><br>${this.escapeHtml(this.club.location)}`)
        .openPopup();

      mapContainer.style.cursor = 'pointer';
      mapContainer.addEventListener('click', () => this.enableMapInteraction(L, mapContainer), { once: true });

      // Leaflet needs a reflow when the container was recently created by Angular.
      setTimeout(() => this.mapInstance?.invalidateSize(), 0);
    } catch (error) {
      console.error('map init error', error);
    }
  }

  private enableMapInteraction(L: any, mapContainer: HTMLDivElement): void {
    if (!this.mapInstance || this.mapInteractive) {
      return;
    }

    this.mapInstance.dragging.enable();
    this.mapInstance.scrollWheelZoom.enable();
    this.mapInstance.doubleClickZoom.enable();
    this.mapInstance.boxZoom.enable();
    this.mapInstance.keyboard.enable();
    L.control.zoom({ position: 'topright' }).addTo(this.mapInstance);
    mapContainer.style.cursor = 'grab';
    this.mapInteractive = true;
  }

  private async geocodeAddress(address: string): Promise<{ lat: number; lng: number }> {
    if (!address) {
      return this.defaultCoords;
    }

    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(address)}`,
        {
          headers: {
            Accept: 'application/json'
          }
        }
      );
      const results = await response.json();
      if (Array.isArray(results) && results.length > 0) {
        return {
          lat: Number(results[0].lat),
          lng: Number(results[0].lon)
        };
      }
    } catch (error) {
      console.error('geocode error', error);
    }

    return this.defaultCoords;
  }

  private async loadLeaflet(): Promise<any> {
    const windowWithLeaflet = window as any;
    if (windowWithLeaflet.L) {
      return windowWithLeaflet.L;
    }

    this.ensureStylesheet(
      'leaflet-css',
      'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
      'sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY='
    );

    await this.ensureScript(
      'leaflet-js',
      'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js',
      'sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo='
    );

    return windowWithLeaflet.L;
  }

  private ensureStylesheet(id: string, href: string, integrity: string): void {
    if (document.getElementById(id)) {
      return;
    }

    const link = document.createElement('link');
    link.id = id;
    link.rel = 'stylesheet';
    link.href = href;
    link.integrity = integrity;
    link.crossOrigin = 'anonymous';
    document.head.appendChild(link);
  }

  private ensureScript(id: string, src: string, integrity: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const existing = document.getElementById(id) as HTMLScriptElement | null;
      if (existing?.dataset['loaded'] === 'true') {
        resolve();
        return;
      }

      const script = existing || document.createElement('script');
      script.id = id;
      script.src = src;
      script.async = true;
      script.integrity = integrity;
      script.crossOrigin = 'anonymous';

      script.onload = () => {
        script.dataset['loaded'] = 'true';
        resolve();
      };
      script.onerror = () => reject(new Error('Leaflet script failed to load'));

      if (!existing) {
        document.body.appendChild(script);
      }
    });
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }
}
