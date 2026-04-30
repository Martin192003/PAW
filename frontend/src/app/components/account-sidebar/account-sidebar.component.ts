import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

export type SideBarSection =
  | 'profile'
  | 'clubs'
  | 'clubsReservations'
  | 'myReservations'
  | 'reviews';

@Component({
  selector: 'app-side-bar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './account-sidebar.component.html'
})
export class SideBarComponent {
  @Input() activeSection: SideBarSection = 'profile';

  readonly items: Array<{
    key: SideBarSection;
    label: string;
    route?: string;
  }> = [
    { key: 'profile', label: 'My Profile', route: '/profile' },
    { key: 'clubs', label: 'My Clubs', route: '/admin/activity' },
    { key: 'clubsReservations', label: 'Clubs Reservations', route: '/admin/bookings' },
    { key: 'myReservations', label: 'My Reservations', route: '/bookings' },
    { key: 'reviews', label: 'Reviews', route: '/profile/reviews' }
  ];

  isActive(section: SideBarSection): boolean {
    return this.activeSection === section;
  }
}
