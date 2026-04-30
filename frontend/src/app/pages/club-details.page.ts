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
import { CommonModule, isPlatformBrowser, DatePipe, DecimalPipe } from '@angular/common';
import { catchError, combineLatest, forkJoin, map, of } from 'rxjs';
import { FormsModule } from '@angular/forms';
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

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './club-details.page.html'
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
  reserveSuccess?: string;
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
  reviewFormSubmitting = false;
  reviewFormError?: string;
  reviewSuccessMessage?: string;
  reviewDeleting = false;

  private currentClubId?: string;
  private availabilityRequestId = 0;
  private previousBodyOverflow?: string;
  private previousDocumentOverflow?: string;

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
    @Inject(PLATFORM_ID) private platformId: object
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
  }

  ngOnInit() {
    // Estado del usuario logueado para condicionar UI de reviews
    this.currentUser = this.authState.getUser();
    this.authState.isLoggedIn$.subscribe(logged => {
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
    ]).subscribe(([params, queryParams, fragment]) => {
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
        this.loadClubData(id);
      }

      this.loadAvailableBookingDates(id, queryDate);
    });
  }

  private loadClubData(id: string) {
    this.coverUrl = undefined;
    this.carouselPhotos = [];
    this.activeCarouselIndex = 0;
    this.reviewPhotos = [];
    this.activeReviewPhotoIndex = 0;

    this.clubService.getClubById(id).subscribe({
      next: c => {
        this.club = c;
        this.coverUrl = this.resolveCoverUrl(id, c.links?.cover);
        this.cdr.detectChanges();
        queueMicrotask(() => this.tryInitMap());
      },
      error: err => console.error('club error', err)
    });

    this.clubService.getClubPhotos(id).subscribe({
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

    this.clubService.getLayout(id).subscribe({
      next: l => {
        this.layoutUrl = this.resolveLayoutUrl(id, l);
        this.cdr.detectChanges();
      },
      error: () => {
        this.layoutUrl = undefined;
        this.cdr.detectChanges();
      }
    });

    this.clubService.getReviewPhotosByClub(id).subscribe({
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

    this.clubService.getMenu(id).subscribe({
      next: m => {
        this.menu = m;
        this.cdr.detectChanges();
      }
    });

    this.reviewsCurrentPage = 1;
    this.reviewsTotalPages = 1;
    this.loadReviews();

    this.clubService.getEvents(id).subscribe({
      next: e => {
        this.events = e;
        this.cdr.detectChanges();
      },
      error: err => console.warn('events error', err)
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

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        date: nextDate,
        fromEvent: this.fromEvent ? 'true' : null
      },
      queryParamsHandling: 'merge',
      fragment: 'dd-tables'
    }).catch(err => console.error('query update error', err));
  }

  formatBookingDate(date: string): string {
    const parsed = new Date(`${date}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) {
      return date;
    }

    return parsed.toLocaleDateString('es-AR', {
      weekday: 'long',
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
    this.reserveSuccess = undefined;
    this.setReservationModalOpen(false);
    this.reservedBookingId = undefined;
    this.reservedBookingClubId = undefined;
    this.clubService.getTables(id, date).subscribe({
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
        this.tablesError = err?.error?.message || 'No se pudieron cargar las mesas disponibles.';
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

    const nextIndex = ((index % this.carouselPhotos.length) + this.carouselPhotos.length) % this.carouselPhotos.length;
    this.activeCarouselIndex = nextIndex;
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

  private loadAvailableBookingDates(id: string, queryDate?: string, preserveReserveSuccess = false): void {
    const requestId = ++this.availabilityRequestId;
    const candidateDates = this.fromEvent && queryDate
      ? [queryDate]
      : this.buildAllowedBookingDates(15);

    this.loadingBookingDates = true;
    this.tablesError = undefined;
    if (!preserveReserveSuccess) {
      this.reserveSuccess = undefined;
    }
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
      this.clubService.getTables(id, date).pipe(
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
      this.tablesError = 'Seleccioná una mesa y una fecha antes de reservar.';
      this.reserveSuccess = undefined;
      this.cdr.detectChanges();
      return;
    }

    const tableExists = this.tables.some(table => table.tableId === this.selectedTableId);
    if (!tableExists) {
      this.tablesError = 'La mesa seleccionada ya no está disponible. Elegí otra opción.';
      this.reserveSuccess = undefined;
      this.selectedTableId = undefined;
      this.cdr.detectChanges();
      return;
    }

    this.clubService.reserveTable(
      this.currentClubId,
      this.selectedTableId,
      this.selectedDate
    ).subscribe({
      next: booking => {
        this.tablesError = undefined;
        this.reserveSuccess = 'Reserva realizada con exito. Revisa tus reservas en tu cuenta.';
        this.reservedBookingId = booking.id;
        this.reservedBookingClubId = this.currentClubId;
        this.setReservationModalOpen(true);
        this.selectedTableId = undefined;
        this.cdr.detectChanges();
      },
      error: err => {
        this.reserveSuccess = undefined;
        if (err?.status === 401) {
          this.tablesError = 'Tenes que iniciar sesion para reservar una mesa.';
          this.cdr.detectChanges();
          return;
        }
        this.tablesError = err?.error?.message || 'No se pudo completar la reserva. Intentalo de nuevo.';
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
    if (isPlatformBrowser(this.platformId)) {
      window.location.reload();
      return;
    }

    if (this.currentClubId) {
      this.loadAvailableBookingDates(this.currentClubId, this.selectedDate, true);
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

  private loadReviews(): void {
    if (!this.currentClubId) return;

    const clubId = this.currentClubId;
    this.reviewService.listByClub(clubId, this.reviewsCurrentPage - 1, this.reviewsPageSize).subscribe({
      next: ({ reviews, total }) => {
        this.reviews = reviews;
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
      return;
    }

    // Si no esta en la pagina actual, pedimos hasta encontrarla (sin spamear: hasta 5 paginas).
    this.reviewService.listByClub(clubId, 0, 50).subscribe({
      next: ({ reviews }) => {
        this.myReview = reviews.find(r => r.userId === user.userId);
        this.cdr.detectChanges();
      },
      error: () => {
        // mantenemos el valor previo
      }
    });
  }

  isMyReview(review: Review): boolean {
    return !!this.currentUser && review.userId === this.currentUser.userId;
  }

  openCreateReviewForm(): void {
    if (!this.isLoggedIn || this.myReview) return;
    this.reviewFormMode = 'create';
    this.reviewFormRating = 0;
    this.reviewFormDescription = '';
    this.reviewFormError = undefined;
    this.reviewSuccessMessage = undefined;
    this.reviewFormOpen = true;
  }

  openEditReviewForm(review: Review): void {
    if (!this.isMyReview(review)) return;
    this.reviewFormMode = 'edit';
    this.reviewFormRating = review.rating ?? 0;
    this.reviewFormDescription = review.description ?? '';
    this.reviewFormError = undefined;
    this.reviewSuccessMessage = undefined;
    this.reviewFormOpen = true;
  }

  closeReviewForm(): void {
    this.reviewFormOpen = false;
    this.reviewFormError = undefined;
  }

  setReviewFormRating(rating: number): void {
    if (rating < 1 || rating > 5) return;
    this.reviewFormRating = rating;
  }

  submitReviewForm(): void {
    if (!this.currentClubId || this.reviewFormSubmitting) return;

    if (this.reviewFormRating < 1 || this.reviewFormRating > 5) {
      this.reviewFormError = 'Tenes que elegir un puntaje entre 1 y 5.';
      return;
    }

    const payload = {
      rating: this.reviewFormRating,
      description: this.reviewFormDescription?.trim() || undefined,
      fromTableGuest: false
    };

    this.reviewFormSubmitting = true;
    this.reviewFormError = undefined;

    const request$ = this.reviewFormMode === 'create'
      ? this.reviewService.create(this.currentClubId, payload)
      : this.reviewService.update(this.currentClubId, payload);

    request$.subscribe({
      next: () => {
        this.reviewFormSubmitting = false;
        this.reviewSuccessMessage = this.reviewFormMode === 'create'
          ? 'Tu reseña fue publicada.'
          : 'Tu reseña fue actualizada.';
        this.reviewFormOpen = false;
        this.reviewsCurrentPage = 1;
        this.loadReviews();
      },
      error: err => {
        this.reviewFormSubmitting = false;
        if (err?.status === 401) {
          this.reviewFormError = 'Tenes que iniciar sesión para dejar una reseña.';
        } else if (err?.status === 409) {
          this.reviewFormError = 'Ya tenés una reseña para este club. Editala en lugar de crear una nueva.';
        } else {
          this.reviewFormError = err?.error?.message || 'No se pudo guardar la reseña.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  deleteMyReview(): void {
    if (!this.currentClubId || !this.myReview || this.reviewDeleting) return;
    if (isPlatformBrowser(this.platformId) && !window.confirm('¿Seguro que querés borrar tu reseña?')) {
      return;
    }

    this.reviewDeleting = true;
    this.reviewService.delete(this.currentClubId).subscribe({
      next: () => {
        this.reviewDeleting = false;
        this.myReview = undefined;
        this.reviewSuccessMessage = 'Tu reseña fue borrada.';
        this.reviewsCurrentPage = 1;
        this.loadReviews();
      },
      error: err => {
        this.reviewDeleting = false;
        this.reviewFormError = err?.error?.message || 'No se pudo borrar la reseña.';
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
