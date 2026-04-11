import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly tokenKey = 'auth_token';
  private readonly userKey = 'auth_user';
  private readonly browserStorageAvailable = typeof window !== 'undefined' && typeof localStorage !== 'undefined';
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

    storage.setItem(this.tokenKey, token);
    storage.setItem(this.userKey, JSON.stringify(user));

    this.loggedInSubject.next(true);
  }

  getToken(): string | null {
    if (!this.browserStorageAvailable) {
      return null;
    }

    return localStorage.getItem(this.tokenKey) ?? sessionStorage.getItem(this.tokenKey);
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

    storage.setItem(this.tokenKey, token);
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
}


