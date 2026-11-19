import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API } from './api';
import { SesionService } from './sesion.service';

/** Agrega el token a cada petición a la API y cierra la sesión si el servidor responde 401. */
export const interceptorSesion: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API + '/')) return next(req);
  const sesion = inject(SesionService);
  const router = inject(Router);
  const peticion = sesion.token ? req.clone({ setHeaders: { Authorization: 'Bearer ' + sesion.token } }) : req;
  return next(peticion).pipe(catchError((e: HttpErrorResponse) => {
    if (e.status === 401 && sesion.activa && !req.url.includes('/auth/')) {
      sesion.cerrarLocal();
      void router.navigate(['/entrar'], { queryParams: { motivo: 'sesion' } });
    }
    return throwError(() => e);
  }));
};
