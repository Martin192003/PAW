import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';

import {
  ClubService,
  ClubUpdatePayload,
  EventPayload,
  ProductPayload,
  TablePayload
} from '../services/club.service';
import { ReviewService } from '../services/review.service';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { Club } from '../models/club.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Event } from '../models/event.model';
import { Review } from '../models/review.model';
import { Layout } from '../models/layout.model';
import { ClubPhoto } from '../models/club-photo.model';
import { catchError, finalize, map, of, Subject, switchMap, takeUntil, timeout } from 'rxjs';
import { I18nService } from '../services/i18n.service';

type ClubHandlerTab = 'menu' | 'tables' | 'events' | 'reviews';
type EventFilter = 'all' | 'past' | 'future';
type TableForm = {
  tableNumber: number | null;
  capacity: number | null;
  price: number | null;
  discount: number | null;
};
type ProductForm = Omit<ProductPayload, 'basePrice' | 'isAvailable'> & {
  basePrice: number | null;
};

@Component({
  standalone: true,
  selector: 'app-club-handler-page',
  imports: [CommonModule, FormsModule, SideBarComponent, TranslateModule],
  templateUrl: './club-handler.page.html',
  styleUrl: './club-handler.page.css'
})
export class ClubHandlerPage implements OnInit, OnDestroy {
  private readonly mutationTimeoutMs = 12000;
  private readonly destroy$ = new Subject<void>();
  private actionSuccessTimeout: ReturnType<typeof setTimeout> | undefined;

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

  menuPage = 1;
  readonly menuPageSize = 10;

  tablePage = 1;
  readonly tablePageSize = 10;

  eventFilter: EventFilter = 'all';
  eventPage = 1;
  readonly eventPageSize = 10;

  reviewPage = 1;
  readonly reviewPageSize = 10;
  reviewTotalPages = 0;

  readonly productTypes = ['ALCOHOLICA', 'NO_ALCOHOLICA', 'TRAGOS'];
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
  deletingProductId: string | null = null;
  productPendingDelete: Product | null = null;
  deleteProductModalOpen = false;
  productForm: ProductForm = {
    productName: '',
    description: '',
    basePrice: null,
    productType: 'ALCOHOLICA'
  };

  savingTable = false;
  editingTableId: string | null = null;
  deletingTableId: string | null = null;
  tablePendingDelete: ClubTable | null = null;
  deleteTableModalOpen = false;
  tableForm: TableForm = {
    tableNumber: null,
    capacity: null,
    price: null,
    discount: null
  };


  tableSubmittedAttempted = false;
  tableFrozenErrors: { [key: string]: boolean } = {};
  tableFrozenErrorMessages: { [key: string]: string } = {};
  private static readonly TABLE_FIELDS = ['tableNumber', 'capacity', 'price', 'discount'];


  clubSubmittedAttempted = false;
  clubFrozenErrors: { [key: string]: boolean } = {};
  clubFrozenErrorMessages: { [key: string]: string } = {};
  private static readonly CLUB_FIELDS = ['clubName', 'location', 'cbu', 'description'];


  productSubmittedAttempted = false;
  productFrozenErrors: { [key: string]: boolean } = {};
  productFrozenErrorMessages: { [key: string]: string } = {};
  private static readonly PRODUCT_FIELDS = ['productName', 'basePrice', 'description'];

  eventSubmittedAttempted = false;
  eventFrozenErrors: { [key: string]: boolean } = {};
  eventFrozenErrorMessages: { [key: string]: string } = {};
  private static readonly EVENT_FIELDS = ['eventName', 'date', 'description'];

  savingEvent = false;
  editingEventId: string | null = null;
  deletingEventId: string | null = null;
  eventPendingDelete: Event | null = null;
  deleteEventModalOpen = false;
  eventForm: EventPayload = {
    eventName: '',
    description: '',
    date: ''
  };

  selectedEventPhotoFile: File | null = null;
  selectedEventPhotoName = '';

  uploadingEventPhotoId: string | null = null;
  uploadingCover = false;
  uploadingLayout = false;
  uploadingGallery = false;
  deletingGalleryPhotoId: string | null = null;
  galleryPhotoPendingDelete: ClubPhoto | null = null;
  deleteGalleryPhotoModalOpen = false;
  private readonly failedEventPhotoIds = new Set<string>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly clubService: ClubService,
    private readonly reviewService: ReviewService,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService,
    private readonly zone: NgZone
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const clubId = params.get('clubId');
      if (!clubId) {
        this.errorMessage = this.i18n.instant('clubHandler.errors.loadSelectedClub');
        this.loading = false;
        return;
      }

