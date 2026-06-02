import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthStateService } from './auth-state.service';

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

  it('adds bearer token to absolute local backend api links', () => {
    authState.getToken.mockReturnValue('token-123');

    const url = 'http://localhost:8080/webapp_war_exploded/api/clubs/club-id/bookings/booking-id/receipt';
    http.put(url, new FormData()).subscribe();

    const apiReq = httpMock.expectOne(url);
    expect(apiReq.request.headers.get('Authorization')).toBe('Bearer token-123');
    apiReq.flush({ ok: true });
  });

  it('skips authorization header when token is missing', () => {
    authState.getToken.mockReturnValue('');

    http.get('/api/secure').subscribe();
    const apiReq = httpMock.expectOne('/api/secure');
    expect(apiReq.request.headers.has('Authorization')).toBe(false);
    apiReq.flush({ ok: true });
  });
});
