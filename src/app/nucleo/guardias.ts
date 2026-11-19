import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from './modelos';
import { SesionService } from './sesion.service';

/** Solo deja pasar si hay sesión iniciada. */
export const requiereSesion: CanActivateFn = (_r, estado) => {
  const s = inject(SesionService);
  return s.activa ? true : inject(Router).createUrlTree(['/entrar'], { queryParams: { volver: estado.url } });
};

/** Solo deja pasar si el usuario tiene el rol indicado. */
export function requiereRol(rol: Rol): CanActivateFn {
  return (_r, estado) => {
    const s = inject(SesionService);
    const router = inject(Router);
    if (!s.activa) return router.createUrlTree(['/entrar'], { queryParams: { volver: estado.url } });
    return s.tiene(rol) ? true : router.createUrlTree([s.inicio]);
  };
}

/** Si ya hay sesión, la pantalla de entrar lleva directo al panel. */
export const soloSinSesion: CanActivateFn = () => {
  const s = inject(SesionService);
  return s.activa ? inject(Router).createUrlTree([s.inicio]) : true;
};
