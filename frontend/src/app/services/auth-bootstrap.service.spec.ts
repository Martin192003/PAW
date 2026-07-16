import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthBootstrapService } from './auth-bootstrap.service';
import { AuthStateService } from './auth-state.service';
import { UserService } from './user.service';
import { User } from '../models/user.model';

// auth-bootstrap.service.ts no tenia ningun test (ver AUDITORIA_ENTREGA_FINAL.md
// punto 25) pese a decidir, en cada carga de la SPA, si la sesion guardada sigue
// siendo valida.
describe('AuthBootstrapService', () => {
  let service: AuthBootstrapService;
  let authState: {
    getToken: ReturnType<typeof vi.fn>;
    getUser: ReturnType<typeof vi.fn>;
    clearSession: ReturnType<typeof vi.fn>;
    updateUser: ReturnType<typeof vi.fn>;
  };
  let userService: { getCurrentUserById: ReturnType<typeof vi.fn> };

  const storedUser = { userId: 'user-1' } as User;
  const freshUser = { userId: 'user-1', username: 'refreshed' } as User;

  beforeEach(() => {
    authState = {
      getToken: vi.fn(),
      getUser: vi.fn(),
      clearSession: vi.fn(),
      updateUser: vi.fn(),
    };
    userService = { getCurrentUserById: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthBootstrapService,
        { provide: AuthStateService, useValue: authState },
        { provide: UserService, useValue: userService },
      ],
    });

    service = TestBed.inject(AuthBootstrapService);
  });

  // async/await + firstValueFrom en vez de un callback done(): con los tipos globals
  // de Vitest de este proyecto, el parámetro de it(...) es un TestContext (no un
  // done-callback estilo Jasmine/Mocha), así que done() no es invocable.
  it('resolves immediately without calling the backend when there is no stored token', async () => {
    authState.getToken.mockReturnValue(null);

    const result = await firstValueFrom(service.initialize());

    expect(result).toBe(true);
    expect(userService.getCurrentUserById).not.toHaveBeenCalled();
  });

  it('clears the session when there is a token but no usable stored user', async () => {
    authState.getToken.mockReturnValue('some-token');
    authState.getUser.mockReturnValue(null);

    const result = await firstValueFrom(service.initialize());

    expect(result).toBe(true);
    expect(authState.clearSession).toHaveBeenCalled();
    expect(userService.getCurrentUserById).not.toHaveBeenCalled();
  });

  it('refreshes the user from the backend when the session looks valid', async () => {
    authState.getToken.mockReturnValue('some-token');
    authState.getUser.mockReturnValue(storedUser);
    userService.getCurrentUserById.mockReturnValue(of(freshUser));

    const result = await firstValueFrom(service.initialize());

    expect(result).toBe(true);
    expect(userService.getCurrentUserById).toHaveBeenCalledWith('user-1');
    expect(authState.updateUser).toHaveBeenCalledWith(freshUser);
    expect(authState.clearSession).not.toHaveBeenCalled();
  });

  it('clears the session when the backend rejects the stored token', async () => {
    authState.getToken.mockReturnValue('expired-token');
    authState.getUser.mockReturnValue(storedUser);
    userService.getCurrentUserById.mockReturnValue(throwError(() => new Error('401')));

    const result = await firstValueFrom(service.initialize());

    expect(result).toBe(true);
    expect(authState.clearSession).toHaveBeenCalled();
    expect(authState.updateUser).not.toHaveBeenCalled();
  });
});
