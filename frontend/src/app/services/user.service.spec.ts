import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserService, UserUpdateResult } from './user.service';
import { ApiMessageResponse, LoginResponse, User } from '../models/user.model';


describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const fakeUser: User = {
    userId: 'user-1',
    username: 'demo',
    name: 'Demo',
    surname: 'User',
    email: 'demo@example.com',
    cellphone: '1122334455',
    role: 'USER',
    verified: true,
    createdAt: '2026-01-01T00:00:00Z',
    locale: 'es',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('register posts the create payload to /users', () => {
    const payload = {
      username: 'demo', name: 'Demo', surname: 'User', email: 'demo@example.com',
      password: 'secret123', cellphone: '1122334455', locale: 'es',
    };

    service.register(payload).subscribe();

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith('/users'));
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('login sends Basic credentials, mints the JWT and fetches the canonical /users/{id}', () => {
    let result: LoginResponse | undefined;
    service.login({ usernameOrEmail: '  demo@example.com  ', password: 'secret123' }).subscribe(res => {
      result = res;
    });

    const jwt = `header.${btoa(JSON.stringify({ sub: 'user-1' }))}.sig`;

    const mintReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/bookings'));
    expect(mintReq.request.headers.get('Authorization')).toBe(`Basic ${btoa('demo@example.com:secret123')}`);
    mintReq.flush([], { headers: { 'Paw-Auth-Token': jwt } });

    const userReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/users/user-1'));
    expect(userReq.request.headers.get('Authorization')).toBe(`Bearer ${jwt}`);
    userReq.flush(fakeUser);

    expect(result).toEqual({ token: jwt, user: fakeUser });
  });

  it('verifyByToken calls the canonical /users/{id}/verification sub-resource', () => {
    let result: LoginResponse | ApiMessageResponse | undefined;
    service.verifyByToken('user-1', 'verif-token').subscribe(res => { result = res; });

    const verificationReq = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/users/user-1/verification'));
    expect(verificationReq.request.body).toEqual({ token: 'verif-token' });
    verificationReq.flush({ message: 'verified' });

    expect(result).toEqual({ message: 'verified' });
  });

  it('verifyByToken passes through an ApiMessageResponse without resolving a user (already verified)', () => {
    let result: LoginResponse | ApiMessageResponse | undefined;
    service.verifyByToken('user-1', 'verif-token').subscribe(res => { result = res; });

    const verificationReq = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/users/user-1/verification'));
    verificationReq.flush({ message: 'already_verified' });

    httpMock.expectNone(request => request.method === 'GET');
    expect(result).toEqual({ message: 'already_verified' });
  });

  it('verifyByToken maps a session response to a LoginResponse for auto-login', () => {
    let result: LoginResponse | ApiMessageResponse | undefined;
    service.verifyByToken('user-1', 'verif-token').subscribe(res => { result = res; });

    const verificationReq = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/users/user-1/verification'));
    verificationReq.flush({ user: fakeUser, accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 });

    expect(result).toEqual({ token: 'jwt-token', user: fakeUser });
  });

  it('resendVerification posts the correct challengeType', () => {
    service.resendVerification({ usernameOrEmail: 'demo@example.com' }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith('/auth/challenges'));
    expect(req.request.body).toEqual({ challengeType: 'email_verification', usernameOrEmail: 'demo@example.com' });
    req.flush({ message: 'sent' });
  });

  it('requestPasswordReset posts the correct challengeType and maps the response to void', () => {
    let completed = false;
    service.requestPasswordReset('demo@example.com').subscribe(value => {
      expect(value).toBeUndefined();
      completed = true;
    });

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith('/auth/challenges'));
    expect(req.request.body).toEqual({ challengeType: 'password_reset', email: 'demo@example.com' });
    req.flush({});

    expect(completed).toBe(true);
  });

  it('resetPassword resolves a LoginResponse from the new token', () => {
    let result: LoginResponse | undefined;
    service.resetPassword('reset-token', 'newPass123', 'newPass123').subscribe(res => { result = res; });

    const resetReq = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith('/users/credentials/password'));
    expect(resetReq.request.body).toEqual({
      token: 'reset-token',
      newPassword: 'newPass123',
      confirmationPassword: 'newPass123',
    });
    resetReq.flush({
      user: fakeUser,
      accessToken: 'jwt-token-3',
      tokenType: 'Bearer',
      expiresIn: 3600,
    });

    expect(result?.token).toBe('jwt-token-3');
    expect(result?.user).toEqual(fakeUser);
  });

  it('changePasswordAuthenticated PUTs to the credentials sub-resource', () => {
    service.changePasswordAuthenticated('user-1', 'newPass123', 'newPass123').subscribe();

    const req = httpMock.expectOne(
      request => request.method === 'PUT' && request.url.endsWith('/users/user-1/credentials/password')
    );
    expect(req.request.body).toEqual({ newPassword: 'newPass123', confirmationPassword: 'newPass123' });
    req.flush(null);
  });

  it('updateUser maps {user, accessToken} into {user, token}', () => {
    let result: UserUpdateResult | undefined;
    service.updateUser('user-1', { username: 'nuevo' }).subscribe(res => { result = res; });

    const req = httpMock.expectOne(request => request.method === 'PATCH' && request.url.endsWith('/users/user-1'));
    req.flush({ user: fakeUser, accessToken: 'jwt-token-4' });

    expect(result).toEqual({ user: fakeUser, token: 'jwt-token-4' });
  });

  it('updateUser maps a missing accessToken to a null token (no email/username change)', () => {
    let result: UserUpdateResult | undefined;
    service.updateUser('user-1', { cellphone: '1155554444' }).subscribe(res => { result = res; });

    const req = httpMock.expectOne(request => request.method === 'PATCH' && request.url.endsWith('/users/user-1'));
    req.flush({ user: fakeUser });

    expect(result).toEqual({ user: fakeUser, token: null });
  });
});
