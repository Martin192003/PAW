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
import { combineLatest } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { ClubPhoto } from '../models/club-photo.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Review } from '../models/review.model';
import { Event } from '../models/event.model';

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
  selectedDate = '';
  tablesError?: string;
  reserveSuccess?: string;
  fromEvent = false;
  selectedTableId?: string;

  reviewsCurrentPage = 1;
  reviewsTotalPages = 1;
  private reviewsPageSize = 3;

  private currentClubId?: string;

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
    if (this.mapInstance) {
      this.mapInstance.remove();
      this.mapInstance = undefined;
    }
  }

  ngOnInit() {
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

      this.bookingDates = this.fromEvent && queryDate
        ? [queryDate]
        : this.buildAllowedBookingDates(15);

      if (this.fromEvent) {
        this.selectedDate = queryDate ?? this.bookingDates[0] ?? '';
      } else {
        const fallback = this.bookingDates[0] ?? '';
        this.selectedDate = (queryDate && this.bookingDates.includes(queryDate)) ? queryDate : fallback;
      }

      if (fragment === 'dd-tables') {
        this.tablesOpen = true;
      }

      const changedClub = this.currentClubId !== id;
      this.currentClubId = id;

      if (changedClub) {
        this.loadClubData(id);
      }

      this.loadTables(id, this.selectedDate);
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
    this.tablesError = undefined;
    this.reserveSuccess = undefined;
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
      this.selectedDate,
      this.fromEvent
    ).subscribe({
      next: () => {
        this.tablesError = undefined;
        this.reserveSuccess = 'Reserva realizada con exito. Revisa tus reservas en tu cuenta.';
        this.selectedTableId = undefined;
        this.loadTables(this.currentClubId!, this.selectedDate);
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

    this.clubService.getReviewsPaginated(this.currentClubId, this.reviewsCurrentPage - 1, this.reviewsPageSize).subscribe({
      next: (response: any) => {
        this.reviews = Array.isArray(response) ? response : (response?.content ?? []);
        this.reviewsTotalPages = response?.totalPages ?? 1;
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
