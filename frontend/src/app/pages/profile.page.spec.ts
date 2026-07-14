import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ProfilePage } from './profile.page';
import { AuthStateService } from '../services/auth-state.service';
import { I18nService } from '../services/i18n.service';
import { UserService } from '../services/user.service';

describe('ProfilePage', () => {
  const user = {
    userId: 'user-1',
    username: 'currentUser',
    name: 'Current',
    surname: 'User',
    email: 'current@example.com',
    cellphone: '123456',
    role: 'USER',
    verified: true,
    createdAt: '2026-01-01T00:00:00Z',
    locale: 'es',
  };

  let authState: { getUser: ReturnType<typeof vi.fn>; updateUser: ReturnType<typeof vi.fn>; updateToken: ReturnType<typeof vi.fn> };
  let userService: { getCurrentUserById: ReturnType<typeof vi.fn>; updateUser: ReturnType<typeof vi.fn> };
  let component: ProfilePage;

  beforeEach(() => {
    authState = {
      getUser: vi.fn().mockReturnValue(user),
      updateUser: vi.fn(),
      updateToken: vi.fn(),
    };
    userService = {
      getCurrentUserById: vi.fn().mockReturnValue(of(user)),
      updateUser: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [ProfilePage, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: AuthStateService, useValue: authState as unknown as AuthStateService },
        { provide: UserService, useValue: userService as unknown as UserService },
        { provide: I18nService, useValue: { instant: (key: string) => key } },
      ],
    });

    component = TestBed.createComponent(ProfilePage).componentInstance;
    component.ngOnInit();
  });

  it('renders username conflict as soon as the backend responds', () => {
    userService.updateUser.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.openEditor('username');
    component.editorForm.get('draftValue')?.setValue('existingUser');

    component.saveEditor();

    expect(component.getError()).toBe('profile.errors.usernameConflict');
    expect(component.showError()).toBe(true);
  });

  it('renders email conflict as soon as the backend responds', () => {
    userService.updateUser.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.openEditor('email');
    component.editorForm.get('draftValue')?.setValue('used@example.com');

    component.saveEditor();

    expect(component.getError()).toBe('profile.errors.emailConflict');
    expect(component.showError()).toBe(true);
  });

  it('filters letters from cellphone input while editing', () => {
    component.openEditor('cellphone');
    const input = document.createElement('input');
    input.value = 'abc+54 11-test(22)';

    component.onDraftInput({ target: input } as unknown as Event);

    expect(input.value).toBe('+54 11-(22)');
    expect(component.editorForm.get('draftValue')?.value).toBe('+54 11-(22)');
  });
});
