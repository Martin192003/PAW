import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { RegisterPage } from './register.page';
import { I18nService } from '../services/i18n.service';
import { UserService } from '../services/user.service';

describe('RegisterPage', () => {
  let router: { navigate: ReturnType<typeof vi.fn> };
  let userService: { register: ReturnType<typeof vi.fn> };
  let component: RegisterPage;

  beforeEach(() => {
    router = { navigate: vi.fn().mockResolvedValue(true) };
    userService = { register: vi.fn() };

    TestBed.configureTestingModule({
      imports: [RegisterPage, TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}), paramMap: convertToParamMap({}) },
            queryParamMap: of(convertToParamMap({})),
            paramMap: of(convertToParamMap({})),
          },
        },
        { provide: Router, useValue: router as unknown as Router },
        { provide: UserService, useValue: userService as unknown as UserService },
        { provide: I18nService, useValue: { instant: (key: string, params?: Record<string, unknown>) => params ? `${key}:${JSON.stringify(params)}` : key } },
      ],
    });

    component = TestBed.createComponent(RegisterPage).componentInstance;
  });

  function fillValidForm(): void {
    component.registerForm.setValue({
      username: ' user ',
      name: ' Name ',
      surname: ' Surname ',
      password: '123456',
      passwordRepeat: '123456',
      email: 'user@example.com',
      cellphone: ' 11 1234-5678 ',
    });
  }

  it('required fields block submit', () => {
    component.onSubmit();

    expect(component.registerForm.invalid).toBe(true);
    expect(userService.register).not.toHaveBeenCalled();
    expect(component.showError('username')).toBe(true);
    expect(component.getError('username')).toBe('register.errors.required');
  });

  it('password min length is enforced', () => {
    fillValidForm();
    component.registerForm.get('password')?.setValue('12345');

    expect(component.registerForm.get('password')?.hasError('minlength')).toBe(true);
    expect(component.registerForm.invalid).toBe(true);
  });

  it('phone pattern and max length are enforced', () => {
    fillValidForm();
    const cellphone = component.registerForm.get('cellphone');

    cellphone?.setValue('phone');
    expect(cellphone?.hasError('pattern')).toBe(true);

    cellphone?.setValue('1'.repeat(31));
    expect(cellphone?.hasError('maxlength')).toBe(true);
  });

  it('live password mismatch is validated while typing', () => {
    fillValidForm();

    component.registerForm.get('passwordRepeat')?.setValue('different');
    component.registerForm.get('passwordRepeat')?.markAsDirty();

    expect(component.registerForm.hasError('passwordMismatch')).toBe(true);
    expect(component.showError('passwordRepeat')).toBe(true);
    expect(component.getError('passwordRepeat')).toBe('register.errors.passwordMismatch');
  });

  it('valid submit trims payload fields', () => {
    userService.register.mockReturnValue(of({}));
    fillValidForm();

    component.onSubmit();

    expect(userService.register).toHaveBeenCalledWith({
      username: 'user',
      name: 'Name',
      surname: 'Surname',
      email: 'user@example.com',
      password: '123456',
      cellphone: '11 1234-5678',
      locale: expect.any(String),
    });
    expect(router.navigate).toHaveBeenCalledWith(['/verify-mail'], {
      queryParams: { email: 'user@example.com' },
    });
  });

  it('409 conflict uses conflict error message', () => {
    userService.register.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, statusText: 'Conflict' }))
    );
    fillValidForm();

    component.onSubmit();

    expect(component.errorMessage).toBe('register.errors.conflict');
  });

  it('400 error uses server message when present', () => {
    userService.register.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        error: { message: 'Server validation failed' },
      }))
    );
    fillValidForm();

    component.onSubmit();

    expect(component.errorMessage).toBe('Server validation failed');
  });
});
