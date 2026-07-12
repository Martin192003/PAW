import { Component, OnDestroy } from '@angular/core';
import { NgClass, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
import { ApiMessageResponse, LoginResponse } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { I18nService } from '../services/i18n.service';
import { UserService } from '../services/user.service';

@Component({
  standalone: true,
  imports: [NgIf, NgClass, ReactiveFormsModule, TranslateModule, RouterLink],
  templateUrl: './verify-mail.page.html',
})
export class VerifyMailPage implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  email = '';

  tokenChecking = false;
  tokenSuccessMessage = '';
  tokenErrorMessage = '';

  resendSubmitting = false;
  resendSuccessMessage = '';
  resendErrorMessage = '';

  verifyingFromLink = false;
  verificationCompleted = false;

  private autoResendTriggered = false;

  readonly resendForm: FormGroup;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly authState: AuthStateService,
    private readonly i18n: I18nService,
    private readonly userService: UserService,
  ) {
    this.resendForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
    });

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const emailParam = params.get('email') ?? '';
      const usernameOrEmailParam = params.get('usernameOrEmail') ?? '';
      const token = params.get('token') ?? '';
      const autoResend = params.get('autoResend') === 'true';

      this.email = emailParam || usernameOrEmailParam;
      this.verifyingFromLink = !!token;

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

  showResendError(): boolean {
    const control = this.resendForm.get('usernameOrEmail');
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get verifiedStateVisible(): boolean {
    return this.verificationCompleted || (this.verifyingFromLink && this.authState.isLoggedIn());
  }

  get linkCheckInProgress(): boolean {
    return this.verifyingFromLink && this.tokenChecking && !this.verifiedStateVisible && !this.tokenErrorMessage;
  }

  private verifyFromToken(token: string): void {
    this.tokenChecking = true;
    this.tokenSuccessMessage = '';
    this.tokenErrorMessage = '';

    this.userService.verifyByToken(token.trim()).subscribe({
      next: (response) => {
        this.tokenChecking = false;
        this.handleVerificationSuccess(response);
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

  private handleVerificationSuccess(response: LoginResponse | ApiMessageResponse): void {
    if (this.isLoginResponse(response)) {
      this.authState.setSession(response.token, response.user, true);
    }

    this.tokenSuccessMessage = this.i18n.instant('verifyMail.messages.verifiedSuccess');
    this.verificationCompleted = true;
  }

  continueAfterVerification(): void {
    this.router.navigate(['/clubs']);
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
