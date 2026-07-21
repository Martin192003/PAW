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

  it('updateUser replaces the stored user without touching the token', () => {
    const service = TestBed.inject(AuthStateService);
    service.setSession('token-123', user, true);

    service.updateUser({ userId: 'u2', username: 'renamed' } as User);

    expect(service.getUser()?.userId).toBe('u2');
    expect(service.getToken()).toBe('token-123');
    expect(JSON.parse(localStorage.getItem('auth_user')!).username).toBe('renamed');
  });

  it('updateToken replaces and normalizes the stored token', () => {
    const service = TestBed.inject(AuthStateService);
    service.setSession('token-123', user, true);

    service.updateToken('Bearer token-999');

    expect(service.getToken()).toBe('token-999');
    expect(localStorage.getItem('auth_token')).toBe('token-999');
  });

  it('restores and normalizes a quoted session persisted in storage', () => {
    localStorage.setItem('auth_token', '"token-xyz"');
    localStorage.setItem('auth_user', JSON.stringify(user));

    const service = TestBed.inject(AuthStateService);

    expect(service.getToken()).toBe('token-xyz');
    expect(service.getUser()?.userId).toBe('u1');
    expect(service.isLoggedIn()).toBe(true);
  });
});
