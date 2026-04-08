import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, TopbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = signal('Clubs App');
  showTopbar = true;

  constructor(private readonly router: Router) {
    this.updateTopbarVisibility(this.router.url);

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.updateTopbarVisibility(event.urlAfterRedirects);
      }
    });
  }

  private updateTopbarVisibility(url: string): void {
    const path = (url || '').split('?')[0];
    this.showTopbar = ![
      '/login',
      '/register',
      '/verify-mail',
      '/verify-account'
    ].includes(path);
  }
}
