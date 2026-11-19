import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { API, mensajeError } from '../../nucleo/api';
import { Colonia, RespuestaEnlace } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';
import { BuscadorColoniaComponent } from '../../compartido/buscador-colonia.component';
import { EnlaceDemoComponent } from '../../compartido/enlace-demo.component';

/** Registro de ciudadanos: datos de contacto, domicilio y aceptación de documentos legales. */
@Component({
  selector: 'app-registro', standalone: true, imports: [FormsModule, RouterLink, BuscadorColoniaComponent, EnlaceDemoComponent],
  template: `
  <section class="tarjeta-acceso ancha">
    @if (!listo) {
      <h1>Crear cuenta</h1>
      <p>Con tu cuenta puedes enviar reportes y seguir su atención. Tu domicilio sirve para ubicar reportes rápido; no se publica.</p>
      <form (ngSubmit)="registrar()" #f="ngForm">
        <fieldset><legend>Tus datos</legend>
          <div class="rejilla">
            <label class="campo"><span>Nombre completo</span><input name="name" [(ngModel)]="d.name" required minlength="2" maxlength="150" autocomplete="name"></label>
            <label class="campo"><span>Teléfono</span><input name="telefono" [(ngModel)]="d.telefono" required minlength="10" maxlength="20" inputmode="tel" autocomplete="tel"></label>
            <label class="campo"><span>Correo</span><input name="email" type="email" [(ngModel)]="d.email" required maxlength="150" autocomplete="email"></label>
            <label class="campo"><span>Contraseña</span><input name="password" type="password" [(ngModel)]="d.password" required minlength="10" maxlength="128" autocomplete="new-password">
              <small>Mínimo 10 caracteres, con letras y números.</small></label>
          </div>
        </fieldset>
        <fieldset><legend>Tu domicilio</legend>
          <app-buscador-colonia [(colonia)]="colonia" />
          <div class="rejilla">
            <label class="campo"><span>Calle</span><input name="calle" [(ngModel)]="d.calle" required maxlength="150" autocomplete="address-line1"></label>
            <label class="campo"><span>Número exterior</span><input name="numero" [(ngModel)]="d.numeroExterior" required maxlength="20"></label>
            <label class="campo"><span>Código postal</span><input name="cp" [(ngModel)]="d.codigoPostal" required pattern="[0-9]{5}" maxlength="5" inputmode="numeric" autocomplete="postal-code"></label>
          </div>
        </fieldset>
        <fieldset><legend>Documentos</legend>
          <label class="casilla"><input type="checkbox" name="terms" [(ngModel)]="d.terms" required> Acepto los <a routerLink="/legal/terminos" target="_blank">términos y condiciones</a></label>
          <label class="casilla"><input type="checkbox" name="privacy" [(ngModel)]="d.privacy" required> Leí el <a routerLink="/legal/privacidad" target="_blank">aviso de privacidad</a> y acepto el tratamiento de mis datos</label>
          <label class="casilla"><input type="checkbox" name="opcionales" [(ngModel)]="d.opcionales"> Quiero recibir comunicaciones informativas (opcional)</label>
        </fieldset>
        @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
        <button class="boton ancho" type="submit" [disabled]="ocupado || f.invalid || !colonia">{{ ocupado ? 'Creando cuenta…' : 'Crear cuenta' }}</button>
      </form>
      <p class="pie-tarjeta">¿Ya tienes cuenta? <a routerLink="/entrar">Entra</a></p>
    } @else {
      <h1>Tu cuenta está lista</h1>
      <p>Falta un paso: verifica tu correo para poder enviar reportes.</p>
      <app-enlace-demo [respuesta]="verificacion" />
      <a class="boton secundario" routerLink="/mis-reportes">Ir a mis reportes</a>
    }
  </section>`
})
export class RegistroComponent implements OnInit {
  d = { name: '', telefono: '', email: '', password: '', calle: '', numeroExterior: '', codigoPostal: '', terms: false, privacy: false, opcionales: false };
  colonia: Colonia | null = null;
  version = ''; ocupado = false; error = ''; listo = false;
  verificacion: RespuestaEnlace | null = null;
  constructor(private http: HttpClient, private sesion: SesionService) {}
  ngOnInit(): void {
    this.http.get<{ version: string }>(API + '/publico/documentos').subscribe({ next: r => this.version = r.version, error: e => this.error = mensajeError(e) });
  }
  registrar(): void {
    if (!this.colonia) return;
    this.ocupado = true; this.error = '';
    this.sesion.registrar({ ...this.d, coloniaId: this.colonia.id, version: this.version }).subscribe({
      next: r => { this.verificacion = r.verificacion; this.listo = true; },
      error: e => { this.error = mensajeError(e); this.ocupado = false; }
    });
  }
}
