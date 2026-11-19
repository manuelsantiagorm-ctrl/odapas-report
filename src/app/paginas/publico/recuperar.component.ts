import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { API, mensajeError } from '../../nucleo/api';
import { RespuestaEnlace } from '../../nucleo/modelos';
import { EnlaceDemoComponent } from '../../compartido/enlace-demo.component';

/** Solicitar un enlace para crear una contraseña nueva. */
@Component({
  selector: 'app-recuperar', standalone: true, imports: [FormsModule, RouterLink, EnlaceDemoComponent],
  template: `
  <section class="tarjeta-acceso">
    <h1>Recuperar contraseña</h1>
    @if (!respuesta) {
      <p>Escribe el correo de tu cuenta. Te enviaremos un enlace válido por 30 minutos.</p>
      <form (ngSubmit)="enviar()" #f="ngForm">
        <label class="campo"><span>Correo</span><input name="email" type="email" [(ngModel)]="email" required autocomplete="email"></label>
        @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
        <button class="boton ancho" [disabled]="ocupado || f.invalid">Enviar enlace</button>
      </form>
    } @else { <app-enlace-demo [respuesta]="respuesta" /> }
    <p class="pie-tarjeta"><a routerLink="/entrar">Volver a entrar</a></p>
  </section>`
})
export class RecuperarComponent {
  email = ''; ocupado = false; error = ''; respuesta: RespuestaEnlace | null = null;
  constructor(private http: HttpClient) {}
  enviar(): void {
    this.ocupado = true; this.error = '';
    this.http.post<RespuestaEnlace>(API + '/auth/recuperar', { email: this.email }).subscribe({
      next: r => this.respuesta = r, error: e => { this.error = mensajeError(e); this.ocupado = false; }
    });
  }
}
