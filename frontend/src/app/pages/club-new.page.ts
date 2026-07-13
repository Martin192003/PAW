import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ChangeDetectorRef,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ClubService } from '../services/club.service';
import { ClubPayload } from '../models/club.model';
import { HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { catchError, finalize, map, switchMap } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

declare const L: any; // Leaflet (cargado vía CDN o instalado como paquete)

const CBU_MAX_LENGTH = 50;
// Espeja DESCRIPTION_MAX_LENGTH en ClubServiceImpl (backend): la columna es
// TEXT (sin limite de longitud), asi que este tope es una regla de negocio,
// no algo impuesto por el schema.
const DESCRIPTION_MAX_LENGTH = 1000;

@Component({
  selector: 'app-add-club',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './club-new.page.html',
  styleUrl: './club-new.page.css',
})
export class AddClubComponent implements OnInit, AfterViewInit, OnDestroy {

  // ----------------------------------------------------------------
  // Estado del formulario
  // ----------------------------------------------------------------
  clubForm!: FormGroup;
  selectedImage: File | null = null;
  submitting = false;
  errorMessage = '';

  // Igual que en register.page.ts: los errores de campo no se muestran en
  // vivo mientras el usuario completa el form, solo despues de un intento de
  // submit. El estado se "congela" en cada intento para que no aparezcan ni
  // desaparezcan solos mientras se sigue escribiendo, hasta el proximo submit.
  submittedAttempted = false;
  frozenErrors: { [key: string]: boolean } = {};
  frozenErrorMessages: { [key: string]: string } = {};

  private static readonly FIELDS = [
    'name', 'location', 'priceRange', 'description', 'musicGenre', 'zone', 'isBar', 'cbu'
  ];

  // ----------------------------------------------------------------
  // Tooltip precio
  // ----------------------------------------------------------------
  tooltipVisible = false;

  // ----------------------------------------------------------------
  // Mapa Leaflet
  // ----------------------------------------------------------------
  mapVisible = false;
  private map: any = null;
  private marker: any = null;
  private lat = -34.6037037;
  private lng = -58.3841771;
  private typingTimer: any;
  private readonly SEARCH_DELAY = 600;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private clubService: ClubService,
    private i18n: I18nService,
  ) {}

  // ----------------------------------------------------------------
  // Lifecycle
  // ----------------------------------------------------------------
  ngOnInit(): void {
    this.clubForm = this.fb.group({
      name:        ['', [Validators.required, Validators.maxLength(100)]],
      location:    ['', [Validators.required]],
      priceRange:  ['', [Validators.required]],
      description: ['', [Validators.required, Validators.maxLength(DESCRIPTION_MAX_LENGTH)]],
      musicGenre:  ['', [Validators.required]],
      zone:        ['', [Validators.required]],
      isBar:       ['', [Validators.required]],
      cbu:         ['', [Validators.required, Validators.maxLength(CBU_MAX_LENGTH)]],
      image:       [null],
    });

    // Escuchar cambios en location para el mapa
    this.clubForm.get('location')!.valueChanges.subscribe((val: string) => {
      this.handleLocationChange(val ?? '');
    });

    // Cerrar tooltip al hacer click fuera
    document.addEventListener('click', this.closeTooltip);
  }

  ngAfterViewInit(): void {
    // Si al cargar ya hay un valor en location (edición futura), mostrar mapa
    const loc = this.clubForm.get('location')!.value;
    if (loc?.trim().length > 0) {
      this.showMap();
    }
  }

  ngOnDestroy(): void {
    clearTimeout(this.typingTimer);
    document.removeEventListener('click', this.closeTooltip);
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  // ----------------------------------------------------------------
  // Helpers de validación (usados en el template)
  // ----------------------------------------------------------------
  showError(field: string): boolean {
    if (!this.submittedAttempted) return false;

    if (this.frozenErrors[field] !== undefined) {
      return this.frozenErrors[field];
    }

    return this.hasCurrentError(field);
  }

  getError(field: string): string {
    if (this.frozenErrorMessages[field]) {
      return this.frozenErrorMessages[field];
    }

    return this.currentErrorMessage(field);
  }

  fieldClass(field: string): object {
    return {
      'border-[#dc3545]': this.showError(field),
      'border-[#ccc]':    !this.showError(field),
    };
  }

  private hasCurrentError(field: string): boolean {
    const ctrl = this.clubForm.get(field);
    return !!(ctrl && ctrl.invalid);
  }

  private currentErrorMessage(field: string): string {
    const ctrl = this.clubForm.get(field);
    if (!ctrl || !ctrl.errors) return '';
    if (ctrl.errors['required'])    return this.i18n.instant('clubNew.errors.required');
    if (ctrl.errors['maxlength'])   return this.i18n.instant('clubNew.errors.maxlength', { max: ctrl.errors['maxlength'].requiredLength });
    if (ctrl.errors['serverError']) return ctrl.errors['serverError'];
    return this.i18n.instant('clubNew.errors.invalid');
  }

  // ----------------------------------------------------------------
  // Archivo imagen
  // ----------------------------------------------------------------
  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedImage = input.files[0];
    }
  }

  // ----------------------------------------------------------------
  // Tooltip precio
  // ----------------------------------------------------------------
  toggleTooltip(event: MouseEvent): void {
    event.stopPropagation();
    this.tooltipVisible = !this.tooltipVisible;
  }

  private closeTooltip = (e: MouseEvent): void => {
    const btn = document.querySelector('[data-price-tooltip-trigger]');
    if (btn && !btn.contains(e.target as Node)) {
      this.tooltipVisible = false;
      this.cdr.detectChanges();
    }
  };

  // ----------------------------------------------------------------
  // Mapa Leaflet
  // ----------------------------------------------------------------
  private handleLocationChange(address: string): void {
    if (address.trim().length > 0) {
      this.showMap();
    } else {
      this.mapVisible = false;
    }

    clearTimeout(this.typingTimer);
    if (address.trim().length >= 3) {
      this.typingTimer = setTimeout(() => this.updateMap(address), this.SEARCH_DELAY);
    }
  }

  private showMap(): void {
    if (this.mapVisible) return;
    this.mapVisible = true;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.initializeMap();
    }, 50); // esperar que el DOM aplique la transición
  }

  private initializeMap(): void {
    if (this.map) {
      setTimeout(() => this.map.invalidateSize(true), 650);
      return;
    }

    this.map = L.map('locationMap', { zoomControl: true }).setView([this.lat, this.lng], 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    this.marker = L.marker([this.lat, this.lng]).addTo(this.map);

    setTimeout(() => {
      this.map.invalidateSize(true);
      const loc = this.clubForm.get('location')!.value;
      if (loc?.trim().length > 0) {
        this.updateMap(loc.trim());
      }
    }, 650);
  }

  private async updateMap(address: string): Promise<void> {
    if (!address || address.trim().length < 3) return;
    try {
      const res     = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}`);
      const results = await res.json();
      if (results && results.length > 0) {
        this.lat = parseFloat(results[0].lat);
        this.lng = parseFloat(results[0].lon);
        if (this.marker) {
          this.marker.setLatLng([this.lat, this.lng]);
        } else {
          this.marker = L.marker([this.lat, this.lng]).addTo(this.map);
        }
        this.map.setView([this.lat, this.lng], 15);
      }
    } catch (e) {
      console.error('Error geocodificando dirección:', e);
    }
  }

  // ----------------------------------------------------------------
  // Submit
  // ----------------------------------------------------------------
  onSubmit(): void {
    this.submittedAttempted = true;
    this.clubForm.markAllAsTouched();

    // Descarta cualquier serverError de un intento anterior antes de
    // recalcular: si el campo ya es valido segun sus validadores propios
    // (required/maxlength), updateValueAndValidity limpia el error manual.
    AddClubComponent.FIELDS.forEach(field => {
      this.clubForm.get(field)?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
    });

    this.frozenErrors = {};
    this.frozenErrorMessages = {};
    AddClubComponent.FIELDS.forEach(field => {
      const hasError = this.hasCurrentError(field);
      this.frozenErrors[field] = hasError;
      if (hasError) {
        this.frozenErrorMessages[field] = this.currentErrorMessage(field);
      }
    });

    if (this.clubForm.invalid || this.submitting) return;

    this.submitting    = true;
    this.errorMessage  = '';

    const val = this.clubForm.value;

    const payload: ClubPayload = {
      clubName: val.name,
      location: val.location,
      priceRange: val.priceRange,
      description: val.description?.trim() ?? '',
      musicGenre: val.musicGenre,
      zone: val.zone,
      bar: val.isBar === 'true',
      cbu: val.cbu?.trim() ?? '',
    };

    this.clubService.createClub(payload).pipe(
      // El endpoint de creación es JSON puro (no multipart): el club se crea
      // primero y recién con su id/links ya asignados se puede subir la
      // portada a POST /clubs/{id}/cover (mismo endpoint y mismo método de
      // servicio que usa club-handler.page.ts para cambiar la portada).
      switchMap((club) => {
        const image = this.selectedImage;
        if (!image) {
          return of(club);
        }

        return this.clubService.uploadCover(club.clubId, image, club.links).pipe(
          map(() => club),
          catchError((err) => {
            // El club ya fue creado exitosamente; un club sin portada es un
            // estado válido en el resto de la app (listado y detalle ya
            // manejan la ausencia de cover), así que no revertimos ni
            // bloqueamos la navegación por un fallo puntual de la subida.
            // El usuario puede volver a subirla luego desde la edición del club.
            console.error('No se pudo subir la portada del club recién creado:', err);
            return of(club);
          })
        );
      }),
      finalize(() => {
        this.submitting = false;
      })
    ).subscribe({
      next: () => {
        this.router.navigate(['/clubs']);
      },
      error: (err: HttpErrorResponse) => {
        const payload = err.error as { fieldErrors?: Record<string, string>; message?: string } | undefined;
        if (payload?.fieldErrors) {
          this.applyServerErrors(payload.fieldErrors);
        } else {
          this.errorMessage = payload?.message ?? this.i18n.instant('clubNew.errors.saveFailed');
        }
      },
    });
  }

  // Aplica errores de validación que vienen del servidor (campo → mensaje).
  // Estos llegan despues del submit (async), asi que hay que actualizar
  // tambien el estado "congelado" para que se muestren de inmediato, sin
  // esperar a un proximo intento de submit.
  private applyServerErrors(fieldErrors: Record<string, string>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      const ctrl = this.clubForm.get(field);
      if (ctrl) {
        ctrl.setErrors({ serverError: message });
        ctrl.markAsTouched();
        this.frozenErrors[field] = true;
        this.frozenErrorMessages[field] = message;
      } else {
        this.errorMessage = message;
      }
    }
  }
}
