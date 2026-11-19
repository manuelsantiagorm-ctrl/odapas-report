import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subscription, filter, interval, startWith, switchMap, catchError, of } from 'rxjs';
import { SesionService } from './nucleo/sesion.service';
import { AvisosService } from './compartido/avisos.service';
import { API } from './nucleo/api';

interface Enlace { ruta: string; texto: string; exacta?: boolean; }

/** Marco de la aplicación: encabezado, menú según el rol y área de contenido. */
@Component({
  selector: 'app-raiz', standalone: true, imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  noLeidas = 0;
  menuAbierto = false;
  private subs = new Subscription();

  readonly ciudadano: Enlace[] = [
    { ruta: '/mis-reportes', texto: 'Mis reportes' },
    { ruta: '/nuevo-reporte', texto: 'Reportar' }
  ];
  readonly trabajador: Enlace[] = [{ ruta: '/trabajo', texto: 'Mis órdenes' }];
  readonly admin: Enlace[] = [
    { ruta: '/admin', texto: 'Panel', exacta: true },
    { ruta: '/admin/reportes', texto: 'Reportes' },
    { ruta: '/admin/ordenes', texto: 'Órdenes' },
    { ruta: '/admin/personal', texto: 'Personal' },
    { ruta: '/admin/zonas', texto: 'Zonas' },
    { ruta: '/admin/arco', texto: 'Privacidad' },
    { ruta: '/admin/bitacora', texto: 'Bitácora' }
  ];

  constructor(public sesion: SesionService, public avisos: AvisosService, private http: HttpClient, private router: Router) {}

  /** Sesión actual (null si nadie ha entrado). */
  get s() { return this.sesion.sesionActual; }

  ngOnInit(): void {
    // Revisa los avisos sin leer cada minuto mientras haya sesión.
    this.subs.add(interval(60000).pipe(startWith(0), filter(() => this.sesion.activa),
      switchMap(() => this.http.get<{ noLeidas: number }>(API + '/notificaciones/contador').pipe(catchError(() => of({ noLeidas: 0 })))))
      .subscribe(r => this.noLeidas = r.noLeidas));
    this.subs.add(this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      this.menuAbierto = false;
      if (this.sesion.activa) this.http.get<{ noLeidas: number }>(API + '/notificaciones/contador').subscribe({ next: r => this.noLeidas = r.noLeidas, error: () => {} });
    }));
  }
  get iniciales(): string {
    return (this.sesion.usuario?.name ?? '?').split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();
  }
  salir(): void { this.sesion.salir(); void this.router.navigate(['/']); }
  ngOnDestroy(): void { this.subs.unsubscribe(); }
}
