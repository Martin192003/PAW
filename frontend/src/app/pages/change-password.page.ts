import { Component, OnDestroy, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { UserService } from '../services/user.service';
import { AuthStateService } from '../services/auth-state.service';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

@Component({
  standalone: true,
  selector: 'app-change-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './change-password.page.html',
  styleUrls: ['./change-password.page.css'],
})
export class ChangePasswordPage implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  token: string | null = null;
  submittingForgot = false;
  submittingReset = false;
  showForgotModal = false;
  showResetModal = false;
  useLoggedUserEmail = false;

  forgotError = '';
  resetError = '';
  forgotSuccess = '';
  resetCompleted = false;

  forgotForm: FormGroup;
  resetForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly location: Location,
    private readonly userService: UserService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
    private readonly ngZone: NgZone,
    private readonly i18n: I18nService,
  ) {
    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });

    this.resetForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmationPassword: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.token = params.get('token');
      const loggedUserEmail = this.authState.getUser()?.email ?? '';
      this.useLoggedUserEmail = !this.token && params.get('source') === 'profile' && loggedUserEmail.trim().length > 0;
      if (this.useLoggedUserEmail) {
        this.forgotForm.patchValue({ email: loggedUserEmail });
      }
      this.showResetModal = false;
      this.resetError = '';
      this.resetCompleted = false;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submitForgot(): void {
    this.forgotForm.markAllAsTouched();
    if (this.forgotForm.invalid || this.submittingForgot) return;

    this.submittingForgot = true;
    this.forgotError = '';
    this.forgotSuccess = '';
    this.showForgotModal = false;

    const email = (this.forgotForm.value.email ?? '').trim();

    this.userService.requestPasswordReset(email).pipe(
      finalize(() => {
        this.submittingForgot = false;
      })
    ).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.forgotSuccess = this.i18n.instant('passwordReset.modal.sentBody');
          this.showForgotModal = true;
          this.cdr.detectChanges();
        });
      },
      error: (error: HttpErrorResponse) => {
        this.ngZone.run(() => {
          this.forgotError = this.resolveForgotError(error);
          this.cdr.detectChanges();
        });
      },
    });
  }

  submitReset(): void {
    this.resetForm.markAllAsTouched();
    if (this.resetForm.invalid || this.submittingReset) return;

    const newPassword = this.resetForm.value.newPassword;
    const confirmationPassword = this.resetForm.value.confirmationPassword;

    if (newPassword !== confirmationPassword) {
      this.resetError = this.i18n.instant('passwordReset.errors.passwordsDontMatch');
      return;
    }

    if (!this.token) {
      this.resetError = this.i18n.instant('passwordReset.errors.invalidToken');
      return;
    }

    this.submittingReset = true;
    this.resetError = '';
    this.resetCompleted = false;
    this.showResetModal = false;

    this.userService.resetPassword(this.token, newPassword, confirmationPassword).pipe(
      finalize(() => {
        this.submittingReset = false;
      })
    ).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.resetCompleted = true;
          this.showResetModal = true;
          this.cdr.detectChanges();
        });
      },
      error: (error: HttpErrorResponse) => {
        this.ngZone.run(() => {
          this.resetError = this.resolveResetError(error);
          this.cdr.detectChanges();
        });
      },
    });
  }

  goToClubs(): void {
    this.router.navigate(['/clubs']);
  }

  goBack(): void {
    this.location.back();
  }

  showForgotError(): boolean {
    const control = this.forgotForm.get('email');
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  showResetError(field: 'newPassword' | 'confirmationPassword'): boolean {
    const control = this.resetForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  confirmForgotModal(): void {
    this.showForgotModal = false;
    this.forgotSuccess = '';
    this.router.navigate([this.useLoggedUserEmail ? '/profile' : '/login']);
  }

  confirmResetModal(): void {
    this.showResetModal = false;
    this.resetCompleted = false;
    this.router.navigate(['/login']);
  }

  private resolveForgotError(error: HttpErrorResponse): string {
    if (error.status === 0) return this.i18n.instant('passwordReset.errors.network');
    if (error.status === 400) return this.i18n.instant('passwordReset.errors.invalidEmail');
    return this.i18n.instant('passwordReset.errors.emailSendFailed');
  }

  private resolveResetError(error: HttpErrorResponse): string {
    if (error.status === 0) return this.i18n.instant('passwordReset.errors.network');
    if (error.status === 400) return this.i18n.instant('passwordReset.errors.invalidData');
    if (error.status === 404) return this.i18n.instant('passwordReset.errors.invalidToken');
    if (error.status === 409) return this.i18n.instant('passwordReset.errors.passwordUnchanged');
    return this.i18n.instant('passwordReset.errors.updateFailed');
  }
}
