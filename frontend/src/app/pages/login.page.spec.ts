import { HttpErrorResponse } from '@angular/common/http';
import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginPage } from './login.page';
import { AuthStateService } from '../services/auth-state.service';
import { I18nService } from '../services/i18n.service';
import { UserService } from '../services/user.service';

describe('LoginPage', () => {
  let queryParams: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let router: {
    navigate: ReturnType<typeof vi.fn>;
    navigateByUrl: ReturnType<typeof vi.fn>;
    createUrlTree: ReturnType<typeof vi.fn>;
    serializeUrl: ReturnType<typeof vi.fn>;
    events: Subject<unknown>;
  };
  let authState: {
    isLoggedIn: ReturnType<typeof vi.fn>;
    setSession: ReturnType<typeof vi.fn>;
  };
  let userService: { login: ReturnType<typeof vi.fn> };

  function createComponent(loggedIn = false, initialQueryParams: Record<string, string> = {}): LoginPage {
    queryParams = new BehaviorSubject(convertToParamMap(initialQueryParams));
    router = {
      navigate: vi.fn().mockResolvedValue(true),
      navigateByUrl: vi.fn().mockResolvedValue(true),

      createUrlTree: vi.fn().mockReturnValue({} as any),
      serializeUrl: vi.fn().mockReturnValue(''),
      events: new Subject<unknown>(),
    };
    authState = {
      isLoggedIn: vi.fn().mockReturnValue(loggedIn),
      setSession: vi.fn(),
    };
    userService = { login: vi.fn() };

    TestBed.configureTestingModule({
      imports: [LoginPage, TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: queryParams.value },
            queryParamMap: queryParams.asObservable(),
          },
        },
        { provide: Router, useValue: router as unknown as Router },
        { provide: AuthStateService, useValue: authState as unknown as AuthStateService },
        { provide: UserService, useValue: userService as unknown as UserService },
        { provide: I18nService, useValue: { instant: (key: string) => key } },
        { provide: Location, useValue: { back: vi.fn() } },
      ],
    });

    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('required fields block submit', () => {
    const component = createComponent();

    component.onSubmit();

    expect(component.loginForm.invalid).toBe(true);
    expect(userService.login).not.toHaveBeenCalled();
    expect(component.showError('usernameOrEmail')).toBe(true);
    expect(component.showError('password')).toBe(true);
  });

  it('successful login stores session and navigates', () => {
    const component = createComponent();
    const user = { userId: 'user-1', username: 'user' };
    userService.login.mockReturnValue(of({ token: 'token-123', user }));

    component.loginForm.setValue({
      usernameOrEmail: ' user@example.com ',
      password: 'secret',
      rememberMe: true,
    });
    component.onSubmit();

    expect(userService.login).toHaveBeenCalledWith({
      usernameOrEmail: 'user@example.com',
      password: 'secret',
    });
    expect(authState.setSession).toHaveBeenCalledWith('token-123', user, true);
    expect(router.navigate).toHaveBeenCalledWith(['/clubs'], { replaceUrl: true });
    expect(component.submitting).toBe(false);
  });

  it('logged-in user redirects', () => {
    createComponent(true);

    expect(router.navigate).toHaveBeenCalledWith(['/clubs'], { replaceUrl: true });
    expect(userService.login).not.toHaveBeenCalled();
  });

  it('401 invalid credentials sets invalid credentials message', () => {
    const component = createComponent();
    userService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }))
    );

    component.loginForm.setValue({
      usernameOrEmail: 'user',
      password: 'bad-password',
      rememberMe: false,
    });
    component.onSubmit();

    expect(component.invalidCredentials).toBe(true);
    expect(component.errorMessage).toBe('login.errors.invalidCredentials');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('403 unverified redirects to verify flow', () => {
    const component = createComponent();
    userService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' }))
    );

    component.loginForm.setValue({
      usernameOrEmail: ' pending-user ',
      password: 'secret',
      rememberMe: false,
    });
    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/verify-mail'], {
      queryParams: {
        usernameOrEmail: 'pending-user',
        autoResend: 'true',
      },
    });
  });

  it('query param error shows invalid credentials', () => {
    const component = createComponent();

    queryParams.next(convertToParamMap({ error: 'true' }));

    expect(component.invalidCredentials).toBe(true);
    expect(component.errorMessage).toBe('login.errors.invalidCredentials');
  });

  it('redirects to returnUrl after a successful login when a guard set one', () => {
    const component = createComponent(false, { returnUrl: '/clubs/new' });
    userService.login.mockReturnValue(of({ token: 'token-123', user: { userId: 'user-1' } }));

    component.loginForm.setValue({
      usernameOrEmail: 'user@example.com',
      password: 'secret',
      rememberMe: false,
    });
    component.onSubmit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/clubs/new', { replaceUrl: true });
    expect(router.navigate).not.toHaveBeenCalledWith(['/clubs'], { replaceUrl: true });
  });

  it('sends an already logged-in user straight to returnUrl instead of /clubs', () => {
    createComponent(true, { returnUrl: '/clubs/new' });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/clubs/new', { replaceUrl: true });
  });

  it('ignores an external returnUrl and falls back to /clubs', () => {
    const component = createComponent(false, { returnUrl: '//evil.example.com' });
    userService.login.mockReturnValue(of({ token: 'token-123', user: { userId: 'user-1' } }));

    component.loginForm.setValue({
      usernameOrEmail: 'user@example.com',
      password: 'secret',
      rememberMe: false,
    });
    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/clubs'], { replaceUrl: true });
  });
});
