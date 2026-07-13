import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { User } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { UserService } from '../services/user.service';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

type EditableProfileField = 'username' | 'name' | 'surname' | 'email' | 'cellphone';

const PHONE_MAX_LENGTH = 30;
const PHONE_PATTERN = /^(?=.*\d)[0-9+()\-\s]+$/;

const FIELD_I18N: Record<EditableProfileField, { labelKey: string; placeholderKey: string; ariaLabelKey: string }> = {
  username: {
    labelKey: 'profile.fields.username',
    placeholderKey: 'profile.placeholders.username',
    ariaLabelKey: 'profile.editUsername'
  },
  name: {
    labelKey: 'profile.fields.name',
    placeholderKey: 'profile.placeholders.name',
    ariaLabelKey: 'profile.editName'
  },
  surname: {
    labelKey: 'profile.fields.surname',
    placeholderKey: 'profile.placeholders.surname',
    ariaLabelKey: 'profile.editSurname'
  },
  email: {
    labelKey: 'profile.fields.email',
    placeholderKey: 'profile.placeholders.email',
    ariaLabelKey: 'profile.editEmail'
  },
  cellphone: {
    labelKey: 'profile.fields.cellphone',
    placeholderKey: 'profile.placeholders.cellphone',
    ariaLabelKey: 'profile.editPhone'
  }
};

const FIELD_TYPES: Record<EditableProfileField, string> = {
  username: 'text',
  name: 'text',
  surname: 'text',
  email: 'email',
  cellphone: 'text'
};

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SideBarComponent, TranslateModule],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.css'
})
export class ProfilePage implements OnInit {
  user: User | null = null;
  activeField: EditableProfileField | null = null;
  errorMessage = '';
  saving = false;

  // Mismo patron de "errores congelados" que register.page.ts / club-new.page.ts:
  // el error no se muestra en vivo mientras se escribe, solo despues de un
  // intento de guardar, y una vez mostrado queda fijo hasta el proximo intento.
  submittedAttempted = false;
  frozenError = false;
  frozenErrorMessage = '';
  serverFieldError = '';

  readonly editorForm: FormGroup;

  constructor(
    private readonly authState: AuthStateService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly i18n: I18nService,
    private readonly fb: FormBuilder,
  ) {
    this.editorForm = this.fb.group({
      draftValue: [''],
    });
  }

  ngOnInit(): void {
    const cachedUser = this.authState.getUser();
    this.user = cachedUser;

    const userId = cachedUser?.userId;
    if (!userId) {
      return;
    }

    this.userService.getUserById(userId).subscribe({
      next: (freshUser) => {
        this.user = freshUser;
        this.authState.updateUser(freshUser);
      },
      error: () => {
        // Mantengo el cache local si el backend no responde; el profile sigue usable.
      }
    });
  }

  get activeFieldLabelKey(): string {
    return this.activeField ? FIELD_I18N[this.activeField].labelKey : '';
  }

  get activeFieldPlaceholderKey(): string {
    return this.activeField ? FIELD_I18N[this.activeField].placeholderKey : '';
  }

  get activeFieldType(): string {
    return this.activeField ? FIELD_TYPES[this.activeField] : 'text';
  }

  goBack(): void {
    this.router.navigate(['/clubs']);
  }

  openEditor(field: EditableProfileField): void {
    if (!this.user) {
      return;
    }

    this.activeField = field;
    this.errorMessage = '';
    this.submittedAttempted = false;
    this.frozenError = false;
    this.frozenErrorMessage = '';
    this.serverFieldError = '';

    const control = this.editorForm.get('draftValue')!;
    control.setValidators(this.validatorsFor(field));
    control.setValue(this.user[field] ?? '');
    control.markAsUntouched();
    control.updateValueAndValidity({ emitEvent: false });
  }

  goToPasswordReset(): void {
    if (!this.user) {
      return;
    }

    this.router.navigate(['/reset-password'], { queryParams: { source: 'profile' } });
  }

  closeEditor(): void {
    if (this.saving) {
      return;
    }

    this.activeField = null;
    this.errorMessage = '';
    this.submittedAttempted = false;
    this.frozenError = false;
    this.frozenErrorMessage = '';
    this.serverFieldError = '';
    this.editorForm.reset({ draftValue: '' });
  }

