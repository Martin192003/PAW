import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly tokenKey = 'auth_token';
  private readonly userKey = 'auth_user';
  private memoryToken: string | null = null;
  private memoryUser: User | null = null;
  private readonly loggedInSubject = new BehaviorSubject<boolean>(false);

  constructor() {
    this.memoryToken = this.readStoredToken();
    this.memoryUser = this.readStoredUser();
    this.loggedInSubject.next(!!this.memoryToken);
  }

  get isLoggedIn$(): Observable<boolean> {
    return this.loggedInSubject.asObservable();
  }

  setSession(token: string, user: User, rememberMe: boolean): void {
    const normalizedToken = this.normalizeToken(token);
    if (!normalizedToken) {
      this.clearMemorySession();
      this.clearStoredSession();
      this.loggedInSubject.next(false);
      return;
    }

    this.memoryToken = normalizedToken;
    this.memoryUser = user;
    this.loggedInSubject.next(true);

    this.persistSession(normalizedToken, user, rememberMe);
  }

  getToken(): string | null {
    return this.normalizeToken(this.memoryToken ?? this.readStoredToken());
  }

  getUser(): User | null {
    if (this.memoryUser) {
      return this.memoryUser;
    }

    return this.readStoredUser();
  }

  updateUser(user: User): void {
    this.memoryUser = user;
    this.persistUser(user);
  }

  updateToken(token: string): void {
    const normalizedToken = this.normalizeToken(token);
    if (!normalizedToken) {
      return;
    }

    this.memoryToken = normalizedToken;
    this.persistToken(normalizedToken);
  }

  /**
   * Cierra la sesión: limpia token y usuario de ambos storages y notifica a los suscriptores.
   * Llamar desde el interceptor al recibir 401 con token expirado o inválido.
   */
  logout(): void {
    this.clearMemorySession();
    this.clearStoredSession();
    this.loggedInSubject.next(false);
  }

  isLoggedIn(): boolean {
    return this.loggedInSubject.getValue();
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

  private persistSession(token: string, user: User, rememberMe: boolean): void {
    const primary = rememberMe ? this.safeStorage(this.getLocalStorage()) : this.safeStorage(this.getSessionStorage());
    const secondary = rememberMe ? this.safeStorage(this.getSessionStorage()) : this.safeStorage(this.getLocalStorage());

    this.clearStorage(secondary);

    if (!primary) {
      return;
    }

    try {
      primary.setItem(this.tokenKey, token);
      primary.setItem(this.userKey, JSON.stringify(user));
    } catch {
      // Si el storage del navegador falla, mantenemos la sesión en memoria.
    }
  }

  private persistUser(user: User): void {
    const storage = this.getPreferredStorage();
    if (!storage) {
      return;
    }

    try {
      storage.setItem(this.userKey, JSON.stringify(user));
    } catch {
      // fallback silencioso a memoria
    }
  }

  private persistToken(token: string): void {
    const storage = this.getPreferredStorage();
    if (!storage) {
      return;
    }

    try {
      storage.setItem(this.tokenKey, token);
    } catch {
      // fallback silencioso a memoria
    }
  }

  private readStoredToken(): string | null {
    const local = this.safeStorage(this.getLocalStorage())?.getItem(this.tokenKey);
    const session = this.safeStorage(this.getSessionStorage())?.getItem(this.tokenKey);
    return this.normalizeToken((local ?? session) ?? null);
  }

  private readStoredUser(): User | null {
    const rawUser = this.safeStorage(this.getLocalStorage())?.getItem(this.userKey) ?? this.safeStorage(this.getSessionStorage())?.getItem(this.userKey);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as User;
    } catch {
      return null;
    }
  }

  private getPreferredStorage(): Storage | null {
    const local = this.safeStorage(this.getLocalStorage());
    const session = this.safeStorage(this.getSessionStorage());

    if (local?.getItem(this.tokenKey) || local?.getItem(this.userKey)) {
      return local;
    }

    if (session?.getItem(this.tokenKey) || session?.getItem(this.userKey)) {
      return session;
    }

    return local ?? session;
  }

  private clearStoredSession(): void {
    this.clearStorage(this.safeStorage(this.getLocalStorage()));
    this.clearStorage(this.safeStorage(this.getSessionStorage()));
  }

  private clearMemorySession(): void {
    this.memoryToken = null;
    this.memoryUser = null;
  }

  private clearStorage(storage: Storage | null): void {
    if (!storage) {
      return;
    }

    try {
      storage.removeItem(this.tokenKey);
      storage.removeItem(this.userKey);
    } catch {
      // ignore
    }
  }

  private safeStorage(storage: Storage | null | undefined): Storage | null {
    if (!AuthStateService.isStorageAvailable(storage ?? undefined)) {
      return null;
    }

    return storage ?? null;
  }

  private getLocalStorage(): Storage | undefined {
    return typeof window === 'undefined' ? undefined : window.localStorage;
  }

  private getSessionStorage(): Storage | undefined {
    return typeof window === 'undefined' ? undefined : window.sessionStorage;
  }

  private static isStorageAvailable(storage: Storage | undefined): storage is Storage {
    return !!storage &&
      typeof storage.getItem === 'function' &&
      typeof storage.setItem === 'function' &&
      typeof storage.removeItem === 'function';
  }
}

