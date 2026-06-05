import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ApiMessageResponse,
  LoginResponse,
  ResendVerificationRequest,
  UserCreateRequest,
  UserLoginRequest,
  UserVerificationRequest,
  User,
} from '../models/user.model';

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

export type PasswordResetTokenResult = {
  token: string;
};

type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    href: string;
  };
};

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  getUserById(userId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${userId}`);
  }

  /**
   * Devuelve el usuario autenticado actual (GET /api/users/me).
   * Requiere Bearer token en el header Authorization (lo agrega el interceptor).
   */
  getMe(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/me`);
  }

  register(payload: UserCreateRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/users`, payload);
  }

  updateUser(userId: string, payload: UserUpdatePayload): Observable<UserUpdateResult> {
    return this.http.put<User>(`${this.apiUrl}/users/${userId}`, payload, { observe: 'response' }).pipe(
      map((response: HttpResponse<User>) => ({
        user: response.body as User,
        token: response.headers.get('X-Access-Token')
      }))
    );
  }

  login(payload: UserLoginRequest): Observable<LoginResponse> {
    return this.http.post<AuthTokenResponse>(`${this.apiUrl}/auth/tokens`, {
      email: payload.usernameOrEmail.trim(),
      password: payload.password,
    }).pipe(
      switchMap((tokenResponse) => {
        const token = tokenResponse.accessToken;
        const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
        return this.http.get<User>(`${this.apiUrl}/users/me`, { headers }).pipe(
          map((user) => ({ token, user }))
        );
      })
    );
  }

  verifyByToken(token: string): Observable<LoginResponse | ApiMessageResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.get<LoginResponse | ApiMessageResponse>(`${this.apiUrl}/users/verifications`, { params });
  }

  verifyByCode(payload: UserVerificationRequest): Observable<LoginResponse | ApiMessageResponse> {
    return this.http.post<LoginResponse | ApiMessageResponse>(`${this.apiUrl}/users/verifications`, payload);
  }

  resendVerification(payload: ResendVerificationRequest): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/users/verification-requests`, payload);
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post(`${this.apiUrl}/users/password-reset-requests`, { email }).pipe(
      map(() => undefined)
    );
  }

  createPasswordResetToken(userId: string): Observable<PasswordResetTokenResult> {
    return this.http.post<PasswordResetTokenResult>(`${this.apiUrl}/users/${userId}/password-reset-tokens`, {});
  }

  resetPassword(token: string, newPassword: string, confirmationPassword: string): Observable<void> {
    const params = new HttpParams().set('token', token);
    return this.http.post(`${this.apiUrl}/users/password-resets`, { newPassword, confirmationPassword }, { params }).pipe(
      map(() => undefined)
    );
  }
}
