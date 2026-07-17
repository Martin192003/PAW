import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BehaviorSubject, firstValueFrom, of } from 'rxjs';
import { vi } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';
import { ClubsPage } from './pages/clubs.page';
import { ApiErrorService } from './services/api-error.service';
import { apiErrorInterceptor } from './services/api-error.interceptor';
import { AuthBootstrapService } from './services/auth-bootstrap.service';
import { AuthStateService } from './services/auth-state.service';
import { authInterceptor } from './services/auth.interceptor';
import { ClubService } from './services/club.service';
import { EventService } from './services/event.service';
import { I18nService } from './services/i18n.service';
import { UserService } from './services/user.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: {} },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the application shell', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('main')).toBeTruthy();
  });

  it('hides the topbar on password reset routes', () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'url', 'get').mockReturnValue('/reset-password?token=abc');

    const fixture = TestBed.createComponent(App);

    expect(fixture.componentInstance.showTopbar).toBe(false);
  });
});

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authState: { clearSession: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn>; url: string };

  beforeEach(() => {
    authState = { clearSession: vi.fn() };
    router = { navigate: vi.fn().mockResolvedValue(true), url: '/bookings/club-id/booking-id' };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthStateService, useValue: authState as unknown as AuthStateService },
        { provide: Router, useValue: router as unknown as Router },
        ApiErrorService,
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('logs out and redirects on 401 for api requests', () => {
    http.get('/api/secure').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/secure');
    req.flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authState.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'expired', returnUrl: '/bookings/club-id/booking-id' },
    });
  });
});

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authState: { getToken: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authState = { getToken: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthStateService, useValue: authState as unknown as AuthStateService },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds bearer token for api requests only', () => {
    authState.getToken.mockReturnValue('token-123');

    http.get('/api/secure').subscribe();
    const apiReq = httpMock.expectOne('/api/secure');
    expect(apiReq.request.headers.get('Authorization')).toBe('Bearer token-123');
    apiReq.flush({ ok: true });

    http.get('/assets/logo.png').subscribe();
    const assetReq = httpMock.expectOne('/assets/logo.png');
    expect(assetReq.request.headers.has('Authorization')).toBe(false);
    assetReq.flush('');
  });
});

describe('AuthBootstrapService', () => {
  let service: AuthBootstrapService;
  let authState: {
    getToken: ReturnType<typeof vi.fn>;
    getUser: ReturnType<typeof vi.fn>;
    updateUser: ReturnType<typeof vi.fn>;
    clearSession: ReturnType<typeof vi.fn>;
  };
  let userService: { getCurrentUserById: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authState = {
      getToken: vi.fn(),
      getUser: vi.fn(),
      updateUser: vi.fn(),
      clearSession: vi.fn(),
    };
    userService = { getCurrentUserById: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthBootstrapService,
        { provide: AuthStateService, useValue: authState as unknown as AuthStateService },
        { provide: UserService, useValue: userService as unknown as UserService },
      ],
    });

    service = TestBed.inject(AuthBootstrapService);
  });

  it('loads the stored user when token exists', async () => {
    authState.getToken.mockReturnValue('token-123');
    authState.getUser.mockReturnValue({ userId: 'u1' });
    userService.getCurrentUserById.mockReturnValue(of({ userId: 'u1' } as any));

    await firstValueFrom(service.initialize());

    expect(userService.getCurrentUserById).toHaveBeenCalledWith('u1');
    expect(authState.updateUser).toHaveBeenCalledWith(expect.objectContaining({ userId: 'u1' }));
  });

  it('skips bootstrap when there is no token', async () => {
    authState.getToken.mockReturnValue(null);

    await firstValueFrom(service.initialize());

    expect(userService.getCurrentUserById).not.toHaveBeenCalled();
  });
});

