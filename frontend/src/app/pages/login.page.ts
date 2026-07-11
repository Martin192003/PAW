import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserLoginRequest } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { UserService } from '../services/user.service';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './login.page.html',
})
export class LoginPage implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  submitting = false;
  invalidCredentials = false;
  errorMessage = '';

  loginForm!: FormGroup;

  /**
   * Destino al que volver después de un login exitoso (por ejemplo,
   * `/clubs/new` cuando authGuard interrumpió el flujo de creación de club).
   * Viene del query param `returnUrl` que setean los guards al redirigir acá.
   */
  private returnUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authState: AuthStateService,
    private userService: UserService,
    private i18n: I18nService,
    private location: Location,
  ) {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [false],
    });
  }

  ngOnInit(): void {
    this.returnUrl = this.sanitizeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));

    if (this.authState.isLoggedIn()) {
      this.redirectAfterLogin();
      return;
    }

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.invalidCredentials = params.has('error');
      if (this.invalidCredentials) {
        this.errorMessage = this.i18n.instant('login.errors.invalidCredentials');
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid || this.submitting) return;

    this.submitting = true;
    this.invalidCredentials = false;
    this.errorMessage = '';

    const usernameOrEmail = this.loginForm.value.usernameOrEmail.trim();
    const payload: UserLoginRequest = {
      usernameOrEmail,
      password: this.loginForm.value.password,
    };

    this.userService.login(payload).pipe(
      finalize(() => { this.submitting = false; })
    ).subscribe({
      next: (response) => {
        try {
          this.authState.setSession(response.token, response.user, !!this.loginForm.value.rememberMe);
          this.redirectAfterLogin();
        } catch {
          this.errorMessage = this.i18n.instant('login.errors.loginFailed');
        }
      },
      error: (err: unknown) => {
        const error = err instanceof HttpErrorResponse ? err : null;
        const status = error?.status ?? 0;

        if (status === 403) {
          // Backend retorna 403 para cuentas no verificadas (DisabledException).
          // Redirigir al flow de verificación y pedir reenvio automatico.
          this.router.navigate(['/verify-mail'], {
            queryParams: {
              usernameOrEmail,
              autoResend: 'true',
            },
          });
          return;
        }

        this.invalidCredentials = status === 401;
        this.errorMessage = error ? this.resolveErrorMessage(error) : this.i18n.instant('login.errors.loginFailed');
      },
    });
  }

  showError(field: 'usernameOrEmail' | 'password'): boolean {
    const control = this.loginForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  goBack(): void {
    if (typeof window !== 'undefined' && window.history.length > 1) {
      this.location.back();
      return;
    }

    void this.router.navigate(['/clubs']);
  }


  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return this.i18n.instant('login.errors.invalidCredentials');
    }
    if (error.status === 403) {
      return this.extractServerMessage(error.error) ?? this.i18n.instant('login.errors.unverified');
    }
    if (error.status === 400) {
      return this.extractServerMessage(error.error) ?? this.i18n.instant('login.errors.required');
    }
    return this.extractServerMessage(error.error) ?? this.i18n.instant('login.errors.loginFailed');
  }

  /**
   * Solo aceptamos returnUrl que sean una ruta interna de la app (empieza
   * con "/" pero no "//", que el navegador podria interpretar como un host
   * externo). Cualquier otra cosa se descarta para evitar un open redirect.
   */
  private sanitizeReturnUrl(raw: string | null): string | null {
    if (!raw) return null;
    if (!raw.startsWith('/') || raw.startsWith('//')) return null;
    return raw;
  }

  /**
   * Vuelve a returnUrl si un guard lo dejó guardado (ej. clubs/new), o a
   * /clubs por default. Usamos navigateByUrl en vez de navigate([...]) porque
   * returnUrl es una URL completa (puede traer sus propios query params,
   * como pasa con /admin/bookings?status=...) y navigate() no la parsea bien.
   */
  private redirectAfterLogin(): void {
    const destination$ = this.returnUrl
      ? this.router.navigateByUrl(this.returnUrl, { replaceUrl: true })
      : this.router.navigate(['/clubs'], { replaceUrl: true });

    void destination$.catch(() => {
      this.errorMessage = this.i18n.instant('login.errors.loginFailed');
    });
  }

  private extractServerMessage(errorBody: unknown): string | null {
    if (!errorBody) return null;

    if (typeof errorBody === 'string') {
      try {
        const parsed = JSON.parse(errorBody) as { message?: string };
        return parsed.message ?? null;
      } catch {
        return errorBody;
      }
    }

    if (errorBody && typeof errorBody === 'object' && 'message' in errorBody) {
      const message = (errorBody as { message?: unknown }).message;
      return typeof message === 'string' ? message : null;
    }

    return null;
  }
}
