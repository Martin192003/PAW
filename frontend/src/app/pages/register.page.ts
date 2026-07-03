import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserCreateRequest } from '../models/user.model';
import { UserService } from '../services/user.service';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

const PASSWORD_MIN_LENGTH = 6;
const PHONE_MAX_LENGTH = 30;
const PHONE_PATTERN = /^(?=.*\d)[0-9+()\-\s]+$/;

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './register.page.html',
})
export class RegisterPage {
  submitting = false;
  errorMessage = '';

  registerForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router,
    private i18n: I18nService,
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required]],
      name: ['', [Validators.required]],
      surname: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]],
      passwordRepeat: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      cellphone: ['', [Validators.required, Validators.maxLength(PHONE_MAX_LENGTH), Validators.pattern(PHONE_PATTERN)]],
    }, { validators: this.passwordsMatchValidator() });
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    if (this.registerForm.invalid || this.submitting) return;

    const payload: UserCreateRequest = {
      username: this.registerForm.value.username.trim(),
      name: this.registerForm.value.name.trim(),
      surname: this.registerForm.value.surname.trim(),
      email: this.registerForm.value.email.trim(),
      password: this.registerForm.value.password,
      cellphone: this.registerForm.value.cellphone.trim(),
      locale: this.getLocale(),
    };

    this.submitting = true;
    this.errorMessage = '';

    this.userService.register(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/verify-mail'], { queryParams: { email: payload.email } });
      },
      error: (error: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.resolveErrorMessage(error);
      },
    });
  }

  showError(field: string): boolean {
    const control = this.registerForm.get(field);
    if (!control || !(control.dirty || control.touched)) return false;
    return control.invalid || (field === 'passwordRepeat' && this.registerForm.hasError('passwordMismatch'));
  }

  getError(field: string): string {
    const control = this.registerForm.get(field);
    if (!control) return '';

    if (control.errors?.['required']) return this.i18n.instant('register.errors.required');
    if (control.errors?.['email']) return this.i18n.instant('register.errors.email');
    if (control.errors?.['minlength']) {
      return this.i18n.instant('register.errors.minlength', {
        min: control.errors['minlength'].requiredLength,
      });
    }
    if (control.errors?.['maxlength']) {
      return this.i18n.instant('register.errors.maxlength', {
        max: control.errors['maxlength'].requiredLength,
      });
    }
    if (control.errors?.['pattern'] && field === 'cellphone') {
      return this.i18n.instant('register.errors.phonePattern');
    }
    if (field === 'passwordRepeat' && this.registerForm.hasError('passwordMismatch')) {
      return this.i18n.instant('register.errors.passwordMismatch');
    }

    return this.i18n.instant('register.errors.invalid');
  }

  private getLocale(): string {
    return (typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'es';
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 409) {
      return this.i18n.instant('register.errors.conflict');
    }
    if (error.status === 400) {
      return this.extractServerMessage(error.error) ?? this.i18n.instant('register.errors.invalidData');
    }
    return this.extractServerMessage(error.error) ?? this.i18n.instant('register.errors.registerFailed');
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

  private passwordsMatchValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const password = group.get('password')?.value;
      const passwordRepeat = group.get('passwordRepeat')?.value;

      if (!password || !passwordRepeat) return null;

      return password === passwordRepeat ? null : { passwordMismatch: true };
    };
  }
}
