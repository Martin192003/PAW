import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, switchMap, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ApiMessageResponse,
  LoginResponse,
  ResendVerificationRequest,
  UserCreateRequest,
  UserLoginRequest,
  User,
} from '../models/user.model';
import { SKIP_API_ERROR_REDIRECT } from './api-error.interceptor';

type UserUpdatePayload = {
  username: string;
  name: string;
  surname: string;
  email: string;
  cellphone: string;
  locale: string;
  password?: string;
};

export type UserUpdateResult = {
  user: User;
  token: string | null;
};

type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    href: string;
    userId?: string;
    username?: string;
    name?: string;
    surname?: string;
    email?: string;
    cellphone?: string;
    role?: string;
    verified?: boolean;
    createdAt?: string;
    locale?: string;
  };
};

type UserUpdateResponse = {
  user: User;
  accessToken?: string;
  tokenType?: string;
  expiresIn?: number;
};

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  getUserById(userId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${userId}`);
  }

  register(payload: UserCreateRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/users`, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    });
  }

  updateUser(userId: string, payload: UserUpdatePayload): Observable<UserUpdateResult> {
    return this.http.put<UserUpdateResponse>(`${this.apiUrl}/users/${userId}`, payload).pipe(
      map((response) => ({
        user: response.user,
        token: response.accessToken ?? null
      }))
    );
  }

  login(payload: UserLoginRequest): Observable<LoginResponse> {
    return this.http.post<AuthTokenResponse>(`${this.apiUrl}/auth/tokens`, {
      email: payload.usernameOrEmail.trim(),
      password: payload.password,
    }, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      switchMap((tokenResponse) => {
        const token = tokenResponse.accessToken;
        const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
        return this.http.get<User>(tokenResponse.user.href, { headers }).pipe(
          map((user) => ({ token, user }))
        );
      })
    );
  }

  /**
   * GET /auth/verifications?token=... (link del mail).
   *
   * El backend puede responder de dos formas: un AuthTokenResponse (cuenta
   * recien verificada, login automatico) o un ApiMessageResponse (ya estaba
   * verificada, o el backend todavia no emite token). Igual que login() y
   * resetPassword(), si viene un token hay que resolver el usuario completo
   * antes de devolver un LoginResponse utilizable por la pagina.
   */
  verifyByToken(token: string): Observable<LoginResponse | ApiMessageResponse> {
    const params = new HttpParams().set('token', token);
    return this.http
      .get<AuthTokenResponse | ApiMessageResponse>(`${this.apiUrl}/auth/verifications`, {
        params,
        context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
        // Esta llamada tiene efecto (verifica la cuenta y hace login automatico),
        // asi que nunca debe resolverse desde la cache de transferencia SSR->cliente:
        // siempre tiene que ser un pedido real hecho por el browser.
        transferCache: false,
      })
      .pipe(
        timeout(15000),
        map((response) => this.resolveVerificationResponse(response))
      );
  }

  private resolveVerificationResponse(
    response: AuthTokenResponse | ApiMessageResponse
  ): LoginResponse | ApiMessageResponse {
    if (!this.isAuthTokenResponse(response)) {
      return response;
    }

    return {
      token: response.accessToken,
      user: this.userFromAuthTokenResponse(response),
    };
  }

  private isAuthTokenResponse(
    response: AuthTokenResponse | ApiMessageResponse
  ): response is AuthTokenResponse {
    return !!response && typeof response === 'object' && 'accessToken' in response;
  }

  private userFromAuthTokenResponse(response: AuthTokenResponse): User {
    const userId = response.user.userId ?? this.userIdFromHref(response.user.href);
    return {
      userId,
      username: response.user.username ?? '',
      name: response.user.name ?? '',
      surname: response.user.surname ?? '',
      email: response.user.email ?? '',
      cellphone: response.user.cellphone ?? '',
      role: response.user.role ?? 'USER',
      verified: response.user.verified ?? true,
      createdAt: response.user.createdAt ?? '',
      locale: response.user.locale ?? '',
    };
  }

  private userIdFromHref(href: string): string {
    const parts = href.split('/').filter(Boolean);
    return parts[parts.length - 1] ?? '';
  }

  resendVerification(payload: ResendVerificationRequest): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/auth/verification-requests`, payload);
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post(`${this.apiUrl}/auth/password-reset-requests`, { email }).pipe(
      map(() => undefined)
    );
  }

  resetPassword(token: string, newPassword: string, confirmationPassword: string): Observable<LoginResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.post<AuthTokenResponse>(
      `${this.apiUrl}/auth/password-resets`,
      { newPassword, confirmationPassword },
      { params }
    ).pipe(
      switchMap((tokenResponse) => {
        const nextToken = tokenResponse.accessToken;
        const headers = new HttpHeaders({ Authorization: `Bearer ${nextToken}` });
        return this.http.get<User>(tokenResponse.user.href, { headers }).pipe(
          map((user) => ({ token: nextToken, user }))
        );
      })
    );
  }
}
