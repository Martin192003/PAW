import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
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

type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    href: string;
    userId?: string;
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
    // Sin este flag, un 409 (usuario/email duplicado) lo agarra el interceptor
    // global y redirige a /error antes de que profile.page.ts pueda mostrarlo
    // como error de campo (mismo problema que tenian createTable/createProduct).
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
    return this.http.post<AuthTokenResponse>(`${this.apiUrl}/auth/tokens`, {
      grantType: 'password',
      usernameOrEmail: payload.usernameOrEmail.trim(),
      password: payload.password,
    }, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      switchMap((tokenResponse) => this.loginResponseFromTokenResponse(tokenResponse))
    );
  }

  /**
   * POST /auth/tokens con grant email_verification.
   *
   * El backend puede responder de dos formas: un AuthTokenResponse (cuenta
   * recien verificada, login automatico) o un ApiMessageResponse (ya estaba
   * verificada, o el backend todavia no emite token). Igual que login() y
   * resetPassword(), si viene un token hay que resolver el usuario completo
   * antes de devolver un LoginResponse utilizable por la pagina.
   */
  verifyByToken(token: string): Observable<LoginResponse | ApiMessageResponse> {
    return this.http
      .post<AuthTokenResponse | ApiMessageResponse>(`${this.apiUrl}/auth/tokens`, {
        grantType: 'email_verification',
        token: token.trim(),
      }, {
        context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
        transferCache: false,
      })
      .pipe(
        timeout(15000),
        switchMap((response) => this.resolveVerificationResponse(response))
      );
  }

  private resolveVerificationResponse(
    response: AuthTokenResponse | ApiMessageResponse
  ): Observable<LoginResponse | ApiMessageResponse> {
    if (!this.isAuthTokenResponse(response)) {
      return of(response);
    }

    return this.loginResponseFromTokenResponse(response);
  }

  private isAuthTokenResponse(
    response: AuthTokenResponse | ApiMessageResponse
  ): response is AuthTokenResponse {
    return !!response && typeof response === 'object' && 'accessToken' in response;
  }

  resendVerification(payload: ResendVerificationRequest): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.apiUrl}/auth/challenges`, {
      challengeType: 'email_verification',
      usernameOrEmail: payload.usernameOrEmail,
    });
  }

  requestPasswordReset(email: string): Observable<void> {
    // Sin este flag, cualquier codigo de la lista del interceptor
    // (401/403/404/409/415/500+) redirige a /error antes de que
    // change-password.page.ts pueda mostrar su propio error de campo/cartel.
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
    // Sin este flag, un token invalido/vencido (404) redirige a /error en vez
    // de mostrar el cartel de "token invalido o vencido" en el propio formulario.
    return this.http.post<AuthTokenResponse>(
      `${this.apiUrl}/auth/tokens`,
      {
        grantType: 'password_reset',
        token,
        newPassword,
        confirmationPassword
      },
      { context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true) }
    ).pipe(
      switchMap((tokenResponse) => this.loginResponseFromTokenResponse(tokenResponse))
    );
  }

  private loginResponseFromTokenResponse(tokenResponse: AuthTokenResponse): Observable<LoginResponse> {
    const token = tokenResponse.accessToken;
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    return this.http.get<User>(tokenResponse.user.href, { headers }).pipe(
      map((user) => ({ token, user }))
    );
  }

  /**
   * Cambio de contraseña para un usuario que ya tiene sesion iniciada (ej.
   * desde /profile). A diferencia de resetPassword(), no hace falta un token
   * de mail: la identidad sale del JWT que ya se manda en cada request
   * (interceptor de auth), asi que no hace falta reflejar un nuevo token acá.
   */
  changePasswordAuthenticated(userId: string, newPassword: string, confirmationPassword: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/users/${userId}/credentials/password`, { newPassword, confirmationPassword }, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    });
  }
}
