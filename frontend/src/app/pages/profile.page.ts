import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { User } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { UserService } from '../services/user.service';
import { SideBarComponent } from '../components/account-sidebar/account-sidebar.component';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../services/i18n.service';

type EditableProfileField = 'username' | 'name' | 'surname' | 'email' | 'cellphone';

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
  imports: [CommonModule, FormsModule, SideBarComponent, TranslateModule],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.css'
})
export class ProfilePage implements OnInit {
  user: User | null = null;
  activeField: EditableProfileField | null = null;
  draftValue = '';
  errorMessage = '';
  passwordResetError = '';
  saving = false;
  creatingPasswordResetToken = false;

  constructor(
    private readonly authState: AuthStateService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly i18n: I18nService
  ) {}

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
    this.draftValue = this.user[field] ?? '';
    this.errorMessage = '';
  }

  goToPasswordReset(): void {
    if (!this.user || this.creatingPasswordResetToken) {
      return;
    }

    this.creatingPasswordResetToken = true;
    this.passwordResetError = '';

    this.userService.createPasswordResetToken(this.user.userId).pipe(
      finalize(() => {
        this.creatingPasswordResetToken = false;
      })
    ).subscribe({
      next: ({ token }) => {
        this.router.navigate(['/reset-password'], { queryParams: { token } });
      },
      error: (error: HttpErrorResponse) => {
        this.passwordResetError = this.extractPasswordResetError(error);
      }
    });
  }

  closeEditor(): void {
    if (this.saving) {
      return;
    }

    this.activeField = null;
    this.draftValue = '';
    this.errorMessage = '';
  }

  saveEditor(): void {
    if (!this.user || !this.activeField) {
      return;
    }

    const nextValue = this.draftValue.trim();
    if (!nextValue) {
      this.errorMessage = this.i18n.instant('profile.errors.emptyField');
      return;
    }

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

    this.userService.updateUser(userId, payload).subscribe({
      next: ({ user: updatedUser, token }) => {
        if (token) {
          this.authState.updateToken(token);
        }

        this.authState.updateUser(updatedUser);
        this.user = updatedUser;
        this.closeEditor();
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
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

  private extractPasswordResetError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return this.i18n.instant('profile.errors.connection');
    }

    if (error.status === 401 || error.status === 403) {
      return this.i18n.instant('profile.errors.invalidSession');
    }

    if (error.status >= 500) {
      return this.i18n.instant('profile.errors.resetPasswordServer');
    }

    return this.extractErrorMessage(error) || this.i18n.instant('profile.errors.resetPassword');
  }
}
