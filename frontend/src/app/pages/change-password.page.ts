import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { UserService } from '../services/user.service';
import { finalize } from 'rxjs/operators';

@Component({
  standalone: true,
  selector: 'app-change-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './change-password.page.html',
  styleUrls: ['./change-password.page.css'],
})
export class ChangePasswordPage implements OnInit {
  token: string | null = null;
  submittingForgot = false;
  submittingReset = false;
  showForgotModal = false;
  showResetModal = false;

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
    private readonly cdr: ChangeDetectorRef,
    private readonly ngZone: NgZone,
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
    this.route.queryParamMap.subscribe((params) => {
      this.token = params.get('token');
      this.showResetModal = false;
      this.resetError = '';
      this.resetCompleted = false;
    });
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
          this.forgotSuccess = 'Te enviamos un email con instrucciones.';
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
      this.resetError = 'Las contrasenas no coinciden.';
      return;
    }

    if (!this.token) {
      this.resetError = 'Token invalido o vencido.';
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
    this.router.navigate(['/login']);
  }

  confirmResetModal(): void {
    this.showResetModal = false;
    this.resetCompleted = false;
    this.router.navigate(['/login']);
  }

  private resolveForgotError(error: HttpErrorResponse): string {
    if (error.status === 0) return 'No pudimos conectar con el servidor.';
    if (error.status === 400) return 'Revisa el email e intentalo de nuevo.';
    return 'No pudimos enviar el email. Intentalo mas tarde.';
  }

  private resolveResetError(error: HttpErrorResponse): string {
    if (error.status === 0) return 'No pudimos conectar con el servidor.';
    const apiMessage = this.extractApiMessage(error);
    if (apiMessage) {
      return 'La contrasena debe ser distinta a la anterior.';
    }
    if (error.status === 400) return 'Los datos ingresados no son validos.';
    if (error.status === 404) return 'Token invalido o vencido.';
    return 'No pudimos actualizar la contrasena. Intentalo mas tarde.';
  }

  private extractApiMessage(error: HttpErrorResponse): string | null {
    const payload = error.error;
    if (typeof payload === 'string' && payload.trim().length > 0) return payload.trim();
    if (payload && typeof payload.message === 'string' && payload.message.trim().length > 0) {
      return payload.message.trim();
    }
    const topMessage = (error as { message?: string }).message;
    if (typeof topMessage === 'string' && topMessage.trim().length > 0) return topMessage.trim();
    return null;
  }
}
