import { Component, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ETAPAS, ESTADOS, etapaDe } from '../nucleo/etiquetas';
import { PasoHistorial } from '../nucleo/modelos';

/**
 * Seguimiento del reporte dibujado como una tubería: cada tramo se llena de agua
 * conforme avanza la atención. Debajo se lista el historial completo.
 */
@Component({
  selector: 'app-tuberia', standalone: true, imports: [DatePipe],
  template: `
  @if (etapa >= 0) {
    <ol class="tuberia" [style.--avance]="avance" aria-label="Avance de la atención">
      @for (e of etapas; track e.clave; let i = $index) {
        <li [class.llena]="i <= etapa" [class.actual]="i === etapa" [attr.aria-current]="i === etapa ? 'step' : null">
          <span class="valvula" aria-hidden="true"></span><span class="nombre">{{ e.texto }}</span>
        </li>
      }
    </ol>
    <p class="tuberia-ayuda">{{ ayuda }}</p>
  } @else {
    <p class="tuberia-cerrada">{{ ayuda }}</p>
  }
  @if (historial.length) {
    <ul class="bitacora-pasos">
      @for (h of historialOrdenado; track h.fecha + h.estado) {
        <li>
          <time [attr.datetime]="h.fecha">{{ h.fecha | date:'d MMM, HH:mm' }}</time>
          <strong>{{ texto(h.estado) }}</strong>
          @if (h.observaciones) { <span>{{ h.observaciones }}</span> }
          @if (h.actor) { <small>Registró: {{ h.actor }}</small> }
        </li>
      }
    </ul>
  }`
})
export class TuberiaComponent {
  @Input({ required: true }) estado = 'recibido';
  @Input() historial: PasoHistorial[] = [];
  etapas = ETAPAS;
  get etapa() { return etapaDe(this.estado); }
  get avance() { return Math.max(0, this.etapa) / (ETAPAS.length - 1); }
  get ayuda() { return ESTADOS[this.estado]?.ayuda ?? ''; }
  get historialOrdenado() { return [...this.historial].sort((a, b) => b.fecha.localeCompare(a.fecha)); }
  texto(e: string) { return ESTADOS[e]?.texto ?? e; }
}