describe('ClubService HATEOAS', () => {
  let service: ClubService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClubService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ClubService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('uses HATEOAS link when available', () => {
    service.getLayout('club-1', { layout: '/api/clubs/club-1/layout' }).subscribe();

    const req = httpMock.expectOne('/api/clubs/club-1/layout');
    req.flush({});
  });

  it('falls back to the canonical endpoint when links are absent', () => {
    service.getLayout('club-1').subscribe();

    const req = httpMock.expectOne((request) => request.url.endsWith('/clubs/club-1/layout'));
    req.flush({});
  });

  it('fails explicitly when links are present but the rel is missing', async () => {
    await expect(firstValueFrom(service.getLayout('club-1', {}))).rejects.toBeTruthy();
  });
});

class ActivatedRouteStub {
  private currentParamMap = convertToParamMap({});
  private readonly subject = new BehaviorSubject(this.currentParamMap);
  readonly queryParamMap = this.subject.asObservable();

  get snapshot(): { queryParamMap: ReturnType<typeof convertToParamMap> } {
    return { queryParamMap: this.currentParamMap };
  }

  setQueryParams(params: Record<string, string>): void {
    this.currentParamMap = convertToParamMap(params);
    this.subject.next(this.currentParamMap);
  }
}

describe('ClubsPage query params', () => {
  let route: ActivatedRouteStub;
  let clubService: { getClubs: ReturnType<typeof vi.fn> };
  let eventService: { getUpcomingEvents: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    route = new ActivatedRouteStub();
    clubService = { getClubs: vi.fn() };
    eventService = { getUpcomingEvents: vi.fn() };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    clubService.getClubs.mockReturnValue(
      of({ content: [], totalPages: 1, totalElements: 0, number: 0 } as any)
    );
    eventService.getUpcomingEvents.mockReturnValue(
      of({ content: [], totalPages: 1, totalElements: 0, number: 0 } as any)
    );

    TestBed.configureTestingModule({
      imports: [ClubsPage, TranslateModule.forRoot()],
      providers: [
        { provide: ActivatedRoute, useValue: route },
        { provide: ClubService, useValue: clubService as unknown as ClubService },
        { provide: EventService, useValue: eventService as unknown as EventService },
        { provide: Router, useValue: router as unknown as Router },
      ],
    });
  });

  it('loads clubs based on query params', () => {
    const fixture = TestBed.createComponent(ClubsPage);
    route.setQueryParams({
      page: '2',
      size: '20',
      name: 'Rock',
      zone: 'ZONA_NORTE',
    });

    fixture.detectChanges();

    expect(clubService.getClubs).toHaveBeenCalledWith(
      2,
      20,
      expect.objectContaining({ name: 'Rock', zone: 'ZONA_NORTE' })
    );
  });

  it('updates query params when applying filters', () => {
    const fixture = TestBed.createComponent(ClubsPage);
    const component = fixture.componentInstance;

    component.filters = {
      name: 'Club',
      zone: '',
      genre: '',
      price: '',
      isBar: '',
      sort: '',
    };
    component.pageSize = 15;

    component.applyFilters();

    expect(router.navigate).toHaveBeenCalled();
    const args = (router.navigate.mock.calls[0]?.[1] ?? {}) as any;
    expect(args.queryParams).toEqual(expect.objectContaining({ page: 1, size: 15, name: 'Club' }));
  });
});

describe('routes', () => {
  it('protects club management with adminGuard', () => {
    const route = routes.find(candidate => candidate.path === 'clubs/handle');
    expect(route?.canActivate).toContain(adminGuard);
  });

  it('protects profile routes with authGuard', () => {
    const profileRoute = routes.find(candidate => candidate.path === 'profile');
    const reviewsRoute = routes.find(candidate => candidate.path === 'profile/reviews');

    expect(profileRoute?.canActivate).toContain(authGuard);
    expect(reviewsRoute?.canActivate).toContain(authGuard);
  });

  it('includes wildcard not-found route', () => {
    const hasWildcard = routes.some(route => route.path === '**' && route.redirectTo === 'not-found');
    expect(hasWildcard).toBe(true);
  });
});
