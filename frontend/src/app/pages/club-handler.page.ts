import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import {
  ClubService,
  ClubUpdatePayload,
  EventPayload,
  ProductPayload,
  TablePayload
} from '../services/club.service';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { Club } from '../models/club.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Event } from '../models/event.model';
import { Review } from '../models/review.model';
import { Layout } from '../models/layout.model';
import { ClubPhoto } from '../models/club-photo.model';
import { finalize, timeout } from 'rxjs';

type ClubHandlerTab = 'menu' | 'tables' | 'events' | 'reviews';
type EventFilter = 'all' | 'past' | 'future';
type TableForm = {
  tableNumber: number | null;
  capacity: number | null;
  price: number | null;
  discount: number | null;
  tableState: string;
};
type ProductForm = Omit<ProductPayload, 'basePrice'> & {
  basePrice: number | null;
};

@Component({
  standalone: true,
  selector: 'app-club-handler-page',
  imports: [CommonModule, DatePipe, DecimalPipe, FormsModule, SideBarComponent],
  templateUrl: './club-handler.page.html',
  styleUrl: './club-handler.page.css'
})
export class ClubHandlerPage implements OnInit {
  private readonly mutationTimeoutMs = 12000;

  clubId = '';

  club: Club | null = null;
  coverUrl = '';
  layoutUrl = '';
  galleryPhotos: ClubPhoto[] = [];

  menu: Product[] = [];
  tables: ClubTable[] = [];
  events: Event[] = [];
  reviews: Review[] = [];

  loading = true;
  clubLoadAttempted = false;
  errorMessage = '';
  actionErrorMessage = '';
  actionSuccessMessage = '';

  activeTab: ClubHandlerTab = 'menu';

  eventFilter: EventFilter = 'all';
  eventPage = 1;
  readonly eventPageSize = 5;

  reviewPage = 1;
  readonly reviewPageSize = 5;
  reviewTotalPages = 0;

  readonly productTypes = ['ALCOHOLICA', 'NO_ALCOHOLICA', 'TRAGOS'];
  readonly tableStates = ['DISPONIBLE', 'RESERVADA', 'PENDIENTE'];
  readonly priceRanges = ['BAJO', 'MEDIO', 'ALTO'];
  readonly musicGenres = [
    'ROCK',
    'POP',
    'CACHENGUE',
    'TECHNO',
    'REGGAETON',
    'CUMBIA',
    'TRAP',
    'JAZZ',
    'ELECTRONICA',
    'AFROBEAT',
    'OTRO'
  ];
  readonly zones = ['ZONA_NORTE', 'ZONA_OESTE', 'ZONA_SUR', 'ZONA_CABA'];

  editingClub = false;
  savingClub = false;
  clubForm: ClubUpdatePayload = {
    clubName: '',
    location: '',
    priceRange: 'MEDIO',
    description: '',
    musicGenre: 'TECHNO',
    zone: 'ZONA_CABA',
    isBar: false,
    cbu: ''
  };

  savingProduct = false;
  editingProductId: string | null = null;
  productForm: ProductForm = {
    productName: '',
    description: '',
    basePrice: null,
    productType: 'ALCOHOLICA',
    isAvailable: true
  };

  savingTable = false;
  editingTableId: string | null = null;
  tableForm: TableForm = {
    tableNumber: null,
    capacity: null,
    price: null,
    discount: null,
    tableState: 'DISPONIBLE'
  };

  savingEvent = false;
  editingEventId: string | null = null;
  eventForm: EventPayload = {
    eventName: '',
    description: '',
    date: ''
  };

  uploadingEventPhotoId: string | null = null;
  uploadingCover = false;
  uploadingLayout = false;
  uploadingGallery = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly clubService: ClubService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const clubId = params.get('clubId');
      if (!clubId) {
        this.errorMessage = 'No se encontro el club seleccionado.';
        this.loading = false;
        return;
      }

