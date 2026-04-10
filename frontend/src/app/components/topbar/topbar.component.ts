import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthStateService } from '../../services/auth-state.service';


@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent {
  get isLoggedIn(): boolean {
    return !!this.authState.getToken();
  }

  get manageClubsHref(): string {
    return '/admin/activity';
  }

  get profileHref(): string {
    const userId = this.authState.getUser()?.userId;
    return userId ? `/profile?userId=${encodeURIComponent(userId)}` : '/profile';
  }

  constructor(private readonly authState: AuthStateService) {}
}
