import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserLoginRequest } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { UserService } from '../services/user.service';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './login.page.html',
})
export class LoginPage implements OnInit {
  submitting = false;
  invalidCredentials = false;
  errorMessage = '';

  loginForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authState: AuthStateService,
    private userService: UserService,
    private i18n: I18nService,
  ) {
    this.loginForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
      password: ['', [Validators.required]],
      rememberMe: [false],
    });
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.invalidCredentials = params.has('error');
      if (this.invalidCredentials) {
        this.errorMessage = this.i18n.instant('login.errors.invalidCredentials');
      }
    });
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

    this.userService.login(payload).subscribe({
      next: (response) => {
        this.submitting = false;
        this.authState.setSession(response.token, response.user, !!this.loginForm.value.rememberMe);
        this.router.navigate(['/clubs']);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting = false;

        if (error.status === 403) {
          // Si no esta verificado, enviamos al flujo de verificacion y pedimos reenvio automatico.
          this.router.navigate(['/verify-mail'], {
            queryParams: {
              usernameOrEmail,
              autoResend: 'true',
            },
          });
          return;
        }

        this.invalidCredentials = error.status === 401;
        this.errorMessage = this.resolveErrorMessage(error);
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/clubs']);
  }

  showError(field: 'usernameOrEmail' | 'password'): boolean {
    const control = this.loginForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
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

    if (typeof errorBody === 'object' && errorBody !== null && 'message' in errorBody) {
      const message = (errorBody as { message?: unknown }).message;
      return typeof message === 'string' ? message : null;
    }

    return null;
  }
}
