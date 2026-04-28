import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthStateService } from '../../services/auth-state.service';
import { ClubService } from '../../services/club.service';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';


@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent implements OnInit, OnDestroy {
  private readonly subscriptions = new Subscription();
  hasClubs = false;
  isOnManagePage = false;
  isOnHome = false;

  get isLoggedIn(): boolean {
    return !!this.authState.getToken();
  }

  readonly manageClubsRoute = '/admin/activity';
  readonly addClubRoute = '/clubs/new';

  get showManageClubs(): boolean {
    return this.isLoggedIn && this.hasClubs && this.isOnHome;
  }

  get showAddClub(): boolean {
    return !this.showManageClubs;
  }

  get profileQueryParams(): { userId: string } | undefined {
    const userId = this.authState.getUser()?.userId;
    return userId ? { userId } : undefined;
  }

  constructor(
    private readonly authState: AuthStateService,
    private readonly clubService: ClubService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.syncHasClubs();
    this.updateRouteState();

    this.subscriptions.add(
      this.authState.isLoggedIn$.subscribe(() => {
        this.syncHasClubs();
      })
    );

    this.subscriptions.add(
      this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
        this.updateRouteState();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private updateRouteState(): void {
    this.isOnManagePage = this.router.url.startsWith(this.manageClubsRoute);
    this.isOnHome = this.router.url === '/clubs' || this.router.url === '/';
  }

  private syncHasClubs(): void {
    if (!this.isLoggedIn) {
      this.hasClubs = false;
      return;
    }

    const userId = this.authState.getUser()?.userId;
    if (!userId) {
      this.hasClubs = false;
      return;
    }

    this.clubService.getClubsByUser(userId, 0, 1).subscribe({
      next: (response) => {
        const total = response.totalElements ?? response.content?.length ?? 0;
        this.hasClubs = total > 0;
      },
      error: () => {
        this.hasClubs = false;
      },
    });
  }
}
