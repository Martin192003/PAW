import { Component, OnDestroy, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { UserService } from '../services/user.service';
import { AuthStateService } from '../services/auth-state.service';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

// Antes este formulario pedia 8 caracteres mientras que el de registro
// (register.page.ts) pedia 6: se unifica al mismo minimo en toda la app.
const PASSWORD_MIN_LENGTH = 6;

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

  // Errores generales de la request (red, 5xx, token invalido/vencido: no son
  // atribuibles a un campo puntual del formulario), se siguen mostrando como
  // cartel arriba del form. Los que si son de un campo van por el mismo
  // patron de "errores congelados" que register.page.ts / club-handler.page.ts.
  forgotError = '';
  resetError = '';
  resetCompleted = false;

  forgotForm: FormGroup;
  resetForm: FormGroup;

  forgotSubmittedAttempted = false;
  forgotFrozenErrors: { [key: string]: boolean } = {};
  forgotFrozenErrorMessages: { [key: string]: string } = {};
  forgotServerFieldErrors: { [key: string]: string } = {};

  resetSubmittedAttempted = false;
  resetFrozenErrors: { [key: string]: boolean } = {};
  resetFrozenErrorMessages: { [key: string]: string } = {};
  resetServerFieldErrors: { [key: string]: string } = {};

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
      newPassword: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]],
      confirmationPassword: ['', [Validators.required]],
    }, { validators: this.passwordsMatchValidator() });
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.token = params.get('token');
      const loggedUserEmail = this.authState.getUser()?.email ?? '';
      this.useLoggedUserEmail = !this.token && params.get('source') === 'profile' && loggedUserEmail.trim().length > 0;
      this.showResetModal = false;
      this.resetError = '';
      this.resetCompleted = false;
    });
  }

  // Si ya tenes sesion iniciada y entraste desde /profile, no tiene sentido
  // mandarte un mail y hacerte tocar un link para volver a esta misma pantalla:
  // se muestra directamente el formulario de nueva contraseña, identificando
  // al usuario por el JWT de la sesion actual en vez de por un token de mail.
  // El flujo de "te mando un mail" solo aplica cuando NO hay sesion (login).
  get showResetForm(): boolean {
    return !!this.token || this.useLoggedUserEmail;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submitForgot(): void {
    this.forgotSubmittedAttempted = true;
    this.forgotForm.markAllAsTouched();

    this.forgotFrozenErrors = {};
    this.forgotFrozenErrorMessages = {};
    const hasError = this.hasCurrentForgotError('email');
    this.forgotFrozenErrors['email'] = hasError;
    if (hasError) {
      this.forgotFrozenErrorMessages['email'] = this.currentForgotErrorMessage('email');
    }

    if (this.forgotForm.invalid || this.submittingForgot) return;

    this.submittingForgot = true;
    this.forgotError = '';
    this.forgotServerFieldErrors = {};
    this.showForgotModal = false;

    const email = (this.forgotForm.value.email ?? '').trim();

    this.userService.requestPasswordReset(email).pipe(
      finalize(() => {
        this.submittingForgot = false;
      })
    ).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.showForgotModal = true;
          this.cdr.detectChanges();
        });
      },
      error: (error: HttpErrorResponse) => {
        this.ngZone.run(() => {
          this.applyForgotError(error);
          this.cdr.detectChanges();
        });
      },
    });
  }

  submitReset(): void {
    this.resetSubmittedAttempted = true;
    this.resetForm.markAllAsTouched();

    this.resetFrozenErrors = {};
    this.resetFrozenErrorMessages = {};
    (['newPassword', 'confirmationPassword'] as const).forEach((field) => {
      const hasError = this.hasCurrentResetError(field);
      this.resetFrozenErrors[field] = hasError;
      if (hasError) {
        this.resetFrozenErrorMessages[field] = this.currentResetErrorMessage(field);
      }
    });

    if (this.resetForm.invalid || this.submittingReset) return;

    const newPassword = this.resetForm.value.newPassword;
    const confirmationPassword = this.resetForm.value.confirmationPassword;

    this.submittingReset = true;
    this.resetError = '';
    this.resetServerFieldErrors = {};
    this.resetCompleted = false;
    this.showResetModal = false;

    if (this.token) {
      this.submitTokenReset(this.token, newPassword, confirmationPassword);
      return;
    }

    // Sin token: solo se llega hasta acá si useLoggedUserEmail es true
    // (ver showResetForm), es decir, cambio de contraseña autenticado.
    this.submitAuthenticatedChange(newPassword, confirmationPassword);
  }

  private submitTokenReset(token: string, newPassword: string, confirmationPassword: string): void {
    this.userService.resetPassword(token, newPassword, confirmationPassword).pipe(
      finalize(() => {
        this.submittingReset = false;
      })
    ).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.authState.setSession(response.token, response.user, false);
          this.router.navigate(['/clubs']);
          this.cdr.detectChanges();
        });
      },
      error: (error: HttpErrorResponse) => {
        this.ngZone.run(() => {
          this.applyResetError(error);
          this.cdr.detectChanges();
        });
      },
    });
  }

  private submitAuthenticatedChange(newPassword: string, confirmationPassword: string): void {
    this.userService.changePasswordAuthenticated(newPassword, confirmationPassword).pipe(
      finalize(() => {
        this.submittingReset = false;
      })
    ).subscribe({
      next: () => {
        this.ngZone.run(() => {
          // A diferencia del flujo con token, aca la sesion actual sigue
          // siendo valida (el JWT no cambia), asi que no hace falta un nuevo
          // login: solo se confirma el cambio con el mismo modal de éxito.
          this.resetCompleted = true;
          this.cdr.detectChanges();
        });
      },
      error: (error: HttpErrorResponse) => {
        this.ngZone.run(() => {
          this.applyAuthenticatedChangeError(error);
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

  showForgotError(field: string): boolean {
    if (this.forgotServerFieldErrors[field]) {
      return true;
    }

    if (!this.forgotSubmittedAttempted) return false;

    if (this.forgotFrozenErrors[field] !== undefined) {
      return this.forgotFrozenErrors[field];
    }

    return this.hasCurrentForgotError(field);
  }

  getForgotError(field: string): string {
    if (this.forgotServerFieldErrors[field]) {
      return this.forgotServerFieldErrors[field];
    }

    if (this.forgotFrozenErrorMessages[field]) {
      return this.forgotFrozenErrorMessages[field];
    }

    return this.currentForgotErrorMessage(field);
  }

  showResetError(field: 'newPassword' | 'confirmationPassword'): boolean {
    if (this.resetServerFieldErrors[field]) {
      return true;
    }

    if (!this.resetSubmittedAttempted) return false;

    if (this.resetFrozenErrors[field] !== undefined) {
      return this.resetFrozenErrors[field];
    }

    return this.hasCurrentResetError(field);
  }

  getResetError(field: 'newPassword' | 'confirmationPassword'): string {
    if (this.resetServerFieldErrors[field]) {
      return this.resetServerFieldErrors[field];
    }

    if (this.resetFrozenErrorMessages[field]) {
      return this.resetFrozenErrorMessages[field];
    }

    return this.currentResetErrorMessage(field);
  }

  confirmForgotModal(): void {
    this.showForgotModal = false;
    this.router.navigate([this.useLoggedUserEmail ? '/profile' : '/login']);
  }

  confirmResetModal(): void {
    this.showResetModal = false;
    this.resetCompleted = false;
    this.router.navigate([this.useLoggedUserEmail ? '/profile' : '/clubs']);
  }

  private hasCurrentForgotError(field: string): boolean {
    const control = this.forgotForm.get(field);
    return !!(control && control.invalid);
  }

  private currentForgotErrorMessage(field: string): string {
    const control = this.forgotForm.get(field);
    if (!control || !control.errors) return '';

    if (control.errors['required']) return this.i18n.instant('passwordReset.errors.required');
    if (control.errors['email']) return this.i18n.instant('passwordReset.errors.email');
    return this.i18n.instant('passwordReset.errors.invalid');
  }

  private hasCurrentResetError(field: 'newPassword' | 'confirmationPassword'): boolean {
    const control = this.resetForm.get(field);
    if (!control) return false;
    return control.invalid || (field === 'confirmationPassword' && this.resetForm.hasError('passwordMismatch'));
  }

  private currentResetErrorMessage(field: 'newPassword' | 'confirmationPassword'): string {
    const control = this.resetForm.get(field);
    if (!control) return '';

    if (control.errors?.['required']) return this.i18n.instant('passwordReset.errors.required');
    if (control.errors?.['minlength']) {
      return this.i18n.instant('passwordReset.errors.minlength', {
        min: control.errors['minlength'].requiredLength,
      });
    }
    if (field === 'confirmationPassword' && this.resetForm.hasError('passwordMismatch')) {
      return this.i18n.instant('passwordReset.errors.passwordsDontMatch');
    }

    return this.i18n.instant('passwordReset.errors.invalid');
  }

  private passwordsMatchValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const newPassword = group.get('newPassword')?.value;
      const confirmationPassword = group.get('confirmationPassword')?.value;

      if (!newPassword || !confirmationPassword) return null;

      return newPassword === confirmationPassword ? null : { passwordMismatch: true };
    };
  }

  // El 400 de "email requerido" solo puede pasar si se manda un email vacio
  // directo a la API (el frontend ya lo exige antes de submitir), pero se
  // mapea igual al campo por si acaso, en vez de mostrar un cartel generico.
  private applyForgotError(error: HttpErrorResponse): void {
    if (error.status === 0) {
      this.forgotError = this.i18n.instant('passwordReset.errors.network');
      return;
    }

    if (error.status === 400) {
      this.forgotServerFieldErrors['email'] = this.i18n.instant('passwordReset.errors.invalidEmail');
      return;
    }

    this.forgotError = this.i18n.instant('passwordReset.errors.emailSendFailed');
  }

  private applyResetError(error: HttpErrorResponse): void {
    if (error.status === 0) {
      this.resetError = this.i18n.instant('passwordReset.errors.network');
      return;
    }

    // El token vencido/invalido no es un campo editable del formulario (viene
    // del link del mail), asi que se sigue mostrando como cartel arriba del
    // form en vez de error de campo.
    if (error.status === 404) {
      this.resetError = this.i18n.instant('passwordReset.errors.invalidToken');
      return;
    }

    if (error.status === 400) {
      const message = this.extractServerMessage(error.error);

      if (message === 'password_confirmation_mismatch') {
        this.resetServerFieldErrors['confirmationPassword'] = this.i18n.instant('passwordReset.errors.passwordsDontMatch');
        return;
      }

      if (message === 'password_required' || message === 'token_required') {
        this.resetError = this.i18n.instant('passwordReset.errors.invalidData');
        return;
      }

      // Unico otro 400 posible a esta altura: ValidationException de
      // "la contraseña no puede ser igual a la anterior" (UserServiceImpl).
      this.resetServerFieldErrors['newPassword'] = this.i18n.instant('passwordReset.errors.passwordUnchanged');
      return;
    }

    this.resetError = this.i18n.instant('passwordReset.errors.updateFailed');
  }

  // A diferencia de /auth/password-resets, /auth/password-changes devuelve
  // "password_unchanged" como mensaje literal (ver AuthRestController), asi
  // que no hace falta adivinar cual es el unico 400 restante.
  private applyAuthenticatedChangeError(error: HttpErrorResponse): void {
    if (error.status === 0) {
      this.resetError = this.i18n.instant('passwordReset.errors.network');
      return;
    }

    if (error.status === 401) {
      this.resetError = this.i18n.instant('passwordReset.errors.sessionExpired');
      return;
    }

    if (error.status === 400) {
      const message = this.extractServerMessage(error.error);

      if (message === 'password_confirmation_mismatch') {
        this.resetServerFieldErrors['confirmationPassword'] = this.i18n.instant('passwordReset.errors.passwordsDontMatch');
        return;
      }

      if (message === 'password_unchanged') {
        this.resetServerFieldErrors['newPassword'] = this.i18n.instant('passwordReset.errors.passwordUnchanged');
        return;
      }

      this.resetError = this.i18n.instant('passwordReset.errors.invalidData');
      return;
    }

    this.resetError = this.i18n.instant('passwordReset.errors.updateFailed');
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
