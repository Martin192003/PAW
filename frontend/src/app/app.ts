import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TopbarComponent } from './components/topbar/topbar.component';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from './services/i18n.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, TopbarComponent, TranslateModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = signal('Clubs App');
  showTopbar = false;

  constructor(
    private readonly router: Router,
    private readonly i18n: I18nService
  ) {
    const initialPath = this.normalizePath(this.router.url);
    if (initialPath !== '/') {
      this.updateTopbarVisibility(initialPath);
    }

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.updateTopbarVisibility(event.urlAfterRedirects);
      }
    });
  }

  private updateTopbarVisibility(url: string): void {
    const path = this.normalizePath(url);
    this.showTopbar = ![
      '/login',
      '/register',
      '/forgot-password',
      '/reset-password',
      '/verify-mail',
      '/verify-account'
    ].includes(path);
  }

  private normalizePath(url: string): string {
    return (url || '').split('?')[0];
  }
}
