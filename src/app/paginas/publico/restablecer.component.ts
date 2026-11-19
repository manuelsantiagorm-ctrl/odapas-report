import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { API, mensajeError } from '../../nucleo/api';
import { SesionService } from '../../nucleo/sesion.service';

/** Se abre desde el enlace de recuperación: /restablecer?token=... */
@Component({
  selector: 'app-restablecer', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <section class="tarjeta-acceso">
    <h1>Nueva contraseña</h1>
    @if (!token) { <p class="error-campo">El enlace está incompleto. <a routerLink="/recuperar">Pide otro</a>.</p> }
    @else {
      <form (ngSubmit)="guardar()" #f="ngForm">
        <label class="campo"><span>Nueva contraseña</span><input name="p1" type="password" [(ngModel)]="p1" required minlength="10" maxlength="128" autocomplete="new-password"><small>Mínimo 10 caracteres, con letras y números.</small></label>
        <label class="campo"><span>Repite la contraseña</span><input name="p2" type="password" [(ngModel)]="p2" required autocomplete="new-password"></label>
        @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
        <button class="boton ancho" [disabled]="ocupado || f.invalid">Guardar contraseña</button>
      </form>
      <p class="pie-tarjeta">Por seguridad se cerrarán tus sesiones abiertas.</p>
    }
  </section>`
})
export class RestablecerComponent {
  @Input() token = '';
  p1 = ''; p2 = ''; ocupado = false; error = '';
  constructor(private http: HttpClient, private router: Router, private sesion: SesionService) {}
  guardar(): void {
    if (this.p1 !== this.p2) { this.error = 'Las contraseñas no coinciden.'; return; }
    this.ocupado = true; this.error = '';
    this.http.post(API + '/auth/restablecer', { token: this.token, password: this.p1 }).subscribe({
      next: () => { this.sesion.cerrarLocal(); void this.router.navigate(['/entrar'], { queryParams: { motivo: 'clave' } }); },
      error: e => { this.error = mensajeError(e); this.ocupado = false; }
    });
  }
}
