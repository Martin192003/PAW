import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiMessageResponse,
  LoginResponse,
  ResendVerificationRequest,
  UserCreateRequest,
  UserLoginRequest,
  UserVerificationRequest,
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = 'http://localhost:8080/webapp_war_exploded/api';

  constructor(private readonly http: HttpClient) {}

  register(payload: UserCreateRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/users`, payload);
  }

  login(payload: UserLoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/users/login`, payload);
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
}