      this.clubId = clubId;
      this.eventPage = 1;
      this.reviewPage = 1;
      this.loadClubHandlerData();
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/activity']);
  }

  retryLoad(): void {
    if (!this.clubId) {
      return;
    }
    this.loadClubHandlerData();
  }

  dismissErrorModal(): void {
    this.errorMessage = '';
    this.actionErrorMessage = '';
  }

  onCoverSelected(uploadEvent: globalThis.Event): void {
    const input = uploadEvent.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.clubId || this.uploadingCover) {
      return;
    }

    this.uploadingCover = true;
    this.clubService.uploadCover(this.clubId, file).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.uploadingCover = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.coverUrl = `${this.clubService.getCoverUrl(this.clubId)}?t=${Date.now()}`;
        this.loadGalleryPhotos();
        this.actionSuccessMessage = 'Portada actualizada.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = 'No se pudo actualizar la portada.';
        this.actionSuccessMessage = '';
      }
    });

    input.value = '';
  }

  onLayoutSelected(uploadEvent: globalThis.Event): void {
    const input = uploadEvent.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.clubId || this.uploadingLayout) {
      return;
    }

    this.uploadingLayout = true;
    this.clubService.uploadLayout(this.clubId, file).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.uploadingLayout = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.layoutUrl = this.withCacheBust(
          this.clubService.toAbsoluteUrl(`/clubs/${this.clubId}/layout/image`, this.clubId)
        );
        this.actionSuccessMessage = 'Layout actualizado.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadLayout();
        this.actionErrorMessage = 'No se pudo actualizar el layout.';
        this.actionSuccessMessage = '';
      }
    });

    input.value = '';
  }

  onGallerySelected(uploadEvent: globalThis.Event): void {
    const input = uploadEvent.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (!files.length || !this.clubId || this.uploadingGallery) {
      return;
    }

    const maxBatch = 5;
    const selected = files.slice(0, maxBatch);
    this.uploadingGallery = true;

    this.clubService.uploadGalleryPhotos(this.clubId, selected).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.uploadingGallery = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.loadGalleryPhotos();
        this.actionSuccessMessage = 'Fotos de galeria actualizadas.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadGalleryPhotos();
        this.actionErrorMessage = 'No se pudieron subir las fotos de galeria.';
        this.actionSuccessMessage = '';
      }
    });

    input.value = '';
  }

  deleteGalleryPhoto(photo: ClubPhoto): void {
    if (!this.clubId || !photo.photoId) {
      return;
    }

    if (!window.confirm('Eliminar esta foto de la galeria?')) {
      return;
    }

    this.clubService.deleteGalleryPhoto(this.clubId, photo.photoId).pipe(
      timeout(this.mutationTimeoutMs)
    ).subscribe({
      next: () => {
        this.galleryPhotos = this.galleryPhotos.filter((item) => item.photoId !== photo.photoId);
        this.actionSuccessMessage = 'Foto eliminada de la galeria.';
        this.actionErrorMessage = '';
        this.cdr.detectChanges();
        this.loadGalleryPhotos();
      },
      error: () => {
        this.actionErrorMessage = 'No se pudo eliminar la foto de galeria.';
        this.actionSuccessMessage = '';
        this.loadGalleryPhotos();
      }
    });
  }

  setTab(tab: ClubHandlerTab): void {
    this.activeTab = tab;
    this.clearActionMessages();
  }

  setEventFilter(filter: EventFilter): void {
    this.eventFilter = filter;
    this.eventPage = 1;
  }

  get filteredEvents(): Event[] {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (this.eventFilter === 'all') {
      return this.events;
    }

    return this.events.filter((event) => {
      const eventDate = new Date(`${event.date}T00:00:00`);
      if (Number.isNaN(eventDate.getTime())) {
        return false;
      }

      if (this.eventFilter === 'past') {
        return eventDate < today;
      }

      return eventDate >= today;
    });
  }

  get eventTotalPages(): number {
    const total = Math.ceil(this.filteredEvents.length / this.eventPageSize);
    return Math.max(total, 1);
  }

  get pagedEvents(): Event[] {
    const start = (this.eventPage - 1) * this.eventPageSize;
    return this.filteredEvents.slice(start, start + this.eventPageSize);
  }

  prevEventPage(): void {
    if (this.eventPage <= 1) {
      return;
    }
    this.eventPage -= 1;
  }

  nextEventPage(): void {
    if (this.eventPage >= this.eventTotalPages) {
      return;
    }
    this.eventPage += 1;
  }

  prevReviewPage(): void {
    if (this.reviewPage <= 1) {
      return;
    }
    this.reviewPage -= 1;
    this.loadReviews();
  }

  nextReviewPage(): void {
    if (this.reviewPage >= this.reviewTotalPages) {
      return;
    }
    this.reviewPage += 1;
    this.loadReviews();
  }

  getReviewUsername(review: Review): string {
    return review.user?.username || review.username || 'Usuario';
  }

  startEditClub(): void {
    if (!this.club) {
      return;
    }

    this.clubForm = {
      clubName: this.club.clubName,
      location: this.club.location,
      priceRange: this.club.priceRange,
      description: this.club.description,
      musicGenre: this.club.musicGenre,
      zone: this.club.zone,
      isBar: this.club.bar,
      cbu: this.club.cbu
    };
    this.editingClub = true;
    this.clearActionMessages();
  }

  cancelEditClub(): void {
    this.editingClub = false;
  }

  saveClub(): void {
    if (!this.clubId || this.savingClub) {
      return;
    }

    const payload = {
      ...this.clubForm,
      clubName: this.clubForm.clubName.trim(),
      location: this.clubForm.location.trim(),
      description: this.clubForm.description.trim(),
      cbu: this.clubForm.cbu.trim()
    };

    if (!payload.clubName || !payload.location || !payload.description || !payload.cbu) {
      this.actionErrorMessage = 'Completa los campos requeridos del club.';
      this.actionSuccessMessage = '';
      return;
    }

    this.savingClub = true;
    this.clubService.updateClub(this.clubId, payload).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.savingClub = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (updatedClub) => {
        this.club = updatedClub;
        this.editingClub = false;
        this.actionSuccessMessage = 'Datos del club actualizados.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadClub();
        this.actionErrorMessage = 'No se pudo confirmar la actualizacion del club. Se recargaron los datos.';
        this.actionSuccessMessage = '';
      }
    });
  }

  startCreateProduct(): void {
    this.editingProductId = null;
    this.productForm = {
      productName: '',
      description: '',
      basePrice: null,
      productType: this.productTypes[0],
      isAvailable: true
    };
  }

  startEditProduct(product: Product): void {
    this.editingProductId = product.productId;
    this.productForm = {
      productName: product.productName,
      description: product.description,
      basePrice: product.basePrice,
      productType: this.normalizeProductType(product.productType),
      isAvailable: product.isAvailable
    };
    this.clearActionMessages();
  }

  cancelEditProduct(): void {
    this.startCreateProduct();
  }

  saveProduct(): void {
    if (!this.clubId || this.savingProduct) {
      return;
    }

    if (this.productForm.basePrice == null) {
      this.actionErrorMessage = 'Completa el precio del producto.';
      this.actionSuccessMessage = '';
      return;
    }

    const payload: ProductPayload = {
      ...this.productForm,
      productName: this.productForm.productName.trim(),
      description: this.productForm.description.trim(),
      basePrice: Number(this.productForm.basePrice)
    };

    if (!payload.productName || !payload.description || payload.basePrice <= 0) {
      this.actionErrorMessage = 'Completa los campos del producto con valores validos.';
      this.actionSuccessMessage = '';
      return;
    }

    this.savingProduct = true;
    const request$ = this.editingProductId
      ? this.clubService.updateProduct(this.clubId, this.editingProductId, payload)
      : this.clubService.createProduct(this.clubId, payload);

    request$.pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.savingProduct = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (savedProduct) => {
        this.upsertProductInList(savedProduct);
        this.startCreateProduct();
        this.actionSuccessMessage = 'Producto guardado correctamente.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadMenu();
        this.actionErrorMessage = 'No se pudo confirmar el guardado del producto. Se recargo el menu.';
        this.actionSuccessMessage = '';
      }
    });
  }

  toggleProductAvailability(product: Product): void {
    if (!this.clubId) {
      return;
    }

    const payload: ProductPayload = {
      productName: product.productName,
      description: product.description,
      basePrice: product.basePrice,
      productType: this.normalizeProductType(product.productType),
      isAvailable: !product.isAvailable
    };

    this.clubService.updateProduct(this.clubId, product.productId, payload).subscribe({
      next: (updatedProduct) => {
        this.upsertProductInList({
          ...product,
          ...updatedProduct,
          isAvailable: payload.isAvailable,
          productType: payload.productType
        });
        this.actionSuccessMessage = payload.isAvailable ? 'Producto habilitado.' : 'Producto deshabilitado.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = 'No se pudo cambiar la disponibilidad del producto.';
        this.actionSuccessMessage = '';
        this.loadMenu();
      }
    });
  }

  startCreateTable(): void {
    this.editingTableId = null;
    this.tableForm = {
      tableNumber: null,
      capacity: null,
      price: null,
      discount: null,
      tableState: this.tableStates[0]
    };
  }

  startEditTable(table: ClubTable): void {
    this.editingTableId = table.tableId;
    this.tableForm = {
      tableNumber: table.tableNumber,
      capacity: table.capacity,
      price: table.price,
      discount: table.discount,
      tableState: this.normalizeTableState(table.tableState)
    };
    this.clearActionMessages();
  }

  cancelEditTable(): void {
    this.startCreateTable();
  }

  saveTable(): void {
    if (!this.clubId || this.savingTable) {
      return;
    }

    if (this.tableForm.tableNumber == null || this.tableForm.capacity == null || this.tableForm.price == null) {
      this.actionErrorMessage = 'Completa numero de mesa, capacidad y precio.';
      this.actionSuccessMessage = '';
      return;
    }

    const payload: TablePayload = {
      tableNumber: Number(this.tableForm.tableNumber),
      capacity: Number(this.tableForm.capacity),
      price: Number(this.tableForm.price),
      discount: Number(this.tableForm.discount ?? 0),
      tableState: this.tableForm.tableState
    };

    if (payload.tableNumber <= 0 || payload.capacity <= 0 || payload.price < 0 || payload.discount < 0) {
      this.actionErrorMessage = 'Revisa los valores de la mesa.';
      this.actionSuccessMessage = '';
      return;
    }

    this.savingTable = true;
    const request$ = this.editingTableId
      ? this.clubService.updateTable(this.clubId, this.editingTableId, payload)
      : this.clubService.createTable(this.clubId, payload);

    request$.pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.savingTable = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (savedTable) => {
        this.upsertTableInList(savedTable);
        this.startCreateTable();
        this.actionSuccessMessage = 'Mesa guardada correctamente.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadTables();
        this.actionErrorMessage = 'No se pudo confirmar el guardado de la mesa. Se recargaron las mesas.';
        this.actionSuccessMessage = '';
      }
    });
  }

  deleteTable(table: ClubTable): void {
    if (!this.clubId) {
      return;
    }
    if (!window.confirm(`Eliminar mesa ${table.tableNumber}?`)) {
      return;
    }

    this.clubService.deleteTable(this.clubId, table.tableId).subscribe({
      next: () => {
        this.tables = this.tables.filter((item) => item.tableId !== table.tableId);
        this.cdr.detectChanges();
        this.actionSuccessMessage = 'Mesa eliminada.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = 'No se pudo eliminar la mesa.';
        this.actionSuccessMessage = '';
      }
    });
  }

  startCreateEvent(): void {
    this.editingEventId = null;
    this.eventForm = {
      eventName: '',
      description: '',
      date: ''
    };
  }

  startEditEvent(event: Event): void {
    this.editingEventId = event.eventId;
    this.eventForm = {
      eventName: event.eventName,
      description: event.description,
      date: (event.date || '').slice(0, 10)
    };
    this.clearActionMessages();
  }

  cancelEditEvent(): void {
    this.startCreateEvent();
  }

  saveEvent(): void {
    if (!this.clubId || this.savingEvent) {
      return;
    }

    const payload: EventPayload = {
      eventName: this.eventForm.eventName.trim(),
      description: this.eventForm.description.trim(),
      date: this.eventForm.date
    };

    if (!payload.eventName || !payload.date) {
      this.actionErrorMessage = 'Completa nombre y fecha del evento.';
      this.actionSuccessMessage = '';
      return;
    }

    this.savingEvent = true;
    const request$ = this.editingEventId
      ? this.clubService.updateEvent(this.clubId, this.editingEventId, payload)
      : this.clubService.createEvent(this.clubId, payload);

    request$.pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.savingEvent = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (savedEvent) => {
        this.upsertEventInList(savedEvent);
        this.startCreateEvent();
        this.actionSuccessMessage = 'Evento guardado correctamente.';
        this.actionErrorMessage = '';
        this.loadEvents();
      },
      error: () => {
        this.loadEvents();
        this.actionErrorMessage = 'No se pudo confirmar el guardado del evento. Se recargaron los eventos.';
        this.actionSuccessMessage = '';
      }
    });
  }

  deleteEvent(event: Event): void {
    if (!this.clubId || !event.eventId) {
      return;
    }
    if (!window.confirm(`Eliminar evento ${event.eventName}?`)) {
      return;
    }

    this.clubService.deleteEvent(this.clubId, event.eventId).subscribe({
      next: () => {
        this.events = this.events.filter((item) => item.eventId !== event.eventId);
        this.eventPage = Math.min(this.eventPage, this.eventTotalPages);
        if (this.pagedEvents.length === 0 && this.eventPage > 1) {
          this.eventPage -= 1;
        }
        this.cdr.detectChanges();
        this.actionSuccessMessage = 'Evento eliminado.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = 'No se pudo eliminar el evento.';
        this.actionSuccessMessage = '';
      }
    });
  }

  onEventPhotoSelected(eventId: string, uploadEvent: globalThis.Event): void {
    const input = uploadEvent.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.clubId || !eventId) {
      return;
    }

    this.uploadingEventPhotoId = eventId;
    this.clubService.uploadEventPhoto(this.clubId, eventId, file).subscribe({
      next: () => {
        this.uploadingEventPhotoId = null;
        this.loadEvents();
        this.actionSuccessMessage = 'Foto de evento actualizada.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.uploadingEventPhotoId = null;
        this.actionErrorMessage = 'No se pudo subir la foto del evento.';
        this.actionSuccessMessage = '';
      }
    });

    input.value = '';
  }

  removeEventPhoto(event: Event): void {
    if (!this.clubId || !event.eventId) {
      return;
    }

    this.clubService.deleteEventPhoto(this.clubId, event.eventId).subscribe({
      next: (updatedEvent) => {
        this.upsertEventInList(updatedEvent);
        this.loadEvents();
        this.actionSuccessMessage = 'Foto de evento eliminada.';
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = 'No se pudo eliminar la foto del evento.';
        this.actionSuccessMessage = '';
      }
    });
  }

  getEventPhotoUrl(event: Event): string {
    const photoLink = event.links?.photo;
    if (photoLink) {
      return this.clubService.toAbsoluteUrl(photoLink);
    }

    if (!this.clubId || !event.eventId || !event.photoId) {
      return '';
    }

    return this.clubService.toAbsoluteUrl(`/clubs/${this.clubId}/events/${event.eventId}/photo`);
  }

  getGalleryPhotoUrl(photo: ClubPhoto): string {
    if (photo.photoId) {
      return this.clubService.getPhotoUrl(photo.photoId);
    }
    return this.clubService.toAbsoluteUrl(photo.url, this.clubId);
  }

  private loadClubHandlerData(): void {
    this.loading = true;
    this.club = null;
    this.coverUrl = '';
    this.layoutUrl = '';
    this.galleryPhotos = [];
    this.clubLoadAttempted = false;
    this.errorMessage = '';

    // Avoid indefinite loading when any secondary endpoint hangs.
    const loadingTimeout = setTimeout(() => {
      this.loading = false;
      this.clubLoadAttempted = true;
      this.cdr.markForCheck();
    }, 7000);

    this.loadClub(() => {
      clearTimeout(loadingTimeout);
      this.loading = false;
      this.clubLoadAttempted = true;
      this.cdr.markForCheck();
    });
    this.loadMenu();
    this.loadTables();
    this.loadEvents();
    this.loadLayout();
    this.loadGalleryPhotos();
    this.startCreateProduct();
    this.startCreateTable();
    this.startCreateEvent();

    this.loadReviews();
  }

  private loadClub(onComplete?: () => void): void {
    this.clubService.getClubById(this.clubId).pipe(
      timeout(6000)
    ).subscribe({
      next: (club) => {
        this.club = club;
        this.coverUrl = this.resolveCoverUrl(club);
        onComplete?.();
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar la informacion del club (timeout o error de red).';
        onComplete?.();
        this.cdr.detectChanges();
      }
    });
  }

  private loadMenu(): void {
    this.clubService.getMenu(this.clubId).subscribe({
      next: (menu) => {
        this.menu = (menu ?? []).map((product) => ({
          ...product,
          productType: this.normalizeProductType(product.productType)
        }));
        this.cdr.detectChanges();
      },
      error: () => {
        this.menu = [];
        this.cdr.detectChanges();
      }
    });
  }

  private loadLayout(): void {
    this.clubService.getLayout(this.clubId).subscribe({
      next: (layout) => {
        this.layoutUrl = this.resolveLayoutUrl(layout);
        this.cdr.detectChanges();
      },
      error: () => {
        this.layoutUrl = '';
        this.cdr.detectChanges();
      }
    });
  }

  private loadGalleryPhotos(): void {
    this.clubService.getClubPhotos(this.clubId).subscribe({
      next: (photos) => {
        this.galleryPhotos = (photos ?? [])
          .filter((photo) => photo.photoType === 'CAROUSEL')
          .map((photo) => ({
            ...photo,
            url: photo.photoId
              ? this.clubService.getPhotoUrl(photo.photoId)
              : this.clubService.toAbsoluteUrl(photo.url, this.clubId)
          }));
        this.cdr.detectChanges();
      },
      error: () => {
        this.galleryPhotos = [];
        this.cdr.detectChanges();
      }
    });
  }

  private loadTables(): void {
    this.clubService.getTables(this.clubId).subscribe({
      next: (tables) => {
        this.tables = (tables ?? []).map((table) => ({
          ...table,
          tableState: this.normalizeTableState(table.tableState)
        }));
        this.cdr.detectChanges();
      },
      error: () => {
        this.tables = [];
        this.cdr.detectChanges();
      }
    });
  }

  private loadEvents(): void {
    this.clubService.getEvents(this.clubId).subscribe({
      next: (events) => {
        this.events = events.map((event) => this.normalizeEvent(event));
        this.cdr.detectChanges();
      },
      error: () => {
        this.events = [];
        this.cdr.detectChanges();
      }
    });
  }

  private loadReviews(onComplete?: () => void): void {
    this.clubService.getReviewsPaginated(this.clubId, this.reviewPage - 1, this.reviewPageSize).subscribe({
      next: (response) => {
        this.reviews = response?.content ?? [];
        this.reviewTotalPages = Math.max(response?.totalPages ?? 0, 1);
        onComplete?.();
      },
      error: () => {
        this.reviews = [];
        this.reviewTotalPages = 1;
        onComplete?.();
      }
    });
  }

  private resolveCoverUrl(club: Club): string {
    const cover = club.links?.cover;
    if (cover) {
      if (/^https?:\/\//i.test(cover)) {
        return cover;
      }
      const normalized = cover.startsWith('/') ? cover.slice(1) : cover;
      return new URL(normalized, 'http://localhost:8080/webapp_war_exploded/').toString();
    }

    return this.clubService.getCoverUrl(club.clubId);
  }

  private resolveLayoutUrl(layout: Layout): string {
    const raw = layout?.url || layout?.image || '';
    if (!raw) {
      return '';
    }
    return this.withCacheBust(this.clubService.toAbsoluteUrl(raw, this.clubId));
  }

  private withCacheBust(url: string): string {
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}t=${Date.now()}`;
  }

  private clearActionMessages(): void {
    this.actionErrorMessage = '';
    this.actionSuccessMessage = '';
  }

  private normalizeProductType(raw: string): string {
    const normalized = (raw || '').toUpperCase();
    if (normalized === 'ALCOHOLICAS' || normalized === 'ALCOHOLICA') {
      return 'ALCOHOLICA';
    }
    if (normalized === 'NOALCOHOLICAS' || normalized === 'NO_ALCOHOLICAS' || normalized === 'NO_ALCOHOLICA') {
      return 'NO_ALCOHOLICA';
    }
    if (normalized === 'TRAGOS') {
      return 'TRAGOS';
    }
    return this.productTypes[0];
  }

  private normalizeTableState(raw: string): string {
    const normalized = (raw || '').toUpperCase();
    if (normalized === 'DISPONIBLE' || normalized === 'RESERVADA' || normalized === 'PENDIENTE') {
      return normalized;
    }
    return this.tableStates[0];
  }

  private upsertProductInList(product: Product): void {
    if (!product?.productId) {
      this.loadMenu();
      return;
    }

    const normalized: Product = {
      ...product,
      productType: this.normalizeProductType(product.productType)
    };

    const currentIndex = this.menu.findIndex((item) => item.productId === normalized.productId);
    if (currentIndex >= 0) {
      this.menu = this.menu.map((item, index) => (index === currentIndex ? normalized : item));
      this.cdr.detectChanges();
      return;
    }

    this.menu = [normalized, ...this.menu];
    this.cdr.detectChanges();
  }

  private upsertTableInList(table: ClubTable): void {
    if (!table?.tableId) {
      this.loadTables();
      return;
    }

    const normalized: ClubTable = {
      ...table,
      tableState: this.normalizeTableState(table.tableState)
    };

    const currentIndex = this.tables.findIndex((item) => item.tableId === normalized.tableId);
    if (currentIndex >= 0) {
      this.tables = this.tables.map((item, index) => (index === currentIndex ? normalized : item));
      this.cdr.detectChanges();
      return;
    }

    this.tables = [normalized, ...this.tables];
    this.cdr.detectChanges();
  }

  private normalizeEvent(event: Event): Event {
    return {
      ...event,
      eventId: event.eventId || event.id || ''
    };
  }

  private upsertEventInList(event: Event): void {
    const normalized = this.normalizeEvent(event);
    if (!normalized.eventId) {
      this.loadEvents();
      return;
    }

    const currentIndex = this.events.findIndex((item) => item.eventId === normalized.eventId);
    if (currentIndex >= 0) {
      this.events = this.events.map((item, index) => (index === currentIndex ? normalized : item));
    } else {
      this.events = [normalized, ...this.events];
    }

    this.eventPage = 1;
    this.cdr.detectChanges();
  }

}
