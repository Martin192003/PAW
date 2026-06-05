import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthStateService } from '../../services/auth-state.service';

export type SideBarSection =
  | 'profile'
  | 'clubs'
  | 'clubsReservations'
  | 'myReservations'
  | 'reviews';

@Component({
  selector: 'app-side-bar',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
  templateUrl: './account-sidebar.component.html'
})
export class SideBarComponent {
  @Input() activeSection: SideBarSection = 'profile';

  constructor(
    private readonly authState: AuthStateService,
    private readonly router: Router,
  ) {}

  readonly items: Array<{
    key: SideBarSection;
    labelKey: string;
    route?: string;
  }> = [
    { key: 'profile', labelKey: 'sidebar.profile', route: '/profile' },
    { key: 'clubs', labelKey: 'sidebar.clubs', route: '/admin/activity' },
    { key: 'clubsReservations', labelKey: 'sidebar.clubsReservations', route: '/admin/bookings' },
    { key: 'myReservations', labelKey: 'sidebar.myReservations', route: '/bookings' },
    { key: 'reviews', labelKey: 'sidebar.reviews', route: '/profile/reviews'  }
  ];

  isActive(section: SideBarSection): boolean {
    return this.activeSection === section;
  }

  logout(): void {
    this.authState.clearSession();
    void this.router.navigate(['/clubs'], { replaceUrl: true });
  }
}
