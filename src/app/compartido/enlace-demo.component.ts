import { Component, Input } from '@angular/core';
import { RespuestaEnlace } from '../nucleo/modelos';

/**
 * Este proyecto aún no envía correos reales. En modo demostración la API devuelve el
 * enlace que llegaría por correo, y aquí se muestra para poder continuar.
 */
@Component({
  selector: 'app-enlace-demo', standalone: true,
  template: `
  @if (respuesta) {
    <div class="nota" role="status">
      <p>{{ respuesta.mensaje }}</p>
      @if (respuesta.enlaceDemo) {
        <p class="demo">Modo demostración: este es el enlace que llegaría por correo.</p>
        <a class="boton secundario" [href]="enlace">Abrir enlace</a>
        <button type="button" class="enlace" (click)="copiar()">{{ copiado ? 'Copiado' : 'Copiar enlace' }}</button>
      }
    </div>
  }`
})
export class EnlaceDemoComponent {
  @Input() respuesta: RespuestaEnlace | null = null;
  copiado = false;
  /** El servidor arma el enlace con localhost; se adapta a la dirección con la que se abrió la app (IP o dominio). */
  get enlace(): string {
    try { const u = new URL(this.respuesta?.enlaceDemo ?? ''); return location.origin + u.pathname + u.search; }
    catch { return this.respuesta?.enlaceDemo ?? ''; }
  }
  copiar(): void {
    if (!this.respuesta?.enlaceDemo) return;
    navigator.clipboard?.writeText(this.enlace).then(() => this.copiado = true).catch(() => {});
  }
}
