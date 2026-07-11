import {
  APP_INITIALIZER,
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors, HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { apiErrorInterceptor } from './services/api-error.interceptor';
import { AuthBootstrapService } from './services/auth-bootstrap.service';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { authInterceptor } from './services/auth.interceptor';

export class CustomTranslateLoader implements TranslateLoader {
  constructor(private http: HttpClient) {}

  getTranslation(lang: string): Observable<any> {
    const url = typeof document === 'undefined'
      ? `assets/i18n/${lang}.json`
      : new URL(`assets/i18n/${lang}.json`, document.baseURI).toString();
    return this.http.get(url);
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor, apiErrorInterceptor])),
    importProvidersFrom(
      TranslateModule.forRoot({
        fallbackLang: 'es',
        loader: { provide: TranslateLoader, useClass: CustomTranslateLoader, deps: [HttpClient] }
      })
    ),
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: (authBootstrap: AuthBootstrapService) => () => authBootstrap.initialize(),
      deps: [AuthBootstrapService]
    }
  ]
};
