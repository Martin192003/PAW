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
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Review } from '../models/review.model';
import { Event } from '../models/event.model';

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './club-details.page.html'
})
export class ClubDetailsPage implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('clubMap') clubMapRef?: ElementRef<HTMLDivElement>;

  club?: Club;
  coverUrl?: string;
  layoutUrl?: string;

  menu: Product[] = [];
  tables: ClubTable[] = [];
  reviews: Review[] = [];
  events: Event[] = [];

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

  menuOpen = false;
  tablesOpen = false;

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
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (!id) return;

      this.loadClubData(id);
    });
  }

  private loadClubData(id: string) {
    this.clubService.getClubById(id).subscribe({
      next: c => {
        this.club = c;
        this.cdr.detectChanges();
        queueMicrotask(() => this.tryInitMap());
      },
      error: err => console.error('club error', err)
    });

    this.clubService.getLayout(id).subscribe({
      next: l => {
        if (l?.url) {
          this.layoutUrl = 'http://localhost:8080/webapp_war_exploded' + l.url;
        } else {
          this.layoutUrl = undefined;
        }
        this.cdr.detectChanges();
      }
    });

    this.clubService.getMenu(id).subscribe({
      next: m => {
        this.menu = m;
        this.cdr.detectChanges();
      }
    });

    this.clubService.getTables(id).subscribe({
      next: t => {
        this.tables = t;
        this.cdr.detectChanges();
      }
    });

        /*
    this.clubService.getReviews(id).subscribe({
      next: r => this.reviews = r,
      error: err => console.warn('reviews no existe todavía', err)
    });

    this.clubService.getCover(id).subscribe({
      next: c => this.coverUrl = c.url,
      error: err => console.error('cover error', err)
    });

    this.clubService.getEvents(id).subscribe({
      next: e => this.events = e,
      error: err => console.warn('events no existe todavía', err)
    });
    */
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
