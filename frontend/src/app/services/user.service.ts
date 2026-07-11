import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
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

type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    href: string;
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
    return this.http.post(`${this.apiUrl}/users`, payload);
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

  resetPassword(token: string, newPassword: string, confirmationPassword: string): Observable<LoginResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.post<AuthTokenResponse>(
      `${this.apiUrl}/users/password-resets`,
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
