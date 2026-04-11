import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserCreateRequest } from '../models/user.model';
import { UserService } from '../services/user.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.page.html',
})
export class RegisterPage {
  submitting = false;
  errorMessage = '';

  registerForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required]],
      name: ['', [Validators.required]],
      surname: ['', [Validators.required]],
      password: ['', [Validators.required]],
      passwordRepeat: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      cellphone: ['', [Validators.required]],
    });
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    if (this.registerForm.invalid || this.submitting) return;

    if (this.registerForm.value.password !== this.registerForm.value.passwordRepeat) {
      this.errorMessage = 'Las contrasenas no coinciden.';
      return;
    }

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
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  private getLocale(): string {
    return (typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'es';
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 409) {
      return 'Ya existe un usuario con ese nombre o email.';
    }
    if (error.status === 400) {
      return this.extractServerMessage(error.error) ?? 'Revisa los datos ingresados.';
    }
    return this.extractServerMessage(error.error) ?? 'No se pudo crear el usuario. Intentalo de nuevo.';
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
