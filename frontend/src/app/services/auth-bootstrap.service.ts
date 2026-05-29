import { Injectable } from '@angular/core';
import { catchError, map, of } from 'rxjs';
import { AuthStateService } from './auth-state.service';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
export class AuthBootstrapService {
  constructor(
    private readonly authState: AuthStateService,
    private readonly userService: UserService
  ) {}

  initialize() {
    const token = this.authState.getToken();
    if (!token) {
      return of(true);
    }

    return this.userService.getMe().pipe(
      map((user) => {
        this.authState.updateUser(user);
        return true;
      }),
      catchError(() => {
        this.authState.clearSession();
        return of(true);
      })
    );
  }
}
