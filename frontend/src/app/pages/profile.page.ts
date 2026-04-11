import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { User } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';
import { UserService } from '../services/user.service';

type EditableProfileField = 'username' | 'name' | 'surname' | 'email' | 'cellphone';

const FIELD_LABELS: Record<EditableProfileField, string> = {
  username: 'Username',
  name: 'Name',
  surname: 'Surname',
  email: 'Email',
  cellphone: 'Cellphone'
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
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.css'
})
export class ProfilePage implements OnInit {
  user: User | null = null;
  activeField: EditableProfileField | null = null;
  draftValue = '';
  errorMessage = '';
  saving = false;

  constructor(
    private readonly authState: AuthStateService,
    private readonly userService: UserService,
    private readonly router: Router
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

  get activeFieldLabel(): string {
    return this.activeField ? FIELD_LABELS[this.activeField] : '';
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
      this.errorMessage = 'El campo no puede quedar vacío';
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
    const fallback = 'No se pudo guardar el cambio';

    if (error.status === 401 || error.status === 403) {
      return 'Tu sesion no es valida. Volve a iniciar sesion.';
    }

    if (error.status >= 500) {
      return 'Error interno del servidor al actualizar el perfil';
    }

    if (!error?.error) {
      return fallback;
    }

    if (typeof error.error === 'string') {
      const trimmed = error.error.trim();

      if (trimmed.startsWith('{')) {
        try {
          const parsed = JSON.parse(trimmed) as { message?: string };
          if (typeof parsed.message === 'string' && parsed.message.trim()) {
            return parsed.message;
          }
        } catch {
          return fallback;
        }
      }

      if (trimmed.startsWith('<!DOCTYPE html') || trimmed.startsWith('<html')) {
        return fallback;
      }

      return trimmed || fallback;
    }

    if (typeof error.error.message === 'string') {
      return error.error.message;
    }

    return fallback;
  }
}

