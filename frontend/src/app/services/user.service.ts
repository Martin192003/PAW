import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, switchMap, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ApiMessageResponse,
  LoginResponse,
  PublicUser,
  ResendVerificationRequest,
  UserCreateRequest,
  UserLoginRequest,
  User,
  UserUpdateRequest,
} from '../models/user.model';
import { SKIP_API_ERROR_REDIRECT } from './api-error.interceptor';

export type UserUpdateResult = {
  user: User;
  token: string | null;
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

  getUserById(userId: string): Observable<PublicUser> {
    return this.http.get<PublicUser>(`${this.apiUrl}/users/${userId}`);
  }

  getCurrentUserById(userId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${userId}`);
  }

  register(payload: UserCreateRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/users`, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    });
  }

  updateUser(userId: string, payload: UserUpdateRequest): Observable<UserUpdateResult> {

    return this.http.patch<UserUpdateResponse>(`${this.apiUrl}/users/${userId}`, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      map((response) => ({
        user: response.user,
        token: response.accessToken ?? null
      }))
    );
  }

  login(payload: UserLoginRequest): Observable<LoginResponse> {
    const usernameOrEmail = payload.usernameOrEmail.trim();
    const basicHeaders = new HttpHeaders({
      Authorization: this.basicAuthorization(usernameOrEmail, payload.password),
    });


    return this.http.get(`${this.apiUrl}/bookings`, {
      headers: basicHeaders,
      observe: 'response',
      params: { page: '0', size: '1' },
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      switchMap((response) => {
        const token = response.headers.get('Paw-Auth-Token');
        const userId = token ? this.userIdFromToken(token) : null;
        if (!token || !userId) {
          throw new Error('missing_auth_response');
        }
        const bearerHeaders = new HttpHeaders({ Authorization: `Bearer ${token}` });
        return this.http.get<User>(`${this.apiUrl}/users/${encodeURIComponent(userId)}`, {
          headers: bearerHeaders,
          context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
        }).pipe(
          map((user) => ({ token, user }))
        );
      })
    );
  }


  private userIdFromToken(token: string): string | null {
    const parts = token.trim().replace(/^Bearer\s+/i, '').split('.');
    if (parts.length < 2) {
      return null;
    }
    try {
      let payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      while (payload.length % 4 !== 0) {
        payload += '=';
      }
      const claims = JSON.parse(atob(payload)) as { sub?: string };
      return typeof claims.sub === 'string' && claims.sub.length > 0 ? claims.sub : null;
    } catch {
      return null;
    }
  }


  verifyByToken(userId: string, token: string): Observable<LoginResponse | ApiMessageResponse> {
    return this.http
      .put<UserUpdateResponse | ApiMessageResponse>(`${this.apiUrl}/users/${encodeURIComponent(userId)}/verification`, {
        token: token.trim(),
      }, {
        context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
      })
      .pipe(
        timeout(15000),
        map((response) => {
          const session = response as UserUpdateResponse;
          if (session && session.accessToken && session.user) {
            return { token: session.accessToken, user: session.user } as LoginResponse;
          }
          return response as ApiMessageResponse;
        })
      );
  }

  resendVerification(payload: ResendVerificationRequest): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/auth/challenges`, {
      challengeType: 'email_verification',
      usernameOrEmail: payload.usernameOrEmail,
    });
  }

  requestPasswordReset(email: string): Observable<void> {

    return this.http.post(`${this.apiUrl}/auth/challenges`, {
      challengeType: 'password_reset',
      email,
    }, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      map(() => undefined)
    );
  }

  resetPassword(token: string, newPassword: string, confirmationPassword: string): Observable<LoginResponse> {

    return this.http.put<UserUpdateResponse>(
      `${this.apiUrl}/users/credentials/password`,
      {
        token,
        newPassword,
        confirmationPassword
      },
      { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) }
    ).pipe(
      map((response) => this.loginResponseFromUserUpdateResponse(response))
    );
  }

  private loginResponseFromUserUpdateResponse(response: UserUpdateResponse): LoginResponse {
    if (!response.accessToken || !response.user) {
      throw new Error('missing_auth_response');
    }
    return { token: response.accessToken, user: response.user };
  }


  changePasswordAuthenticated(userId: string, newPassword: string, confirmationPassword: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/users/${userId}/credentials/password`, { newPassword, confirmationPassword }, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    });
  }

  private basicAuthorization(usernameOrEmail: string, password: string): string {
    return `Basic ${this.base64Encode(`${usernameOrEmail}:${password}`)}`;
  }

  private base64Encode(value: string): string {
    const bytes = new TextEncoder().encode(value);
    let binary = '';
    bytes.forEach((byte) => {
      binary += String.fromCharCode(byte);
    });
    return btoa(binary);
  }
}
