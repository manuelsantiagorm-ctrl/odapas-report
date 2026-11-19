import { Component, Input, OnChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';
import { DetalleReporte } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';
import { EstadoComponent } from '../../compartido/estado.component';
import { TuberiaComponent } from '../../compartido/tuberia.component';
import { FotoComponent } from '../../compartido/foto.component';
import { MapaComponent } from '../../compartido/mapa.component';
import { SelectorFotosComponent } from '../../compartido/selector-fotos.component';
import { AvisosService } from '../../compartido/avisos.service';
import { RESULTADOS, PRIORIDADES } from '../../nucleo/etiquetas';

/** Seguimiento de un reporte: avance, historial, fotos y trabajos realizados. */
@Component({
  selector: 'app-detalle-reporte', standalone: true,
  imports: [RouterLink, DatePipe, EstadoComponent, TuberiaComponent, FotoComponent, MapaComponent, SelectorFotosComponent],
  template: `
  <nav class="migas"><a [routerLink]="sesion.tiene('ciudadano') ? '/mis-reportes' : sesion.inicio">Volver</a></nav>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (r) {
    <header class="cabeza-pagina">
      <div><p class="folio grande">{{ r.folio }}</p><h1>{{ r.tipo_nombre }} en {{ r.colonia }}</h1>
        <p>Enviado el {{ r.creado_en | date:"d 'de' MMMM 'de' y, HH:mm" }}</p></div>
      <app-estado [estado]="r.estado" />
    </header>
    <section class="panel"><h2>Avance</h2><app-tuberia [estado]="r.estado" [historial]="historialCompleto" /></section>
    <div class="dos-columnas">
      <section class="panel">
        <h2>Lo que reportaste</h2>
        <p class="cita">{{ r.descripcion }}</p>
        <dl class="datos">
          <div><dt>Dirección</dt><dd>{{ r.calle }} {{ r.numero_exterior }}@if (r.numero_interior) {, int. {{ r.numero_interior }}}, {{ r.colonia }}, CP {{ r.codigo_postal }}</dd></div>
          @if (r.referencias) { <div><dt>Referencias</dt><dd>{{ r.referencias }}</dd></div> }
          <div><dt>Zona</dt><dd>{{ r.zona || 'Sin zona asignada todavía' }}</dd></div>
          @if (r.prioridad) { <div><dt>Prioridad</dt><dd>{{ prioridades[r.prioridad] }}</dd></div> }
        </dl>
        @if (r.latitud != null && r.longitud != null) {
          <app-mapa [puntos]="[{ latitud: r.latitud, longitud: r.longitud, tipo: r.tipo, estado: r.estado, colonia: r.colonia }]" [seleccion]="{ latitud: r.latitud, longitud: r.longitud }" alto="240px" />
        }
      </section>
      <section class="panel">
        <h2>Fotos del reporte</h2>
        <div class="galeria">
          @for (f of r.fotos; track f.url) { <app-foto [url]="f.url" [alt]="'Foto del reporte ' + r.folio" /> }
          @empty { <p class="tenue">No se agregaron fotos.</p> }
        </div>
        @if (puedeAgregarFotos) {
          <app-selector-fotos #sel [max]="3 - r.fotos.length" (cambio)="nuevas = $event" etiqueta="Agregar más fotos" />
          @if (nuevas.length) { <button class="boton secundario" type="button" (click)="subir(); sel.limpiar()" [disabled]="ocupado">Subir {{ nuevas.length }} foto(s)</button> }
        }
      </section>
    </div>
    @for (o of r.ordenes; track o.folio) {
      <section class="panel">
        <header class="cabeza-panel"><h2>Orden de trabajo {{ o.folio }}</h2><app-estado [estado]="o.estado" /></header>
        <dl class="datos">
          <div><dt>Prioridad</dt><dd>{{ prioridades[o.prioridad] }}</dd></div>
          @if (o.fecha_programada) { <div><dt>Programada</dt><dd>{{ o.fecha_programada | date:'d MMM y' }}</dd></div> }
          @for (m of o.equipo; track $index) {
            <div><dt>{{ m.responsable_principal ? 'Responsable' : 'Apoyo' }}</dt><dd>{{ m.nombre || 'Personal de ODAPAS' }}@if (m.recibido_en) {, recibió la orden {{ m.recibido_en | date:'d MMM HH:mm' }}}</dd></div>
          }
        </dl>
        @for (rep of o.reparaciones; track rep.id) {
          <article class="reparacion">
            <h3>{{ rep.resultado ? 'Reparación: ' + resultados[rep.resultado] : 'Reparación en curso' }}</h3>
            <p><strong>Diagnóstico.</strong> {{ rep.diagnostico }}</p>
            @if (rep.trabajo_realizado) { <p><strong>Trabajo realizado.</strong> {{ rep.trabajo_realizado }}</p> }
            <div class="antes-despues">
              <div><h4>Antes</h4><div class="galeria">@for (f of fase(rep.fotos, 'antes'); track f.url) { <app-foto [url]="f.url" alt="Antes de la reparación" /> } @empty { <p class="tenue">Sin fotos</p> }</div></div>
              <div><h4>Después</h4><div class="galeria">@for (f of fase(rep.fotos, 'despues'); track f.url) { <app-foto [url]="f.url" alt="Después de la reparación" /> } @empty { <p class="tenue">Sin fotos</p> }</div></div>
            </div>
          </article>
        }
      </section>
    }
  }`
})
export class DetalleReporteComponent implements OnChanges {
  @Input() folio = '';
  r: DetalleReporte | null = null; error = ''; nuevas: File[] = []; ocupado = false;
  resultados = RESULTADOS; prioridades = PRIORIDADES;
  constructor(private http: HttpClient, public sesion: SesionService, private avisos: AvisosService) {}
  ngOnChanges(): void { this.cargar(); }
  cargar(): void {
    this.http.get<DetalleReporte>(API + '/reportes/' + encodeURIComponent(this.folio) + '/detalle').subscribe({
      next: r => this.r = r, error: e => this.error = mensajeError(e)
    });
  }
  get puedeAgregarFotos(): boolean {
    return !!this.r && this.r.fotos.length < 3 && this.sesion.tiene('ciudadano') && ['recibido', 'en_revision', 'validado'].includes(this.r.estado);
  }
  /** Historial del reporte más los pasos de sus órdenes (sin notas internas del personal). */
  get historialCompleto() {
    if (!this.r) return [];
    const ordenes = this.r.ordenes.flatMap(o => o.historial.map(h => ({ ...h, observaciones: this.sesion.tiene('administrador') ? h.observaciones : null, actor: undefined })));
    return [...this.r.historial, ...ordenes];
  }
  fase(fotos: { url: string; fase?: string }[], f: string) { return (fotos ?? []).filter(x => x.fase === f); }
  subir(): void {
    const datos = new FormData();
    this.nuevas.forEach(f => datos.append('fotos', f, f.name));
    this.ocupado = true;
    this.http.post(API + '/reportes/' + this.folio + '/fotos', datos).subscribe({
      next: () => { this.avisos.exito('Fotos agregadas.'); this.nuevas = []; this.ocupado = false; this.cargar(); },
      error: e => { this.avisos.error(mensajeError(e)); this.ocupado = false; }
    });
  }
}
