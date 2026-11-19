import { Component, Input } from '@angular/core';
import { textoEstado, tonoEstado } from '../nucleo/etiquetas';

/** Etiqueta de color con el estado de un reporte u orden. */
@Component({
  selector: 'app-estado', standalone: true,
  template: `<span class="estado" [attr.data-tono]="tono">{{ texto }}</span>`
})
export class EstadoComponent {
  @Input({ required: true }) estado = '';
  get texto() { return textoEstado(this.estado); }
  get tono() { return tonoEstado(this.estado); }
}
