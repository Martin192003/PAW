import { Component, OnDestroy } from '@angular/core';
import { NgClass, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
import { ApiMessageResponse, LoginResponse } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { I18nService } from '../services/i18n.service';
import { UserService } from '../services/user.service';

@Component({
  standalone: true,
  imports: [NgIf, NgClass, ReactiveFormsModule, TranslateModule],
  templateUrl: './verify-mail.page.html',
})
export class VerifyMailPage implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  email = '';

  tokenChecking = false;
  tokenSuccessMessage = '';
  tokenErrorMessage = '';

  codeSubmitting = false;
  codeSuccessMessage = '';
  codeErrorMessage = '';

  resendSubmitting = false;
  resendSuccessMessage = '';
  resendErrorMessage = '';

  private autoResendTriggered = false;

  readonly codeForm: FormGroup;
  readonly resendForm: FormGroup;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly authState: AuthStateService,
    private readonly i18n: I18nService,
    private readonly userService: UserService,
  ) {
    this.codeForm = this.fb.group({
      code: ['', [Validators.required]],
    });

    this.resendForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
    });

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const emailParam = params.get('email') ?? '';
      const usernameOrEmailParam = params.get('usernameOrEmail') ?? '';
      const token = params.get('token') ?? '';
      const autoResend = params.get('autoResend') === 'true';

      this.email = emailParam || usernameOrEmailParam;

      if (this.email) {
        this.resendForm.patchValue({ usernameOrEmail: this.email });
      }

      if (token) {
        this.verifyFromToken(token);
      }

      if (autoResend && !this.autoResendTriggered && this.email) {
        this.autoResendTriggered = true;
        this.triggerAutoResend(this.email);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  verifyWithCode(): void {
    this.codeForm.markAllAsTouched();
    if (this.codeForm.invalid || this.codeSubmitting) return;

    this.codeSubmitting = true;
    this.codeSuccessMessage = '';
    this.codeErrorMessage = '';

    const code = this.codeForm.value.code?.trim() ?? '';
    this.userService.verifyByCode({ code }).subscribe({
      next: (response) => {
        this.codeSubmitting = false;
        this.handleVerificationSuccess(response, 'code');
      },
      error: (error: HttpErrorResponse) => {
        this.codeSubmitting = false;
        this.codeErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.verifyCode'));
      },
    });
  }

  resendVerification(): void {
    this.resendForm.markAllAsTouched();
    if (this.resendForm.invalid || this.resendSubmitting) return;

    this.resendSubmitting = true;
    this.resendSuccessMessage = '';
    this.resendErrorMessage = '';

    const usernameOrEmail = this.resendForm.value.usernameOrEmail?.trim() ?? '';
    this.userService.resendVerification({ usernameOrEmail }).subscribe({
      next: (response) => {
        this.resendSubmitting = false;
        this.resendSuccessMessage = response.message || this.i18n.instant('verifyMail.messages.resendSuccess');
      },
      error: (error: HttpErrorResponse) => {
        this.resendSubmitting = false;
        this.resendErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.resend'));
      },
    });
  }

  showCodeError(): boolean {
    const control = this.codeForm.get('code');
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  showResendError(): boolean {
    const control = this.resendForm.get('usernameOrEmail');
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  private verifyFromToken(token: string): void {
    this.tokenChecking = true;
    this.tokenSuccessMessage = '';
    this.tokenErrorMessage = '';

    this.userService.verifyByToken(token.trim()).subscribe({
      next: (response) => {
        this.tokenChecking = false;
        this.handleVerificationSuccess(response, 'token');
      },
      error: (error: HttpErrorResponse) => {
        this.tokenChecking = false;
        this.tokenErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.verifyLink'));
      },
    });
  }

  private triggerAutoResend(usernameOrEmail: string): void {
    this.resendSubmitting = true;
    this.resendSuccessMessage = '';
    this.resendErrorMessage = '';

    this.userService.resendVerification({ usernameOrEmail }).subscribe({
      next: (response) => {
        this.resendSubmitting = false;
        this.resendSuccessMessage = response.message || 'Te enviamos un nuevo mail de verificacion.';
      },
      error: (error: HttpErrorResponse) => {
        this.resendSubmitting = false;
        this.resendErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.resend'));
      },
    });
  }

  private handleVerificationSuccess(response: LoginResponse | ApiMessageResponse, source: 'token' | 'code'): void {
    if (this.isLoginResponse(response)) {
      // Auto-login despues de verificar correctamente.
      this.authState.setSession(response.token, response.user, true);
      this.router.navigate(['/clubs']);
      return;
    }

    const message = response.message || this.i18n.instant('verifyMail.messages.verifiedSuccess');
    if (source === 'token') {
      this.tokenSuccessMessage = message;
    } else {
      this.codeSuccessMessage = message;
    }
  }

  private isLoginResponse(response: LoginResponse | ApiMessageResponse): response is LoginResponse {
    return 'token' in response && 'user' in response;
  }

  private resolveErrorMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'object' && error.error !== null && 'message' in error.error) {
      const msg = (error.error as { message?: unknown }).message;
      if (typeof msg === 'string' && msg.trim()) return msg;
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      try {
        const parsed = JSON.parse(error.error) as { message?: string };
        if (parsed.message) return parsed.message;
      } catch {
        return error.error;
      }
    }

    return fallback;
  }
}
