import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { API } from './api';
import { Rol, Sesion, Usuario } from './modelos';

const CLAVE = 'odapas_session_v2';

/** Guarda la sesión del usuario (token + datos) mientras la pestaña esté abierta. */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly sujeto = new BehaviorSubject<Sesion | null>(this.leer());
  readonly sesion$ = this.sujeto.asObservable();

  constructor(private readonly http: HttpClient) {}

  private leer(): Sesion | null {
    try {
      const s = JSON.parse(sessionStorage.getItem(CLAVE) || 'null') as Sesion | null;
      return s && typeof s.token === 'string' && s.user ? { ...s, user: { ...s.user, roles: s.user.roles ?? [] } } : null;
    } catch { return null; }
  }
  get sesionActual(): Sesion | null { return this.sujeto.value; }
  get token(): string | null { return this.sujeto.value?.token ?? null; }
  get usuario(): Usuario | null { return this.sujeto.value?.user ?? null; }
  get activa(): boolean { return this.token !== null; }
  tiene(rol: Rol): boolean { return this.usuario?.roles?.includes(rol) === true || (rol === 'administrador' && this.usuario?.admin === true); }

  /** Pantalla de inicio según el perfil principal del usuario. */
  get inicio(): string {
    if (this.tiene('administrador')) return '/admin';
    if (this.tiene('trabajador')) return '/trabajo';
    return '/mis-reportes';
  }

  guardar(sesion: Sesion): void {
    this.sujeto.next(sesion);
    try { sessionStorage.setItem(CLAVE, JSON.stringify(sesion)); } catch { /* almacenamiento bloqueado */ }
  }
  actualizarUsuario(cambios: Partial<Usuario>): void {
    const s = this.sujeto.value;
    if (s) this.guardar({ ...s, user: { ...s.user, ...cambios } });
  }

  entrar(email: string, password: string): Observable<Sesion> {
    return this.http.post<Sesion>(API + '/auth/login', { email, password }).pipe(tap(s => this.guardar(s)));
  }
  registrar(datos: Record<string, unknown>): Observable<Sesion & { verificacion: { mensaje: string; enlaceDemo?: string } }> {
    return this.http.post<Sesion & { verificacion: { mensaje: string; enlaceDemo?: string } }>(API + '/auth/registro', datos)
      .pipe(tap(s => this.guardar(s)));
  }
  cerrarLocal(): void {
    this.sujeto.next(null);
    try { sessionStorage.removeItem(CLAVE); } catch { /* sin almacenamiento */ }
  }
  salir(): void {
    // El interceptor agrega el token al suscribirse, por eso se cierra la sesión local justo después.
    if (this.token) this.http.post(API + '/auth/logout', {}).subscribe({ error: () => {} });
    this.cerrarLocal();
  }
}