      this.clubId = clubId;
      this.eventPage = 1;
      this.reviewPage = 1;
      this.loadClubHandlerData();
    });
  }

  ngOnDestroy(): void {
    if (this.actionSuccessTimeout) {
      clearTimeout(this.actionSuccessTimeout);
    }
    this.destroy$.next();
    this.destroy$.complete();
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
    this.clubService.uploadCover(this.clubId, file, this.club?.links).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.uploadingCover = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.coverUrl = this.withCacheBust(
          this.club ? this.resolveCoverUrl(this.club) : this.clubService.getCoverUrl(this.clubId)
        );
        this.loadGalleryPhotos();
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.coverUpdated'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.coverUpdateFailed');
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
    this.clubService.uploadLayout(this.clubId, file, this.club?.links).pipe(
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
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.layoutUpdated'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadLayout();
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.layoutUpdateFailed');
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

    this.clubService.uploadGalleryPhotos(this.clubId, selected, this.club?.links).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.uploadingGallery = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.loadGalleryPhotos();
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.galleryUpdated'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadGalleryPhotos();
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.galleryUpdateFailed');
        this.actionSuccessMessage = '';
      }
    });

    input.value = '';
  }

  openDeleteGalleryPhotoModal(photo: ClubPhoto): void {
    if (!photo.photoId || this.deletingGalleryPhotoId) {
      return;
    }
    this.galleryPhotoPendingDelete = photo;
    this.deleteGalleryPhotoModalOpen = true;
  }

  closeDeleteGalleryPhotoModal(): void {
    if (this.deletingGalleryPhotoId) {
      return;
    }
    this.deleteGalleryPhotoModalOpen = false;
    this.galleryPhotoPendingDelete = null;
  }

  confirmDeleteGalleryPhoto(): void {
    const photo = this.galleryPhotoPendingDelete;
    if (!this.clubId || !photo?.photoId) {
      return;
    }

    this.deletingGalleryPhotoId = photo.photoId;

    this.clubService.deleteGalleryPhoto(this.clubId, photo.photoId, photo.links, this.club?.links).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.deletingGalleryPhotoId = null;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.zone.run(() => {
          this.galleryPhotos = this.galleryPhotos.filter((item) => item.photoId !== photo.photoId);
          this.deleteGalleryPhotoModalOpen = false;
          this.galleryPhotoPendingDelete = null;
          this.showActionSuccess(this.i18n.instant('clubHandler.messages.galleryPhotoDeleted'));
          this.actionErrorMessage = '';
          this.cdr.detectChanges();
          this.loadGalleryPhotos();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.actionErrorMessage = this.i18n.instant('clubHandler.messages.galleryPhotoDeleteFailed');
          this.actionSuccessMessage = '';
          this.loadGalleryPhotos();
          this.cdr.detectChanges();
        });
      }
    });
  }

  setTab(tab: ClubHandlerTab): void {
    this.activeTab = tab;
    this.clearActionMessages();
  }

  get menuTotalPages(): number {
    const total = Math.ceil(this.menu.length / this.menuPageSize);
    return Math.max(total, 1);
  }

  get pagedMenu(): Product[] {
    const start = (this.menuPage - 1) * this.menuPageSize;
    return this.menu.slice(start, start + this.menuPageSize);
  }

  prevMenuPage(): void {
    if (this.menuPage <= 1) {
      return;
    }
    this.menuPage -= 1;
  }

  nextMenuPage(): void {
    if (this.menuPage >= this.menuTotalPages) {
      return;
    }
    this.menuPage += 1;
  }

  get tableTotalPages(): number {
    const total = Math.ceil(this.tables.length / this.tablePageSize);
    return Math.max(total, 1);
  }

  get pagedTables(): ClubTable[] {
    const start = (this.tablePage - 1) * this.tablePageSize;
    return this.tables.slice(start, start + this.tablePageSize);
  }

  prevTablePage(): void {
    if (this.tablePage <= 1) {
      return;
    }
    this.tablePage -= 1;
  }

  nextTablePage(): void {
    if (this.tablePage >= this.tableTotalPages) {
      return;
    }
    this.tablePage += 1;
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
    return review.user?.username || review.username || this.i18n.instant('clubHandler.userFallback');
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
      cbu: this.club.cbu ?? ''
    };
    this.editingClub = true;
    this.resetClubValidation();
    this.clearActionMessages();
  }

  cancelEditClub(): void {
    this.editingClub = false;
  }

  showClubError(field: string): boolean {
    if (!this.clubSubmittedAttempted) return false;
    return !!this.clubFrozenErrors[field];
  }

  getClubError(field: string): string {
    return this.clubFrozenErrorMessages[field] ?? '';
  }

  private resetClubValidation(): void {
    this.clubSubmittedAttempted = false;
    this.clubFrozenErrors = {};
    this.clubFrozenErrorMessages = {};
  }

  private clubFieldError(field: string): string {
    const raw = (this.clubForm as unknown as Record<string, unknown>)[field];
    const value = typeof raw === 'string' ? raw.trim() : '';

    switch (field) {
      case 'clubName':
        if (!value) return this.i18n.instant('clubHandler.clubErrors.required');
        if (value.length > 100) return this.i18n.instant('clubHandler.clubErrors.clubNameMax');
        return '';
      case 'location':
        if (!value) return this.i18n.instant('clubHandler.clubErrors.required');
        if (value.length > 255) return this.i18n.instant('clubHandler.clubErrors.locationMax');
        return '';
      case 'cbu':
        if (!value) return this.i18n.instant('clubHandler.clubErrors.required');
        if (/\D/.test(value)) return this.i18n.instant('clubHandler.clubErrors.required');
        if (value.length > 50) return this.i18n.instant('clubHandler.clubErrors.cbuMax');
        return '';
      case 'description':
        if (!value) return this.i18n.instant('clubHandler.clubErrors.required');
        if (value.length > 1000) return this.i18n.instant('clubHandler.clubErrors.descriptionMax');
        return '';
      default:
        return '';
    }
  }

  onClubCbuInput(value: string): void {
    this.clubForm.cbu = this.digitsOnly(value);
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
      cbu: this.digitsOnly(this.clubForm.cbu)
    };
    this.clubForm = payload;

    this.clubSubmittedAttempted = true;
    this.clubFrozenErrors = {};
    this.clubFrozenErrorMessages = {};

    let hasError = false;
    for (const field of ClubHandlerPage.CLUB_FIELDS) {
      const message = this.clubFieldError(field);
      if (message) {
        this.clubFrozenErrors[field] = true;
        this.clubFrozenErrorMessages[field] = message;
        hasError = true;
      }
    }

    if (hasError) {
      this.actionErrorMessage = '';
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
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.clubUpdated'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadClub();
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.clubUpdateFailed');
        this.actionSuccessMessage = '';
      }
    });
  }

  private digitsOnly(value: string): string {
    return value.replace(/\D/g, '');
  }

  startCreateProduct(): void {
    this.editingProductId = null;
    this.productForm = {
      productName: '',
      description: '',
      basePrice: null,
      productType: this.productTypes[0]
    };
    this.resetProductValidation();
  }

  startEditProduct(product: Product): void {
    this.editingProductId = product.productId;
    this.productForm = {
      productName: product.productName,
      description: product.description,
      basePrice: product.basePrice,
      productType: this.normalizeProductType(product.productType)
    };
    this.resetProductValidation();
    this.clearActionMessages();
  }

  cancelEditProduct(): void {
    this.startCreateProduct();
  }

  showProductError(field: string): boolean {
    if (!this.productSubmittedAttempted) return false;
    return !!this.productFrozenErrors[field];
  }

  getProductError(field: string): string {
    return this.productFrozenErrorMessages[field] ?? '';
  }

  private resetProductValidation(): void {
    this.productSubmittedAttempted = false;
    this.productFrozenErrors = {};
    this.productFrozenErrorMessages = {};
  }

  private productFieldError(field: string): string {
    switch (field) {
      case 'productName': {
        const value = (this.productForm.productName ?? '').trim();
        if (!value) return this.i18n.instant('clubHandler.productErrors.required');
        if (value.length > 100) return this.i18n.instant('clubHandler.productErrors.productNameMax');
        return '';
      }
      case 'basePrice': {
        const raw = this.productForm.basePrice;
        if (raw === null || raw === undefined || (raw as unknown) === '') {
          return this.i18n.instant('clubHandler.productErrors.required');
        }
        const value = Number(raw);
        if (!Number.isFinite(value) || value <= 0) return this.i18n.instant('clubHandler.productErrors.invalid');
        if (value >= 99999999.99) return this.i18n.instant('clubHandler.productErrors.priceMax');
        return '';
      }
      case 'description': {
        const value = (this.productForm.description ?? '').trim();
        if (!value) return this.i18n.instant('clubHandler.productErrors.required');
        if (value.length > 300) return this.i18n.instant('clubHandler.productErrors.descriptionMax');
        return '';
      }
      default:
        return '';
    }
  }

  saveProduct(): void {
    if (!this.clubId || this.savingProduct) {
      return;
    }

    this.productSubmittedAttempted = true;
    this.productFrozenErrors = {};
    this.productFrozenErrorMessages = {};

    let hasError = false;
    for (const field of ClubHandlerPage.PRODUCT_FIELDS) {
      const message = this.productFieldError(field);
      if (message) {
        this.productFrozenErrors[field] = true;
        this.productFrozenErrorMessages[field] = message;
        hasError = true;
      }
    }

    if (hasError) {
      this.actionErrorMessage = '';
      this.actionSuccessMessage = '';
      return;
    }

    const payload: ProductPayload = {
      ...this.productForm,
      productName: this.productForm.productName.trim(),
      description: this.productForm.description.trim(),
      basePrice: Number(this.productForm.basePrice),
      isAvailable: true
    };

    this.savingProduct = true;
    const editingProduct = this.editingProductId
      ? this.menu.find(product => product.productId === this.editingProductId)
      : undefined;
    const request$ = this.editingProductId
      ? this.clubService.updateProduct(this.clubId, this.editingProductId, payload, editingProduct)
      : this.clubService.createProduct(this.clubId, payload, this.club?.links);

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
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.productSaved'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.loadMenu();
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.productSaveFailed');
        this.actionSuccessMessage = '';
      }
    });
  }

  openDeleteProductModal(product: Product): void {
    if (!product.productId || this.deletingProductId) {
      return;
    }
    this.productPendingDelete = product;
    this.deleteProductModalOpen = true;
  }

  closeDeleteProductModal(): void {
    if (this.deletingProductId) {
      return;
    }
    this.deleteProductModalOpen = false;
    this.productPendingDelete = null;
  }

  confirmDeleteProduct(): void {
    const product = this.productPendingDelete;
    if (!this.clubId || !product?.productId) {
      return;
    }

    this.deletingProductId = product.productId;
    this.clubService.deleteProduct(this.clubId, product.productId, product).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.deletingProductId = null;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.zone.run(() => {
          this.menu = this.menu.filter((item) => item.productId !== product.productId);
          this.menuPage = Math.min(this.menuPage, this.menuTotalPages);
          if (this.pagedMenu.length === 0 && this.menuPage > 1) {
            this.menuPage -= 1;
          }
          if (this.editingProductId === product.productId) {
            this.startCreateProduct();
          }
          this.deleteProductModalOpen = false;
          this.productPendingDelete = null;
          this.showActionSuccess(this.i18n.instant('clubHandler.messages.productDeleted'));
          this.actionErrorMessage = '';
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.actionErrorMessage = this.i18n.instant('clubHandler.messages.productDeleteFailed');
          this.actionSuccessMessage = '';
          this.loadMenu();
          this.cdr.detectChanges();
        });
      }
    });
  }

  startCreateTable(): void {
    this.editingTableId = null;
    this.tableForm = {
      tableNumber: null,
      capacity: null,
      price: null,
      discount: null
    };
    this.resetTableValidation();
  }

  startEditTable(table: ClubTable): void {
    this.editingTableId = table.tableId;
    this.tableForm = {
      tableNumber: table.tableNumber,
      capacity: table.capacity,
      price: table.price,
      discount: table.discount
    };
    this.resetTableValidation();
    this.clearActionMessages();
  }

  cancelEditTable(): void {
    this.startCreateTable();
  }

  showTableError(field: string): boolean {
    if (!this.tableSubmittedAttempted) return false;
    return !!this.tableFrozenErrors[field];
  }

  getTableError(field: string): string {
    return this.tableFrozenErrorMessages[field] ?? '';
  }

  private resetTableValidation(): void {
    this.tableSubmittedAttempted = false;
    this.tableFrozenErrors = {};
    this.tableFrozenErrorMessages = {};
  }

  private tableFieldError(field: string): string {
    const raw = (this.tableForm as unknown as Record<string, unknown>)[field];
    const isEmpty = raw === null || raw === undefined || raw === '';
    const value = Number(raw);

    switch (field) {
      case 'tableNumber':
        if (isEmpty) return this.i18n.instant('clubHandler.tableErrors.required');
        if (!Number.isFinite(value) || value <= 0) return this.i18n.instant('clubHandler.tableErrors.invalid');
        if (value >= 1_000_000) return this.i18n.instant('clubHandler.tableErrors.tableNumberMax');
        return '';
      case 'capacity':
        if (isEmpty) return this.i18n.instant('clubHandler.tableErrors.required');
        if (!Number.isFinite(value) || value <= 0) return this.i18n.instant('clubHandler.tableErrors.invalid');
        if (value >= 10_000) return this.i18n.instant('clubHandler.tableErrors.capacityMax');
        return '';
      case 'price':
        if (isEmpty) return this.i18n.instant('clubHandler.tableErrors.required');
        if (!Number.isFinite(value) || value < 0) return this.i18n.instant('clubHandler.tableErrors.invalid');
        if (value >= 99999999.99) return this.i18n.instant('clubHandler.tableErrors.priceMax');
        return '';
      case 'discount':
        // El descuento es opcional (default 0), a diferencia de los otros campos.
        if (isEmpty) return '';
        if (!Number.isFinite(value) || value < 0 || value > 100) return this.i18n.instant('clubHandler.tableErrors.discountRange');
        return '';
      default:
        return '';
    }
  }

  saveTable(): void {
    if (!this.clubId || this.savingTable) {
      return;
    }

    this.tableSubmittedAttempted = true;
    this.tableFrozenErrors = {};
    this.tableFrozenErrorMessages = {};

    let hasError = false;
    for (const field of ClubHandlerPage.TABLE_FIELDS) {
      const message = this.tableFieldError(field);
      if (message) {
        this.tableFrozenErrors[field] = true;
        this.tableFrozenErrorMessages[field] = message;
        hasError = true;
      }
    }

    if (hasError) {
      this.actionErrorMessage = '';
      this.actionSuccessMessage = '';
      return;
    }

    const payload: TablePayload = {
      tableNumber: Number(this.tableForm.tableNumber),
      capacity: Number(this.tableForm.capacity),
      price: Number(this.tableForm.price),
      discount: Number(this.tableForm.discount ?? 0),
      tableState: 'DISPONIBLE'
    };

    this.savingTable = true;
    const editingTable = this.editingTableId
      ? this.tables.find(table => table.tableId === this.editingTableId)
      : undefined;
    const request$ = this.editingTableId
      ? this.clubService.updateTable(this.clubId, this.editingTableId, payload, editingTable)
      : this.clubService.createTable(this.clubId, payload, this.club?.links);

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
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.tableSaved'));
        this.actionErrorMessage = '';
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 409) {
          this.tableSubmittedAttempted = true;
          this.tableFrozenErrors['tableNumber'] = true;
          this.tableFrozenErrorMessages['tableNumber'] = this.i18n.instant('clubHandler.tableErrors.duplicateNumber');
          this.actionErrorMessage = '';
          this.actionSuccessMessage = '';
          return;
        }

        this.loadTables();
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.tableSaveFailed');
        this.actionSuccessMessage = '';
      }
    });
  }

  openDeleteTableModal(table: ClubTable): void {
    if (!table.tableId || this.deletingTableId) {
      return;
    }
    this.tablePendingDelete = table;
    this.deleteTableModalOpen = true;
  }

  closeDeleteTableModal(): void {
    if (this.deletingTableId) {
      return;
    }
    this.deleteTableModalOpen = false;
    this.tablePendingDelete = null;
  }

  confirmDeleteTable(): void {
    const table = this.tablePendingDelete;
    if (!this.clubId || !table?.tableId) {
      return;
    }

    this.deletingTableId = table.tableId;

    this.clubService.deleteTable(this.clubId, table.tableId, table).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.deletingTableId = null;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.zone.run(() => {
          this.tables = this.tables.filter((item) => item.tableId !== table.tableId);
          this.tablePage = Math.min(this.tablePage, this.tableTotalPages);
          if (this.pagedTables.length === 0 && this.tablePage > 1) {
            this.tablePage -= 1;
          }
          if (this.editingTableId === table.tableId) {
            this.startCreateTable();
          }
          this.deleteTableModalOpen = false;
          this.tablePendingDelete = null;
          this.showActionSuccess(this.i18n.instant('clubHandler.messages.tableDeleted'));
          this.actionErrorMessage = '';
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.actionErrorMessage = this.i18n.instant('clubHandler.messages.tableDeleteFailed');
          this.actionSuccessMessage = '';
          this.loadTables();
          this.cdr.detectChanges();
        });
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
    this.clearSelectedEventPhoto();
    this.resetEventValidation();
  }

  startEditEvent(event: Event): void {
    this.editingEventId = event.eventId;
    this.eventForm = {
      eventName: event.eventName,
      description: event.description,
      date: (event.date || '').slice(0, 10)
    };
    this.clearSelectedEventPhoto();
    this.resetEventValidation();
    this.clearActionMessages();
  }

  onNewEventPhotoSelected(uploadEvent: globalThis.Event): void {
    const input = uploadEvent.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.selectedEventPhotoFile = file;
    this.selectedEventPhotoName = file.name;
  }

  clearSelectedEventPhoto(): void {
    this.selectedEventPhotoFile = null;
    this.selectedEventPhotoName = '';
  }

  cancelEditEvent(): void {
    this.startCreateEvent();
  }

  showEventError(field: string): boolean {
    if (!this.eventSubmittedAttempted) return false;
    return !!this.eventFrozenErrors[field];
  }

  getEventError(field: string): string {
    return this.eventFrozenErrorMessages[field] ?? '';
  }

  private resetEventValidation(): void {
    this.eventSubmittedAttempted = false;
    this.eventFrozenErrors = {};
    this.eventFrozenErrorMessages = {};
  }

  private eventFieldError(field: string): string {
    switch (field) {
      case 'eventName': {
        const value = (this.eventForm.eventName ?? '').trim();
        if (!value) return this.i18n.instant('clubHandler.eventErrors.required');
        if (value.length > 120) return this.i18n.instant('clubHandler.eventErrors.eventNameMax');
        return '';
      }
      case 'date': {
        const value = this.eventForm.date;
        if (!value) return this.i18n.instant('clubHandler.eventErrors.required');
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const parsed = new Date(`${value}T00:00:00`);
        if (Number.isNaN(parsed.getTime())) return this.i18n.instant('clubHandler.eventErrors.required');
        if (parsed < today) return this.i18n.instant('clubHandler.eventErrors.pastDate');
        return '';
      }
      case 'description': {
        const value = (this.eventForm.description ?? '').trim();
        if (value.length > 500) return this.i18n.instant('clubHandler.eventErrors.descriptionMax');
        return '';
      }
      default:
        return '';
    }
  }

  saveEvent(): void {
    if (!this.clubId || this.savingEvent) {
      return;
    }

    this.eventSubmittedAttempted = true;
    this.eventFrozenErrors = {};
    this.eventFrozenErrorMessages = {};

    let hasError = false;
    for (const field of ClubHandlerPage.EVENT_FIELDS) {
      const message = this.eventFieldError(field);
      if (message) {
        this.eventFrozenErrors[field] = true;
        this.eventFrozenErrorMessages[field] = message;
        hasError = true;
      }
    }

    if (hasError) {
      this.actionErrorMessage = '';
      this.actionSuccessMessage = '';
      return;
    }

    const payload: EventPayload = {
      eventName: this.eventForm.eventName.trim(),
      description: this.eventForm.description.trim(),
      date: this.eventForm.date
    };

    this.savingEvent = true;
    const editingEvent = this.editingEventId
      ? this.events.find(event => event.eventId === this.editingEventId)
      : undefined;
    const request$ = this.editingEventId
      ? this.clubService.updateEvent(this.clubId, this.editingEventId, payload, editingEvent)
      : this.clubService.createEvent(this.clubId, payload, this.club?.links);

    const photoToUpload = this.selectedEventPhotoFile;
    const clubId = this.clubId;

    request$.pipe(
      timeout(this.mutationTimeoutMs),
      switchMap((savedEvent) => {
        if (!photoToUpload || !savedEvent.eventId) {
          return of({ savedEvent, photoFailed: false });
        }
        return this.clubService.uploadEventPhoto(clubId, savedEvent.eventId, photoToUpload, savedEvent).pipe(
          timeout(this.mutationTimeoutMs),
          map((eventWithPhoto) => ({ savedEvent: eventWithPhoto, photoFailed: false })),
          catchError(() => of({ savedEvent, photoFailed: true }))
        );
      }),
      finalize(() => {
        this.savingEvent = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: ({ savedEvent, photoFailed }) => {
        if (!photoFailed && savedEvent.eventId) {
          this.failedEventPhotoIds.delete(savedEvent.eventId);
        }
        this.upsertEventInList(savedEvent);
        this.startCreateEvent();
        this.showActionSuccess(this.i18n.instant(
          photoFailed ? 'clubHandler.messages.eventSavedPhotoFailed' : 'clubHandler.messages.eventSaved'
        ));
        this.actionErrorMessage = '';
        this.loadEvents();
      },
      error: (error: HttpErrorResponse) => {

        if (error.status === 400) {
          this.eventSubmittedAttempted = true;
          this.eventFrozenErrors['date'] = true;
          this.eventFrozenErrorMessages['date'] = this.i18n.instant('clubHandler.eventErrors.pastDate');
          this.actionErrorMessage = '';
          this.actionSuccessMessage = '';
          return;
        }

        this.loadEvents();
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.eventSaveFailed');
        this.actionSuccessMessage = '';
      }
    });
  }

  openDeleteEventModal(event: Event): void {
    if (!event.eventId || this.deletingEventId) {
      return;
    }
    this.eventPendingDelete = event;
    this.deleteEventModalOpen = true;
  }

  closeDeleteEventModal(): void {
    if (this.deletingEventId) {
      return;
    }
    this.deleteEventModalOpen = false;
    this.eventPendingDelete = null;
  }

  confirmDeleteEvent(): void {
    const event = this.eventPendingDelete;
    if (!this.clubId || !event?.eventId) {
      return;
    }

    this.deletingEventId = event.eventId;

    this.clubService.deleteEvent(this.clubId, event.eventId, event).pipe(
      timeout(this.mutationTimeoutMs),
      finalize(() => {
        this.deletingEventId = null;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.zone.run(() => {
          this.events = this.events.filter((item) => item.eventId !== event.eventId);
          this.eventPage = Math.min(this.eventPage, this.eventTotalPages);
          if (this.pagedEvents.length === 0 && this.eventPage > 1) {
            this.eventPage -= 1;
          }
          if (this.editingEventId === event.eventId) {
            this.startCreateEvent();
          }
          this.deleteEventModalOpen = false;
          this.eventPendingDelete = null;
          this.showActionSuccess(this.i18n.instant('clubHandler.messages.eventDeleted'));
          this.actionErrorMessage = '';
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.actionErrorMessage = this.i18n.instant('clubHandler.messages.eventDeleteFailed');
          this.actionSuccessMessage = '';
          this.loadEvents();
          this.cdr.detectChanges();
        });
      }
    });
  }

  onEventPhotoSelected(event: Event, uploadEvent: globalThis.Event): void {
    const input = uploadEvent.target as HTMLInputElement;
    const file = input.files?.[0];
    const eventId = event.eventId;
    if (!file || !this.clubId || !eventId) {
      return;
    }

    this.uploadingEventPhotoId = eventId;
    this.clubService.uploadEventPhoto(this.clubId, eventId, file, event).subscribe({
      next: () => {
        this.uploadingEventPhotoId = null;
        this.failedEventPhotoIds.delete(eventId);
        this.loadEvents();
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.eventPhotoUpdated'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.uploadingEventPhotoId = null;
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.eventPhotoUpdateFailed');
        this.actionSuccessMessage = '';
      }
    });

    input.value = '';
  }

  removeEventPhoto(event: Event): void {
    if (!this.clubId || !event.eventId) {
      return;
    }

    this.clubService.deleteEventPhoto(this.clubId, event.eventId, event).subscribe({
      next: () => {
        this.failedEventPhotoIds.add(event.eventId);
        this.upsertEventInList({ ...event, photoId: undefined });
        this.loadEvents();
        this.showActionSuccess(this.i18n.instant('clubHandler.messages.eventPhotoDeleted'));
        this.actionErrorMessage = '';
      },
      error: () => {
        this.actionErrorMessage = this.i18n.instant('clubHandler.messages.eventPhotoDeleteFailed');
        this.actionSuccessMessage = '';
      }
    });
  }

  getEventPhotoUrl(event: Event): string {
    if (event.eventId && this.failedEventPhotoIds.has(event.eventId)) {
      return '';
    }

    if (!this.clubId || !event.eventId || !event.photoId) {
      return '';
    }

    const photoLink = event.links?.['photo'];
    if (photoLink) {
      return this.clubService.toAbsoluteUrl(photoLink);
    }

    return this.clubService.toAbsoluteUrl(`/events/${event.eventId}/photo`);
  }

  onEventPhotoError(event: Event): void {
    if (event.eventId) {
      this.failedEventPhotoIds.add(event.eventId);
    }
    this.cdr.detectChanges();
  }

  getGalleryPhotoUrl(photo: ClubPhoto): string {
    if (photo.url) {
      return this.clubService.toAbsoluteUrl(photo.url, this.clubId);
    }
    if (photo.photoId) {
      return this.clubService.getPhotoUrl(photo.photoId);
    }
    return '';
  }

  private loadClubHandlerData(): void {
    this.loading = true;
    this.club = null;
    this.coverUrl = '';
    this.layoutUrl = '';
    this.galleryPhotos = [];
    this.menuPage = 1;
    this.tablePage = 1;
    this.eventPage = 1;
    this.reviewPage = 1;
    this.clubLoadAttempted = false;
    this.errorMessage = '';

    const loadingTimeout = setTimeout(() => {
      this.loading = false;
      this.clubLoadAttempted = true;
      this.cdr.markForCheck();
    }, 7000);

    this.loadClub(() => {
      this.loadSecondaryClubData();
    }, () => {
      clearTimeout(loadingTimeout);
      this.loading = false;
      this.clubLoadAttempted = true;
      this.cdr.markForCheck();
    });
    this.startCreateProduct();
    this.startCreateTable();
    this.startCreateEvent();
  }

  private loadSecondaryClubData(): void {
    this.loadMenu();
    this.loadTables();
    this.loadEvents();
    this.loadLayout();
    this.loadGalleryPhotos();
    this.loadReviews();
  }

  private loadClub(onSuccess?: () => void, onComplete?: () => void): void {
    this.clubService.getManagedClubById(this.clubId).pipe(
      timeout(6000)
    ).subscribe({
      next: (club) => {
        this.club = club;
        this.coverUrl = this.resolveCoverUrl(club);
        onSuccess?.();
        onComplete?.();
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = this.i18n.instant('clubHandler.errors.loadFailed');
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
        this.menuPage = Math.min(this.menuPage, this.menuTotalPages);
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
    this.clubService.getClubPhotos(this.clubId, this.club?.links).subscribe({
      next: (photos) => {
        this.zone.run(() => {
          this.galleryPhotos = (photos ?? []).filter((photo) => !photo.photoType || photo.photoType === 'CAROUSEL');
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.galleryPhotos = [];
          this.cdr.detectChanges();
        });
      }
    });
  }

  private loadTables(): void {
    this.clubService.getTables(this.clubId).subscribe({
      next: (tables) => {
        this.tables = tables ?? [];
        this.tablePage = Math.min(this.tablePage, this.tableTotalPages);
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
        this.eventPage = Math.min(this.eventPage, this.eventTotalPages);
        this.cdr.detectChanges();
      },
      error: () => {
        this.events = [];
        this.cdr.detectChanges();
      }
    });
  }

  private loadReviews(onComplete?: () => void): void {
    this.reviewService.listByClub(this.clubId, this.reviewPage - 1, this.reviewPageSize).subscribe({
      next: ({ reviews, total }) => {
        this.reviews = reviews;
        this.reviewTotalPages = Math.max(1, Math.ceil(total / this.reviewPageSize));
        onComplete?.();
        this.cdr.detectChanges();
      },
      error: () => {
        this.reviews = [];
        this.reviewTotalPages = 1;
        onComplete?.();
        this.cdr.detectChanges();
      }
    });
  }

  private resolveCoverUrl(club: Club): string {
    const cover = club.links?.['cover'];
    if (cover) {
      if (/^https?:\/\//i.test(cover)) {
        return cover;
      }
      return this.clubService.toAbsoluteUrl(cover, club.clubId);
    }

    return this.clubService.getCoverUrl(club.clubId);
  }

  private resolveLayoutUrl(layout: Layout): string {
    const raw = layout?.url || layout?.image || '';
    if (!raw) {
      return '';
    }
    return this.clubService.toAbsoluteUrl(raw, this.clubId);
  }

  private withCacheBust(url: string): string {
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}t=${Date.now()}`;
  }

  private clearActionMessages(): void {
    this.actionErrorMessage = '';
    this.actionSuccessMessage = '';
    if (this.actionSuccessTimeout) {
      clearTimeout(this.actionSuccessTimeout);
      this.actionSuccessTimeout = undefined;
    }
  }

  private showActionSuccess(message: string): void {
    if (this.actionSuccessTimeout) {
      clearTimeout(this.actionSuccessTimeout);
    }

    this.actionSuccessMessage = message;
    this.actionSuccessTimeout = globalThis.setTimeout(() => {
      this.zone.run(() => {
        if (this.actionSuccessMessage === message) {
          this.actionSuccessMessage = '';
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        }
        this.actionSuccessTimeout = undefined;
      });
    }, 3500);
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
    this.menuPage = 1;
    this.cdr.detectChanges();
  }

  private upsertTableInList(table: ClubTable): void {
    if (!table?.tableId) {
      this.loadTables();
      return;
    }

    const normalized: ClubTable = {
      ...table,
      tableState: table.tableState
    };

    const currentIndex = this.tables.findIndex((item) => item.tableId === normalized.tableId);
    if (currentIndex >= 0) {
      this.tables = this.tables.map((item, index) => (index === currentIndex ? normalized : item));
      this.cdr.detectChanges();
      return;
    }

    this.tables = [normalized, ...this.tables];
    this.tablePage = 1;
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
