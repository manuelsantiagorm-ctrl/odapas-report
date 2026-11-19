import { Component, Input, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { API, mensajeError } from '../../nucleo/api';
import { SesionService } from '../../nucleo/sesion.service';

/** Se abre desde el enlace del correo: /verificar?token=... */
@Component({
  selector: 'app-verificar', standalone: true, imports: [RouterLink],
  template: `
  <section class="tarjeta-acceso">
    <h1>Verificar correo</h1>
    @if (estado === 'cargando') { <p>Verificando…</p> }
    @if (estado === 'ok') {
      <p class="nota exito">{{ mensaje }}</p>
      <a class="boton" [routerLink]="sesion.activa ? '/nuevo-reporte' : '/entrar'">{{ sesion.activa ? 'Hacer mi primer reporte' : 'Entrar' }}</a>
    }
    @if (estado === 'error') {
      <p class="error-campo">{{ mensaje }}</p>
      <p>Si ya tienes sesión, puedes pedir un enlace nuevo desde <a routerLink="/cuenta">Mi cuenta</a>.</p>
    }
  </section>`
})
export class VerificarComponent implements OnInit {
  @Input() token = '';
  estado: 'cargando' | 'ok' | 'error' = 'cargando'; mensaje = '';
  constructor(private http: HttpClient, public sesion: SesionService) {}
  ngOnInit(): void {
    if (!this.token) { this.estado = 'error'; this.mensaje = 'El enlace está incompleto.'; return; }
    this.http.post<{ mensaje: string }>(API + '/auth/verificar', { token: this.token }).subscribe({
      next: r => { this.estado = 'ok'; this.mensaje = r.mensaje; this.sesion.actualizarUsuario({ correoVerificado: true }); },
      error: e => { this.estado = 'error'; this.mensaje = mensajeError(e); }
    });
  }
}
