import { ɵresetCompiledComponents as resetCompiledComponents } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { ApiErrorService } from '../services/api-error.service';

describe('NotFoundPage', () => {
  let NotFoundPage: typeof import('./not-found.page').NotFoundPage;

  beforeEach(async () => {
    resetCompiledComponents();
    ({ NotFoundPage } = await import('./not-found.page'));

    await TestBed.configureTestingModule({
      imports: [NotFoundPage],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => {
    resetCompiledComponents();
  });

  it('renders the 404 message and home link', () => {
    const fixture = TestBed.createComponent(NotFoundPage);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Pagina no encontrada');
    expect(element.textContent).toContain('La ruta que intentaste visitar no existe o fue movida.');
    expect(element.querySelector('a')?.getAttribute('href')).toBe('/clubs');
  });
});

describe('ForbiddenPage', () => {
  let ForbiddenPage: typeof import('./forbidden.page').ForbiddenPage;

  beforeEach(async () => {
    resetCompiledComponents();
    ({ ForbiddenPage } = await import('./forbidden.page'));

    await TestBed.configureTestingModule({
      imports: [ForbiddenPage],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => {
    resetCompiledComponents();
  });

  it('renders the 403 message and home link', () => {
    const fixture = TestBed.createComponent(ForbiddenPage);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Acceso denegado');
    expect(element.textContent).toContain('No tenes permisos para acceder a este recurso.');
    expect(element.querySelector('a')?.getAttribute('href')).toBe('/clubs');
  });
});

describe('ErrorPage', () => {
  let ErrorPage: typeof import('./error.page').ErrorPage;
  let apiErrors: {
    getSnapshot: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    resetCompiledComponents();
    ({ ErrorPage } = await import('./error.page'));

    apiErrors = {
      getSnapshot: vi.fn().mockReturnValue({ status: 500, message: 'Server failed' }),
      clear: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ErrorPage],
      providers: [
        provideRouter([]),
        { provide: ApiErrorService, useValue: apiErrors },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    resetCompiledComponents();
  });

  it('renders the captured API error and clears it', () => {
    const fixture = TestBed.createComponent(ErrorPage);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(apiErrors.getSnapshot).toHaveBeenCalled();
    expect(apiErrors.clear).toHaveBeenCalled();
    expect(element.textContent).toContain('Ocurrio un problema');
    expect(element.textContent).toContain('Server failed');
    expect(element.textContent).toContain('Codigo: 500');
  });

  it('renders the fallback error message when there is no captured API error', () => {
    apiErrors.getSnapshot.mockReturnValueOnce(null);

    const fixture = TestBed.createComponent(ErrorPage);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Ocurrio un error inesperado.');
    expect(element.textContent).not.toContain('Codigo:');
  });
});
