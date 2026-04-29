import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = 'http://localhost:8080/webapp_war_exploded/api';

  constructor(private readonly http: HttpClient) {}

  getUserById(userId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${userId}`);
  }

  register(payload: UserCreateRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/users`, payload);
  }

  updateUser(userId: string, payload: UserUpdatePayload): Observable<UserUpdateResult> {
    return this.http.put<User>(`${this.apiUrl}/users/${userId}`, payload, { observe: 'response' }).pipe(
      map((response: HttpResponse<User>) => ({
        user: response.body as User,
        token: null
      }))
    );
  }

  login(payload: UserLoginRequest): Observable<LoginResponse> {
    const headers = new HttpHeaders({
      Authorization: `Basic ${this.basicCredentials(payload.usernameOrEmail, payload.password)}`,
    });

    return this.http.get<unknown>(`${this.apiUrl}/bookings`, { headers, observe: 'response' }).pipe(
      map((response: HttpResponse<unknown>) => ({
        token: this.requiredHeader(response, 'X-Access-Token'),
        user: this.userFromAuthHeaders(response),
      }))
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

  private userFromAuthHeaders(response: HttpResponse<unknown>): User {
    const href = response.headers.get('X-User-Href') ?? '';
    const userId = href.substring(href.lastIndexOf('/') + 1);

    return {
      userId,
      username: '',
      name: '',
      surname: '',
      email: '',
      cellphone: '',
      role: '',
      verified: true,
      createdAt: '',
      locale: '',
    };
  }

  private requiredHeader(response: HttpResponse<unknown>, header: string): string {
    const value = response.headers.get(header);
    if (!value) {
      throw new Error(`Missing ${header} response header`);
    }
    return value;
  }

  private basicCredentials(username: string, password: string): string {
    const raw = `${username.trim()}:${password}`;
    return btoa(unescape(encodeURIComponent(raw)));
  }
}
