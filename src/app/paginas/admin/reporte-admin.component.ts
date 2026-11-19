import { Component, Input, OnChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { finalize, Observable } from 'rxjs';
import { API, mensajeError } from '../../nucleo/api';
import { DetalleReporte, OrdenDeReporte } from '../../nucleo/modelos';
import { EstadoComponent } from '../../compartido/estado.component';
import { TuberiaComponent } from '../../compartido/tuberia.component';
import { FotoComponent } from '../../compartido/foto.component';
import { MapaComponent } from '../../compartido/mapa.component';
import { AvisosService } from '../../compartido/avisos.service';
import { PRIORIDADES, RESULTADOS, textoEstado } from '../../nucleo/etiquetas';

interface Elegibles { zona: { id?: number; codigo?: string; nombre?: string }; trabajadores: { id: number; nombre: string; numero_empleado: string; carga: number }[]; }

/** Atención de un reporte por el administrador: revisión, orden de trabajo, cierre o devolución. */
@Component({
  selector: 'app-reporte-admin', standalone: true, imports: [FormsModule, RouterLink, DatePipe, EstadoComponent, TuberiaComponent, FotoComponent, MapaComponent],
  template: `
  <nav class="migas"><a routerLink="/admin/reportes">Reportes</a></nav>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (r) {
    <header class="cabeza-pagina">
      <div><p class="folio grande">{{ r.folio }}</p><h1>{{ r.tipo_nombre }} en {{ r.calle }} {{ r.numero_exterior }}, {{ r.colonia }}</h1>
        <p>Recibido el {{ r.creado_en | date:"d 'de' MMMM, HH:mm" }}. Zona {{ r.zona || 'sin asignar' }}.</p></div>
      <app-estado [estado]="r.estado" />
    </header>

    <!-- Acción que toca según el estado -->
    @if (!r.zona) {
      <section class="panel nota">
        <h2>Este reporte no tiene zona</h2>
        <p>La colonia {{ r.colonia }} no está cubierta por ninguna zona activa. Asígnala en <a routerLink="/admin/zonas">Zonas</a> y después reclasifica.</p>
        <button class="boton secundario" (click)="hacer(http.post(api + '/clasificar', {}), 'Zona asignada.')" [disabled]="ocupado">Reclasificar zona</button>
      </section>
    }
    @if (puedeRevisar) {
      <section class="panel" [class.paso-actual]="r.estado !== 'validado'">
        <details [open]="r.estado !== 'validado'">
        <summary><h2>{{ r.estado === 'validado' ? 'Rechazar o cancelar este reporte' : 'Revisar el reporte' }}</h2></summary>
        <form (ngSubmit)="revisar()" #fr="ngForm">
          <div class="segmentado" role="radiogroup">
            @for (op of opcionesRevision; track op[0]) { <label [class.elegida]="rev.estado === op[0]"><input type="radio" name="est" [value]="op[0]" [(ngModel)]="rev.estado" required> {{ op[1] }}</label> }
          </div>
          <label class="campo"><span>Observaciones {{ rev.estado === 'rechazado' || rev.estado === 'cancelado' ? '(obligatorio: el ciudadano verá el motivo)' : '(opcional)' }}</span>
            <textarea name="obs" [(ngModel)]="rev.observaciones" maxlength="1000" rows="2" [required]="rev.estado === 'rechazado' || rev.estado === 'cancelado'"></textarea></label>
          <button class="boton" [disabled]="ocupado || fr.invalid">Guardar revisión</button>
        </form>
        </details>
      </section>
    }
    @if (puedeCrearOrden) {
      <section class="panel paso-actual">
        <h2>Crear orden de trabajo</h2>
        @if (!eleg?.trabajadores?.length) { <p class="error-campo">No hay personal activo en la zona {{ r.zona }}. Da de alta personal en <a routerLink="/admin/personal">Personal</a>.</p> }
        @else {
          <form (ngSubmit)="crearOrden()" #fo="ngForm">
            <div class="rejilla">
              <label class="campo"><span>Prioridad</span><select name="pri" [(ngModel)]="orden.prioridad" required>@for (p of prioridades; track p[0]) { <option [value]="p[0]">{{ p[1] }}</option> }</select></label>
              <label class="campo"><span>Fecha programada (opcional)</span><input type="date" name="fecha" [(ngModel)]="orden.fechaProgramada" [min]="hoy"></label>
            </div>
            <fieldset><legend>Personal de la zona {{ eleg!.zona.nombre }}</legend>
              <table class="tabla compacta">
                <thead><tr><th>Responsable</th><th>Apoyo</th><th>Nombre</th><th>Órdenes activas</th></tr></thead>
                <tbody>@for (t of eleg!.trabajadores; track t.id) {
                  <tr><td><input type="radio" name="resp" [value]="t.id" [(ngModel)]="orden.responsableId" required [attr.aria-label]="'Responsable ' + t.nombre"></td>
                    <td><input type="checkbox" [checked]="orden.colaboradores.includes(t.id)" [disabled]="orden.responsableId === t.id" (change)="alternar(t.id)" [attr.aria-label]="'Apoyo ' + t.nombre"></td>
                    <td>{{ t.nombre }} <span class="tenue">{{ t.numero_empleado }}</span></td><td>{{ t.carga }}</td></tr>
                }</tbody>
              </table>
            </fieldset>
            <label class="campo"><span>Indicaciones para la cuadrilla (opcional)</span><textarea name="obs" [(ngModel)]="orden.observaciones" maxlength="1000" rows="2"></textarea></label>
            <button class="boton" [disabled]="ocupado || fo.invalid">Crear orden y notificar</button>
          </form>
        }
      </section>
    }

    @for (o of r.ordenes; track o.folio) {
      <section class="panel" [class.paso-actual]="!o.finalizado_en">
        <header class="cabeza-panel"><h2>Orden {{ o.folio }}</h2><app-estado [estado]="o.estado" /></header>
        <dl class="datos">
          <div><dt>Prioridad</dt><dd>{{ prioridadTexto[o.prioridad] }}</dd></div>
          @if (o.fecha_programada) { <div><dt>Programada</dt><dd>{{ o.fecha_programada | date:'d MMM y' }}</dd></div> }
          @for (m of o.equipo; track $index) {
            <div><dt>{{ m.responsable_principal ? 'Responsable' : 'Apoyo' }}</dt><dd>{{ m.nombre }} ({{ m.numero_empleado }})@if (m.finalizado_en) { <span class="tenue">, asignación terminada</span> } @else if (m.recibido_en) { <span class="tenue">, confirmó {{ m.recibido_en | date:'d MMM HH:mm' }}</span> } @else { <span class="tenue">, sin confirmar</span> }</dd></div>
          }
        </dl>
        @for (rep of o.reparaciones; track rep.id) {
          <article class="reparacion">
            <h3>{{ rep.resultado ? 'Intervención: ' + resultados[rep.resultado] : 'Intervención en curso' }}</h3>
            <p><strong>Diagnóstico.</strong> {{ rep.diagnostico }}</p>
            @if (rep.trabajo_realizado) { <p><strong>Trabajo realizado.</strong> {{ rep.trabajo_realizado }}</p> }
            <div class="galeria">@for (f of rep.fotos; track f.url) { <figure><app-foto [url]="f.url" /><figcaption>{{ f.fase === 'antes' ? 'Antes' : 'Después' }}</figcaption></figure> }</div>
          </article>
        }
        <ul class="bitacora-pasos">@for (h of o.historial; track h.secuencia) { <li><time>{{ h.fecha | date:'d MMM, HH:mm' }}</time><strong>{{ texto(h.estado) }}</strong>@if (h.observaciones) { <span>{{ h.observaciones }}</span> }</li> }</ul>

        @if (!o.finalizado_en) {
          <div class="acciones-orden">
            <label class="campo"><span>Observaciones o motivo</span><textarea [(ngModel)]="nota[o.folio]" maxlength="1000" rows="2" [name]="'nota' + o.folio"></textarea></label>
            <div class="acciones">
              @if (o.estado === 'reparacion_realizada') {
                <button class="boton" (click)="estadoOrden(o, 'cerrar')" [disabled]="ocupado">Cerrar orden</button>
                <button class="boton secundario" (click)="estadoOrden(o, 'devolver')" [disabled]="ocupado || !nota[o.folio]">Devolver al responsable</button>
              }
              <button class="boton rojo-suave" (click)="estadoOrden(o, 'cancelar')" [disabled]="ocupado || !nota[o.folio]">Cancelar orden</button>
              <button class="enlace" type="button" (click)="reasignando = reasignando === o.folio ? '' : o.folio">Cambiar responsable</button>
            </div>
            @if (reasignando === o.folio) {
              <form class="rejilla" (ngSubmit)="reasignar(o)" #fa="ngForm">
                <label class="campo"><span>Nuevo responsable</span><select name="nuevo" [(ngModel)]="nuevoResponsable" required>@for (t of eleg?.trabajadores ?? []; track t.id) { <option [ngValue]="t.id">{{ t.nombre }} ({{ t.carga }} activas)</option> }</select></label>
                <label class="campo"><span>Motivo</span><input name="motivo" [(ngModel)]="motivo" required minlength="5" maxlength="500"></label>
                <button class="boton secundario" [disabled]="ocupado || fa.invalid">Reasignar</button>
              </form>
            }
            <p class="tenue">Para devolver o cancelar escribe primero el motivo.</p>
          </div>
        }
      </section>
    }

    <div class="dos-columnas">
      <section class="panel">
        <h2>Reporte del ciudadano</h2>
        <p class="cita">{{ r.descripcion }}</p>
        <dl class="datos">
          @if (r.ciudadano) { <div><dt>Ciudadano</dt><dd>{{ r.ciudadano.nombre }}, <a [href]="'tel:' + r.ciudadano.telefono">{{ r.ciudadano.telefono }}</a></dd></div> }
          <div><dt>Dirección</dt><dd>{{ r.calle }} {{ r.numero_exterior }}@if (r.numero_interior) {, int. {{ r.numero_interior }}}, {{ r.colonia }}, {{ r.municipio }}, CP {{ r.codigo_postal }}</dd></div>
          @if (r.referencias) { <div><dt>Referencias</dt><dd>{{ r.referencias }}</dd></div> }
        </dl>
        <div class="galeria">@for (f of r.fotos; track f.url) { <app-foto [url]="f.url" /> } @empty { <p class="tenue">Sin fotos.</p> }</div>
        @if (r.latitud != null && r.longitud != null) {
          <app-mapa [puntos]="[{ latitud: r.latitud, longitud: r.longitud, tipo: r.tipo, estado: r.estado, colonia: r.colonia }]" [seleccion]="{ latitud: r.latitud, longitud: r.longitud }" alto="240px" />
        }
      </section>
      <section class="panel"><h2>Seguimiento</h2><app-tuberia [estado]="r.estado" [historial]="historialCompleto" /></section>
    </div>
  }`
})
export class ReporteAdminComponent implements OnChanges {
  @Input() folio = '';
  r: DetalleReporte | null = null; eleg: Elegibles | null = null; error = ''; ocupado = false;
  rev = { estado: '', observaciones: '' };
  orden = { prioridad: 'media', fechaProgramada: '', responsableId: null as number | null, colaboradores: [] as number[], observaciones: '' };
  nota: Record<string, string> = {}; reasignando = ''; nuevoResponsable: number | null = null; motivo = '';
  prioridades = Object.entries(PRIORIDADES); prioridadTexto = PRIORIDADES; resultados = RESULTADOS; texto = textoEstado;
  hoy = new Date().toISOString().slice(0, 10);
  constructor(public http: HttpClient, private avisos: AvisosService) {}
  get historialCompleto() { return this.r ? [...this.r.historial, ...this.r.ordenes.flatMap(o => o.historial.map(h => ({ ...h, observaciones: o.folio + (h.observaciones ? ': ' + h.observaciones : '') })))] : []; }
  get api() { return API + '/admin/reportes/' + encodeURIComponent(this.folio); }
  ngOnChanges(): void { this.cargar(); }
  cargar(): void {
    this.http.get<DetalleReporte>(API + '/reportes/' + encodeURIComponent(this.folio) + '/detalle').subscribe({
      next: r => { this.r = r; this.rev = { estado: '', observaciones: '' }; }, error: e => this.error = mensajeError(e)
    });
    this.http.get<Elegibles>(this.api + '/elegibles').subscribe({ next: e => this.eleg = e, error: () => {} });
  }
  private get ordenActiva(): boolean { return !!this.r?.ordenes.some(o => !o.finalizado_en); }
  get puedeRevisar() { return !!this.r && ['recibido', 'en_revision', 'validado'].includes(this.r.estado) && !this.ordenActiva; }
  get puedeCrearOrden() { return !!this.r && this.r.estado === 'validado' && !this.ordenActiva && !!this.r.zona; }
  get opcionesRevision(): [string, string][] {
    switch (this.r?.estado) {
      case 'recibido': return [['en_revision', 'Marcar en revisión'], ['validado', 'Validar'], ['rechazado', 'No procede']];
      case 'en_revision': return [['validado', 'Validar'], ['rechazado', 'No procede']];
      default: return [['rechazado', 'No procede'], ['cancelado', 'Cancelar reporte']];
    }
  }
  /** Ejecuta una acción, muestra el resultado y recarga la pantalla. */
  hacer(peticion: Observable<unknown>, exito: string): void {
    this.ocupado = true;
    peticion.pipe(finalize(() => this.ocupado = false)).subscribe({
      next: () => { this.avisos.exito(exito); this.cargar(); }, error: e => this.avisos.error(mensajeError(e))
    });
  }
  revisar(): void { this.hacer(this.http.put(this.api + '/revision', this.rev), 'Revisión guardada y ciudadano notificado.'); }
  alternar(id: number): void {
    const c = this.orden.colaboradores;
    this.orden.colaboradores = c.includes(id) ? c.filter(x => x !== id) : [...c, id];
  }
  crearOrden(): void {
    const cuerpo = { ...this.orden, reporteFolio: this.folio, fechaProgramada: this.orden.fechaProgramada || null,
      colaboradores: this.orden.colaboradores.filter(x => x !== this.orden.responsableId) };
    this.hacer(this.http.post(API + '/admin/ordenes', cuerpo), 'Orden creada. El personal ya fue notificado.');
  }
  estadoOrden(o: OrdenDeReporte, accion: 'cerrar' | 'devolver' | 'cancelar'): void {
    const textos = { cerrar: 'Orden cerrada. El ciudadano fue notificado.', devolver: 'Orden devuelta al responsable.', cancelar: 'Orden cancelada.' };
    this.hacer(this.http.put(API + '/admin/ordenes/' + o.folio + '/estado', { accion, observaciones: this.nota[o.folio] || null }), textos[accion]);
  }
  reasignar(o: OrdenDeReporte): void {
    this.hacer(this.http.put(API + '/admin/ordenes/' + o.folio + '/responsable', { responsableId: this.nuevoResponsable, motivo: this.motivo }), 'Responsable actualizado.');
    this.reasignando = ''; this.motivo = '';
  }
}
