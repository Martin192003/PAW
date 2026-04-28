import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly tokenKey = 'auth_token';
  private readonly userKey = 'auth_user';
  private readonly browserStorageAvailable =
    typeof window !== 'undefined' &&
    AuthStateService.isStorageAvailable(typeof localStorage === 'undefined' ? undefined : localStorage) &&
    AuthStateService.isStorageAvailable(typeof sessionStorage === 'undefined' ? undefined : sessionStorage);
  private readonly loggedInSubject = new BehaviorSubject<boolean>(this.hasStoredSession());

  get isLoggedIn$(): Observable<boolean> {
    return this.loggedInSubject.asObservable();
  }

  setSession(token: string, user: User, rememberMe: boolean): void {
    if (!this.browserStorageAvailable) {
      this.loggedInSubject.next(false);
      return;
    }

    const storage = rememberMe ? localStorage : sessionStorage; //dependiendo de si se
    const otherStorage = rememberMe ? sessionStorage : localStorage; // hace click en recordar

    otherStorage.removeItem(this.tokenKey);
    otherStorage.removeItem(this.userKey);

    const normalizedToken = this.normalizeToken(token);
    if (!normalizedToken) {
      this.loggedInSubject.next(false);
      return;
    }

    storage.setItem(this.tokenKey, normalizedToken);
    storage.setItem(this.userKey, JSON.stringify(user));

    this.loggedInSubject.next(true);
  }

  getToken(): string | null {
    if (!this.browserStorageAvailable) {
      return null;
    }

    const rawToken = localStorage.getItem(this.tokenKey) ?? sessionStorage.getItem(this.tokenKey);
    return this.normalizeToken(rawToken);
  }

  getUser(): User | null {
    if (!this.browserStorageAvailable) {
      return null;
    }

    const rawUser = localStorage.getItem(this.userKey) ?? sessionStorage.getItem(this.userKey);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as User;
    } catch {
      return null;
    }
  }

  updateUser(user: User): void {
    if (!this.browserStorageAvailable) {
      return;
    }

    const storage = this.getActiveStorage();
    if (!storage) {
      return;
    }

    storage.setItem(this.userKey, JSON.stringify(user));
  }

  updateToken(token: string): void {
    if (!this.browserStorageAvailable || !token) {
      return;
    }

    const storage = this.getActiveStorage();
    if (!storage) {
      return;
    }

    const normalizedToken = this.normalizeToken(token);
    if (!normalizedToken) {
      return;
    }

    storage.setItem(this.tokenKey, normalizedToken);
  }

  private normalizeToken(token: string | null): string | null {
    if (!token) {
      return null;
    }

    let normalized = token.trim();
    if (!normalized) {
      return null;
    }

    if (normalized.startsWith('Bearer ')) {
      normalized = normalized.substring(7).trim();
    }

    if (normalized.startsWith('"') && normalized.endsWith('"') && normalized.length > 1) {
      normalized = normalized.slice(1, -1).trim();
    }

    if (!normalized || normalized === 'null' || normalized === 'undefined') {
      return null;
    }

    return normalized;
  }

  private getActiveStorage(): Storage | null {
    const localHasSession = localStorage.getItem(this.tokenKey) || localStorage.getItem(this.userKey);
    if (localHasSession) {
      return localStorage;
    }

    const sessionHasSession = sessionStorage.getItem(this.tokenKey) || sessionStorage.getItem(this.userKey);
    if (sessionHasSession) {
      return sessionStorage;
    }

    return null;
  }

  private hasStoredSession(): boolean {
    return !!this.getToken();
  }

  private static isStorageAvailable(storage: Storage | undefined): storage is Storage {
    return !!storage &&
      typeof storage.getItem === 'function' &&
      typeof storage.setItem === 'function' &&
      typeof storage.removeItem === 'function';
  }
}

