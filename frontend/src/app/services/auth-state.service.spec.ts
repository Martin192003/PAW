import { TestBed } from '@angular/core/testing';
import { AuthStateService } from './auth-state.service';
import { User } from '../models/user.model';

describe('AuthStateService', () => {
  const user = { userId: 'u1', username: 'tester' } as User;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [AuthStateService] });
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('persists remembered sessions in localStorage and normalizes bearer tokens', () => {
    const service = TestBed.inject(AuthStateService);

    service.setSession('Bearer token-123', user, true);

    expect(service.getToken()).toBe('token-123');
    expect(service.getUser()?.userId).toBe('u1');
    expect(localStorage.getItem('auth_token')).toBe('token-123');
    expect(sessionStorage.getItem('auth_token')).toBeNull();
    expect(service.isLoggedIn()).toBe(true);
  });

  it('stores non-remembered sessions in sessionStorage only', () => {
    const service = TestBed.inject(AuthStateService);

    service.setSession('token-123', user, false);

    expect(sessionStorage.getItem('auth_token')).toBe('token-123');
    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('clears memory, storage and login observable on logout', () => {
    const service = TestBed.inject(AuthStateService);
    const states: boolean[] = [];
    service.isLoggedIn$.subscribe(value => states.push(value));

    service.setSession('token-123', user, true);
    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.getUser()).toBeNull();
    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(states).toEqual([false, true, false]);
  });
});
