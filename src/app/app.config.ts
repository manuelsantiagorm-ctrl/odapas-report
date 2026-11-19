import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es-MX';
import { rutas } from './rutas';
import { interceptorSesion } from './nucleo/interceptor-sesion';

registerLocaleData(localeEs);

export const configuracionApp: ApplicationConfig = {
  providers: [
    provideRouter(rutas, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([interceptorSesion])),
    { provide: LOCALE_ID, useValue: 'es-MX' }
  ]
};
