import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SesionService } from '../../nucleo/sesion.service';
import { mensajeError } from '../../nucleo/api';

/** Inicio de sesión para ciudadanos, trabajadores y administradores. */
@Component({
  selector: 'app-entrar', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <section class="tarjeta-acceso">
    <h1>Entrar</h1>
    @if (motivo === 'sesion') { <p class="nota">Tu sesión terminó. Vuelve a entrar.</p> }
    @if (motivo === 'clave') { <p class="nota exito">Contraseña actualizada. Entra con tu nueva contraseña.</p> }
    <form (ngSubmit)="entrar()" #f="ngForm">
      <label class="campo"><span>Correo</span><input name="email" type="email" [(ngModel)]="email" required autocomplete="email" autofocus></label>
      <label class="campo"><span>Contraseña</span><input name="password" [type]="ver ? 'text' : 'password'" [(ngModel)]="password" required autocomplete="current-password"></label>
      <label class="casilla"><input type="checkbox" [(ngModel)]="ver" name="ver"> Mostrar contraseña</label>
      @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
      <button class="boton ancho" type="submit" [disabled]="ocupado || f.invalid">{{ ocupado ? 'Entrando…' : 'Entrar' }}</button>
    </form>
    <p class="pie-tarjeta"><a routerLink="/recuperar">Olvidé mi contraseña</a></p>
    <p class="pie-tarjeta">¿No tienes cuenta? <a routerLink="/registro">Crea una</a></p>
  </section>`
})
export class EntrarComponent {
  @Input() motivo = '';
  @Input() volver = '';
  email = ''; password = ''; ver = false; ocupado = false; error = '';
  constructor(private sesion: SesionService, private router: Router) {}
  entrar(): void {
    this.ocupado = true; this.error = '';
    this.sesion.entrar(this.email, this.password).subscribe({
      next: () => void this.router.navigateByUrl(this.volver && this.volver.startsWith('/') ? this.volver : this.sesion.inicio),
      error: e => { this.error = mensajeError(e); this.ocupado = false; }
    });
  }
}
