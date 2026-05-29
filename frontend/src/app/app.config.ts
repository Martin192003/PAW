import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER } from '@angular/core';
import { apiErrorInterceptor } from './services/api-error.interceptor';
import { AuthBootstrapService } from './services/auth-bootstrap.service';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
// @ts-ignore
import { authInterceptor } from './services/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor])),
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: (authBootstrap: AuthBootstrapService) => () => authBootstrap.initialize(),
      deps: [AuthBootstrapService]
    }
  ]
};
