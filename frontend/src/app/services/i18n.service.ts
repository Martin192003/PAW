import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private readonly supportedLanguages = ['es', 'en'];
  private readonly defaultLanguage = 'es';
  private currentLanguageSubject = new BehaviorSubject<string>(this.defaultLanguage);

  public currentLanguage$: Observable<string> = this.currentLanguageSubject.asObservable();

  constructor(
    private readonly translate: TranslateService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {
    this.initializeLanguage();

    if (isPlatformBrowser(this.platformId) && typeof window !== 'undefined' && 'addEventListener' in window) {
      window.addEventListener('languagechange', () => {
        const newLang = this.getBrowserLanguage();
        const lang = this.supportedLanguages.includes(newLang) ? newLang : this.defaultLanguage;
        this.setLanguage(lang);
      });
    }
  }

  private initializeLanguage(): void {
    this.translate.setDefaultLang(this.defaultLanguage);
    this.translate.addLangs(this.supportedLanguages);

    if (!isPlatformBrowser(this.platformId)) {
      this.currentLanguageSubject.next(this.defaultLanguage);
      return;
    }

    const storedLanguage = localStorage.getItem('language')?.toLowerCase() ?? '';
    const browserLanguage = this.getBrowserLanguage();
    const candidate = storedLanguage || browserLanguage;
    const language = this.supportedLanguages.includes(candidate) ? candidate : this.defaultLanguage;
    this.setLanguage(language);
  }

  private getBrowserLanguage(): string {
    if (!isPlatformBrowser(this.platformId)) {
      return this.defaultLanguage;
    }
    const browserLang = navigator.language || (navigator as any).userLanguage;
    return browserLang.split('-')[0].toLowerCase();
  }

  public setLanguage(language: string): void {
    if (this.supportedLanguages.includes(language)) {
      this.translate.use(language);
      this.currentLanguageSubject.next(language);
      if (isPlatformBrowser(this.platformId)) {
        localStorage.setItem('language', language);
      }
    }
  }

  public getLanguage(): string {
    return this.currentLanguageSubject.value;
  }

  public getTranslation(key: string, params?: any): Observable<string> {
    return this.translate.get(key, params);
  }

  public instant(key: string, params?: any): string {
    return this.translate.instant(key, params);
  }
}