  showError(): boolean {
    if (this.serverFieldError) {
      return true;
    }

    if (!this.submittedAttempted) return false;

    return this.frozenError;
  }

  getError(): string {
    if (this.serverFieldError) {
      return this.serverFieldError;
    }

    if (this.frozenErrorMessage) {
      return this.frozenErrorMessage;
    }

    return this.currentErrorMessage();
  }

  saveEditor(): void {
    if (!this.user || !this.activeField) {
      return;
    }

    this.submittedAttempted = true;
    const control = this.editorForm.get('draftValue');
    control?.markAsTouched();
    control?.updateValueAndValidity({ onlySelf: true, emitEvent: false });

    this.frozenError = this.hasCurrentError();
    this.frozenErrorMessage = this.frozenError ? this.currentErrorMessage() : '';

    if (this.editorForm.invalid || this.saving) return;

    const nextValue = (control?.value ?? '').trim();
    const userId = this.user.userId;
    const payload = {
      username: this.activeField === 'username' ? nextValue : this.user.username,
      name: this.activeField === 'name' ? nextValue : this.user.name,
      surname: this.activeField === 'surname' ? nextValue : this.user.surname,
      email: this.activeField === 'email' ? nextValue : this.user.email,
      cellphone: this.activeField === 'cellphone' ? nextValue : this.user.cellphone,
      locale: this.user.locale,
    };

    this.saving = true;
    this.errorMessage = '';
    this.serverFieldError = '';

    this.userService.updateUser(userId, payload).subscribe({
      next: ({ user: updatedUser, token }) => {
        if (token) {
          this.authState.updateToken(token);
        }

        this.authState.updateUser(updatedUser);
        this.user = updatedUser;
        this.saving = false;
        this.closeEditor();
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        if (this.applyServerFieldError(error)) {
          return;
        }
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.activeField) {
      this.closeEditor();
    }
  }

  private validatorsFor(field: EditableProfileField): ValidatorFn[] {
    switch (field) {
      case 'email':
        return [Validators.required, Validators.email];
      case 'cellphone':
        return [Validators.required, Validators.maxLength(PHONE_MAX_LENGTH), Validators.pattern(PHONE_PATTERN)];
      default:
        return [Validators.required];
    }
  }

  private hasCurrentError(): boolean {
    const control = this.editorForm.get('draftValue');
    return !!(control && control.invalid);
  }

  private currentErrorMessage(): string {
    const control = this.editorForm.get('draftValue');
    if (!control || !control.errors) return '';

    if (control.errors['required']) return this.i18n.instant('profile.errors.required');
    if (control.errors['email']) return this.i18n.instant('profile.errors.email');
    if (control.errors['maxlength']) {
      return this.i18n.instant('profile.errors.maxlength', {
        max: control.errors['maxlength'].requiredLength,
      });
    }
    if (control.errors['pattern'] && this.activeField === 'cellphone') {
      return this.i18n.instant('profile.errors.phonePattern');
    }

    return this.i18n.instant('profile.errors.invalid');
  }

  // El backend no distingue el campo en conflicto en /users/{id} (a diferencia
  // de /users en el alta), pero como acá se edita un solo campo por vez, un
  // 409 solo puede deberse al campo que se está editando (username o email
  // son los únicos con restricción de unicidad).
  private applyServerFieldError(error: HttpErrorResponse): boolean {
    if (error.status !== 409) {
      return false;
    }

    if (this.activeField !== 'username' && this.activeField !== 'email') {
      return false;
    }

    this.serverFieldError = this.activeField === 'email'
      ? this.i18n.instant('profile.errors.emailConflict')
      : this.i18n.instant('profile.errors.usernameConflict');
    this.editorForm.get('draftValue')?.markAsTouched();
    return true;
  }

  private extractErrorMessage(error: HttpErrorResponse): string {
    const fallback = this.i18n.instant('profile.errors.saveFailed');

    if (error.status === 401 || error.status === 403) {
      return this.i18n.instant('profile.errors.invalidSession');
    }

    if (error.status >= 500) {
      return this.i18n.instant('profile.errors.serverUpdate');
    }

    if (!error?.error) {
      return fallback;
    }

    return fallback;
  }
}
