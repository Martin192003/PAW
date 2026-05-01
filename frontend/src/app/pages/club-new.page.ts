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
import { ClubCreateRequest } from '../models/club.model';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';

declare const L: any; // Leaflet (cargado vía CDN o instalado como paquete)

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
  ) {}

  // ----------------------------------------------------------------
  // Lifecycle
  // ----------------------------------------------------------------
  ngOnInit(): void {
    this.clubForm = this.fb.group({
      name:        ['', [Validators.required, Validators.maxLength(100)]],
      location:    ['', [Validators.required]],
      priceRange:  ['', [Validators.required]],
      description: [''],
      musicGenre:  ['', [Validators.required]],
      zone:        ['', [Validators.required]],
      isBar:       ['', [Validators.required]],
      cbu:         [''],
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
    const ctrl = this.clubForm.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  getError(field: string): string {
    const ctrl = this.clubForm.get(field);
    if (!ctrl || !ctrl.errors) return '';
    if (ctrl.errors['required'])   return 'Este campo es requerido.';
    if (ctrl.errors['maxlength'])  return `Máximo ${ctrl.errors['maxlength'].requiredLength} caracteres.`;
    if (ctrl.errors['serverError']) return ctrl.errors['serverError'];
    return 'Campo inválido.';
  }

  fieldClass(field: string): object {
    return {
      'border-[#dc3545]': this.showError(field),
      'border-[#ccc]':    !this.showError(field),
    };
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
    const btn = document.querySelector('[aria-label="Información rango de precio"]');
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
    this.clubForm.markAllAsTouched();
    if (this.clubForm.invalid || this.submitting) return;

    this.submitting    = true;
    this.errorMessage  = '';

    const val = this.clubForm.value;

    const payload: ClubCreateRequest = {
      clubName: val.name,
      location: val.location,
      priceRange: val.priceRange,
      description: val.description ?? '',
      musicGenre: val.musicGenre,
      zone: val.zone,
      bar: val.isBar === 'true',
      cbu: val.cbu ?? '',
    };

    this.clubService.createClub(payload).pipe(
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
          this.errorMessage = payload?.message ?? 'Ocurrió un error al guardar el club.';
        }
      },
    });
  }

  // Aplica errores de validación que vienen del servidor (campo → mensaje)
  private applyServerErrors(fieldErrors: Record<string, string>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      const ctrl = this.clubForm.get(field);
      if (ctrl) {
        ctrl.setErrors({ serverError: message });
        ctrl.markAsTouched();
      } else {
        this.errorMessage = message;
      }
    }
  }
}
