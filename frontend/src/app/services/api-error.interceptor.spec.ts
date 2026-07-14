import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { apiErrorInterceptor } from './api-error.interceptor';
import { AuthStateService } from './auth-state.service';
import { ApiErrorService } from './api-error.service';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authState: { clearSession: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let apiErrors: ApiErrorService;

  beforeEach(() => {
	authState = { clearSession: vi.fn() };
	router = { navigate: vi.fn().mockResolvedValue(true) };

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
	apiErrors = TestBed.inject(ApiErrorService);
  });

  afterEach(() => {
	httpMock.verify();
  });

  it('logs out and redirects on 401 for api requests', () => {
	http.get('/api/secure').subscribe({ error: () => undefined });

	const req = httpMock.expectOne('/api/secure');
	req.flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

	expect(authState.clearSession).toHaveBeenCalled();
	expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { reason: 'expired' } });
  });

  it('uses the generic message instead of rendering html error bodies', () => {
	http.get('/api/bookings').subscribe({ error: () => undefined });

	const req = httpMock.expectOne('/api/bookings');
	req.flush('<!doctype html><html><body>HTTP Status 500</body></html>', {
	  status: 500,
	  statusText: 'Internal Server Error',
	});

	expect(apiErrors.getSnapshot()).toEqual({
	  status: 500,
	  message: 'Ocurrio un error inesperado.',
	});
	expect(router.navigate).toHaveBeenCalledWith(['/error']);
  });
});
