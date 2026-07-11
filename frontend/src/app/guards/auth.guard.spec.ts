import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { vi } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthStateService } from '../services/auth-state.service';

describe('authGuard', () => {
  let authState: { getToken: ReturnType<typeof vi.fn>; getUser: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authState = { getToken: vi.fn(), getUser: vi.fn() };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthStateService, useValue: authState as unknown as AuthStateService },
        { provide: Router, useValue: router as unknown as Router },
      ],
    });
  });

  function runGuard(url = '/clubs/new'): boolean {
    const state = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, state)
    ) as boolean;
  }

  it('allows navigation when there is a valid session', () => {
    authState.getToken.mockReturnValue('token-123');
    authState.getUser.mockReturnValue({ userId: 'user-1' });

    expect(runGuard()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects to login preserving returnUrl when there is no token', () => {
    authState.getToken.mockReturnValue(null);
    authState.getUser.mockReturnValue(null);

    expect(runGuard('/clubs/new')).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'auth', returnUrl: '/clubs/new' },
    });
  });

  it('redirects to login when there is a token but no stored user', () => {
    authState.getToken.mockReturnValue('token-123');
    authState.getUser.mockReturnValue(null);

    expect(runGuard('/clubs/new')).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'auth', returnUrl: '/clubs/new' },
    });
  });
});
