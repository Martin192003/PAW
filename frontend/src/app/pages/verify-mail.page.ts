import { ChangeDetectorRef, Component, Inject, OnDestroy, PLATFORM_ID } from '@angular/core';
import { NgClass, NgIf, isPlatformBrowser } from '@angular/common';
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


  redirectCountdown = 5;
  private redirectTimerId: ReturnType<typeof setInterval> | null = null;

  private autoResendTriggered = false;

  readonly resendForm: FormGroup;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly authState: AuthStateService,
    private readonly i18n: I18nService,
    private readonly userService: UserService,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private readonly platformId: object,
  ) {
    this.resendForm = this.fb.group({
      usernameOrEmail: ['', [Validators.required]],
    });

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const emailParam = params.get('email') ?? '';
      const usernameOrEmailParam = params.get('usernameOrEmail') ?? '';
      const token = params.get('token') ?? '';
      const userId = params.get('userId') ?? '';
      const autoResend = params.get('autoResend') === 'true';

      this.email = emailParam || usernameOrEmailParam;
      this.verifyingFromLink = !!token;
      this.tokenChecking = !!token;

      if (this.email) {
        this.resendForm.patchValue({ usernameOrEmail: this.email });
      }


      if (!isPlatformBrowser(this.platformId)) {
        return;
      }

      if (token) {
        this.verifyFromToken(userId, token);
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
    this.clearRedirectTimer();
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
        this.resendSuccessMessage = this.displayMessageForApiCode(response.message || 'verification_email_sent');
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.resendSubmitting = false;
        this.resendErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.resend'));
        this.cdr.detectChanges();
      },
    });
  }

  showResendError(): boolean {
    const control = this.resendForm.get('usernameOrEmail');
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get verifiedStateVisible(): boolean {
    return this.verificationCompleted;
  }

  get linkCheckInProgress(): boolean {
    return this.verifyingFromLink && this.tokenChecking && !this.verifiedStateVisible && !this.tokenErrorMessage;
  }


  get headingKey(): string {
    if (this.verifiedStateVisible) return 'verifyMail.success.title';
    if (this.linkCheckInProgress) return 'verifyMail.linkVerification.title';
    return 'verifyMail.title';
  }

  get descriptionKey(): string {
    if (this.verifiedStateVisible) return 'verifyMail.success.description';
    if (this.linkCheckInProgress) return 'verifyMail.linkVerification.description';
    return 'verifyMail.description';
  }

  private verifyFromToken(userId: string, token: string): void {
    this.tokenChecking = true;
    this.tokenSuccessMessage = '';
    this.tokenErrorMessage = '';

    this.userService.verifyByToken(userId.trim(), token.trim()).subscribe({
      next: (response) => {
        this.tokenChecking = false;
        this.handleVerificationSuccess(response);
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.tokenChecking = false;
        this.tokenErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.verifyLink'));
        this.cdr.detectChanges();
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
        this.resendSuccessMessage = this.displayMessageForApiCode(response.message || 'verification_email_sent');
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.resendSubmitting = false;
        this.resendErrorMessage = this.resolveErrorMessage(error, this.i18n.instant('verifyMail.errors.resend'));
        this.cdr.detectChanges();
      },
    });
  }

  private handleVerificationSuccess(response: LoginResponse | ApiMessageResponse): void {
    if (this.isLoginResponse(response)) {
      this.authState.setSession(response.token, response.user, true);
    }

    this.tokenSuccessMessage = this.i18n.instant('verifyMail.messages.verifiedSuccess');
    this.verificationCompleted = true;
    this.startRedirectCountdown();
  }

  private startRedirectCountdown(): void {

    if (!isPlatformBrowser(this.platformId) || this.redirectTimerId) {
      return;
    }

    this.redirectCountdown = 5;
    this.redirectTimerId = setInterval(() => {
      this.redirectCountdown -= 1;
      if (this.redirectCountdown <= 0) {
        this.clearRedirectTimer();
        this.continueAfterVerification();
        return;
      }
      this.cdr.detectChanges();
    }, 1000);
  }

  private clearRedirectTimer(): void {
    if (this.redirectTimerId !== null) {
      clearInterval(this.redirectTimerId);
      this.redirectTimerId = null;
    }
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
      if (typeof msg === 'string' && msg.trim()) return this.displayMessageForApiCode(msg.trim());
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      try {
        const parsed = JSON.parse(error.error) as { message?: string };
        if (parsed.message) return this.displayMessageForApiCode(parsed.message);
      } catch {
        return error.error;
      }
    }

    return fallback;
  }

  private displayMessageForApiCode(message: string): string {
    switch (message) {
      case 'invalid_verification_token':
        return this.i18n.instant('verifyMail.errors.expiredVerificationLink');
      case 'verification_token_required':
        return this.i18n.instant('verifyMail.errors.missingVerificationToken');
      case 'verification_email_sent':
        return this.i18n.instant('verifyMail.messages.resendSuccess');
      default:
        return message;
    }
  }
}
