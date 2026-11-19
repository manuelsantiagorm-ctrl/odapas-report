import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { API, mensajeError } from '../../nucleo/api';
import { Sesion } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';

/** El trabajador abre la invitación que generó el administrador y crea su contraseña. */
@Component({
  selector: 'app-activar', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <section class="tarjeta-acceso">
    <h1>Activa tu cuenta de trabajador</h1>
    <p>El administrador de ODAPAS te dio de alta. Crea tu contraseña para ver las órdenes de tu zona.</p>
    @if (!token) { <p class="error-campo">La invitación está incompleta. Pide al administrador que la reenvíe.</p> }
    @else {
      <form (ngSubmit)="activar()" #f="ngForm">
        <label class="campo"><span>Contraseña</span><input name="p1" type="password" [(ngModel)]="p1" required minlength="10" maxlength="128" autocomplete="new-password"><small>Mínimo 10 caracteres, con letras y números.</small></label>
        <label class="campo"><span>Repite la contraseña</span><input name="p2" type="password" [(ngModel)]="p2" required autocomplete="new-password"></label>
        <label class="casilla"><input type="checkbox" name="t" [(ngModel)]="terms" required> Acepto los <a routerLink="/legal/terminos" target="_blank">términos y condiciones</a></label>
        <label class="casilla"><input type="checkbox" name="p" [(ngModel)]="privacy" required> Leí el <a routerLink="/legal/privacidad" target="_blank">aviso de privacidad</a></label>
        @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
        <button class="boton ancho" [disabled]="ocupado || f.invalid">Activar cuenta</button>
      </form>
    }
  </section>`
})
export class ActivarComponent implements OnInit {
  @Input() token = '';
  p1 = ''; p2 = ''; terms = false; privacy = false; version = ''; ocupado = false; error = '';
  constructor(private http: HttpClient, private sesion: SesionService, private router: Router) {}
  ngOnInit(): void { this.http.get<{ version: string }>(API + '/publico/documentos').subscribe(r => this.version = r.version); }
  activar(): void {
    if (this.p1 !== this.p2) { this.error = 'Las contraseñas no coinciden.'; return; }
    this.ocupado = true; this.error = '';
    this.http.post<Sesion>(API + '/auth/activar', { token: this.token, password: this.p1, terms: this.terms, privacy: this.privacy, version: this.version }).subscribe({
      next: s => { this.sesion.guardar(s); void this.router.navigate(['/trabajo']); },
      error: e => { this.error = mensajeError(e); this.ocupado = false; }
    });
  }
}
