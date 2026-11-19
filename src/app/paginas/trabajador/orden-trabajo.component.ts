import { Component, Input, OnChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { finalize } from 'rxjs';
import { API, mensajeError } from '../../nucleo/api';
import { DetalleReporte, OrdenDeReporte, OrdenTrabajador } from '../../nucleo/modelos';
import { EstadoComponent } from '../../compartido/estado.component';
import { FotoComponent } from '../../compartido/foto.component';
import { MapaComponent } from '../../compartido/mapa.component';
import { SelectorFotosComponent } from '../../compartido/selector-fotos.component';
import { AvisosService } from '../../compartido/avisos.service';
import { PRIORIDADES, RESULTADOS, textoEstado } from '../../nucleo/etiquetas';

/** Orden de trabajo en campo: confirmar, diagnosticar, fotos del antes y después y cierre del trabajo. */
@Component({
  selector: 'app-orden-trabajo', standalone: true, imports: [FormsModule, RouterLink, DatePipe, EstadoComponent, FotoComponent, MapaComponent, SelectorFotosComponent],
  template: `
  <nav class="migas"><a routerLink="/trabajo">Mis órdenes</a></nav>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (r && o) {
    <header class="cabeza-pagina">
      <div><p class="folio grande">{{ o.folio }}</p><h1>{{ r.tipo_nombre }}: {{ r.calle }} {{ r.numero_exterior }}, {{ r.colonia }}</h1>
        <p>Prioridad <strong>{{ prioridades[o.prioridad] }}</strong>@if (o.fecha_programada) {, programada para el {{ o.fecha_programada | date:'d MMM' }}}. Reporte {{ r.folio }}.</p></div>
      <app-estado [estado]="o.estado" />
    </header>

    <section class="panel paso-actual">
      <h2>{{ titulo }}</h2>
      @if (!mia?.responsable_principal && !terminada) { <p class="nota">Estás como apoyo. El responsable principal registra el avance.</p> }
      @if (puedeRecibir) {
        <p>Confirma que viste la orden para que el administrador sepa que vas en camino.</p>
        <button class="boton grande" (click)="accion('recibir', {})" [disabled]="ocupado">Confirmar que la recibí</button>
      }
      @if (puedeIniciar) {
        <form (ngSubmit)="accion('iniciar', { diagnostico })" #fi="ngForm">
          <label class="campo"><span>Diagnóstico al llegar</span><textarea name="diag" [(ngModel)]="diagnostico" required minlength="5" maxlength="2000" rows="3" placeholder="Qué encontraste: tubo roto, válvula dañada, toma clandestina…"></textarea></label>
          <button class="boton grande" [disabled]="ocupado || fi.invalid">Iniciar reparación</button>
        </form>
      }
      @if (enProceso) {
        <div class="antes-despues">
          @for (fase of ['antes', 'despues']; track fase) {
            <div>
              <h3>{{ fase === 'antes' ? 'Antes' : 'Después' }}</h3>
              <div class="galeria">@for (f of fotosFase(fase); track f.url) { <app-foto [url]="f.url" /> } @empty { <p class="tenue">Sin fotos todavía.</p> }</div>
              @if (fotosFase(fase).length < 4) {
                <app-selector-fotos #sel [max]="4 - fotosFase(fase).length" [etiqueta]="'Agregar fotos del ' + (fase === 'antes' ? 'antes' : 'después')" (cambio)="pendientes[fase] = $event" />
                @if (pendientes[fase].length) { <button class="boton secundario" type="button" (click)="subir(fase); sel.limpiar()" [disabled]="ocupado">Subir {{ pendientes[fase].length }} foto(s)</button> }
              }
            </div>
          }
        </div>
        <form (ngSubmit)="terminar()" #ft="ngForm" class="cierre">
          <h3>Registrar el trabajo terminado</h3>
          <label class="campo"><span>¿Qué hiciste?</span><textarea name="trabajo" [(ngModel)]="fin.trabajoRealizado" required minlength="5" maxlength="1800" rows="3"></textarea></label>
          <fieldset class="segmentado" role="radiogroup"><legend>Resultado</legend>
            @for (x of resultadosLista; track x[0]) { <label [class.elegida]="fin.resultado === x[0]"><input type="radio" name="res" [value]="x[0]" [(ngModel)]="fin.resultado" required> {{ x[1] }}</label> }
          </fieldset>
          @if (!fotosFase('despues').length) {
            <label class="campo"><span>No hay foto del después. Explica por qué</span><input name="just" [(ngModel)]="fin.justificacionSinFoto" minlength="10" maxlength="500"></label>
          }
          <button class="boton grande" [disabled]="ocupado || ft.invalid">Marcar trabajo como terminado</button>
        </form>
      }
      @if (o.estado === 'reparacion_realizada') { <p>Registraste el trabajo. El administrador lo revisará para cerrar la orden o regresártela con observaciones.</p> }
      @if (terminada) { <p>Esta orden está {{ textoEstado(o.estado).toLowerCase() }}.</p> }
    </section>

    <div class="dos-columnas">
      <section class="panel">
        <h2>El reporte</h2>
        <p class="cita">{{ r.descripcion }}</p>
        <dl class="datos">
          <div><dt>Dirección</dt><dd>{{ r.calle }} {{ r.numero_exterior }}@if (r.numero_interior) {, int. {{ r.numero_interior }}}, {{ r.colonia }}, CP {{ r.codigo_postal }}</dd></div>
          @if (r.referencias) { <div><dt>Referencias</dt><dd>{{ r.referencias }}</dd></div> }
          @if (r.ciudadano) { <div><dt>Contacto</dt><dd>{{ r.ciudadano.nombre }}, <a [href]="'tel:' + r.ciudadano.telefono">{{ r.ciudadano.telefono }}</a></dd></div> }
        </dl>
        @if (r.latitud != null && r.longitud != null) {
          <app-mapa [puntos]="[{ latitud: r.latitud, longitud: r.longitud, tipo: r.tipo, estado: r.estado, colonia: r.colonia }]" [seleccion]="{ latitud: r.latitud, longitud: r.longitud }" alto="220px" />
          <a class="boton secundario" target="_blank" rel="noopener" [href]="'https://www.google.com/maps/dir/?api=1&destination=' + r.latitud + ',' + r.longitud">Cómo llegar</a>
        }
        @if (r.fotos.length) { <h3>Fotos del ciudadano</h3> }
        <div class="galeria">@for (f of r.fotos; track f.url) { <app-foto [url]="f.url" alt="Foto enviada por el ciudadano" /> }</div>
      </section>
      <section class="panel">
        <h2>Historial de la orden</h2>
        <ul class="bitacora-pasos">
          @for (h of historial; track h.secuencia) { <li><time>{{ h.fecha | date:'d MMM, HH:mm' }}</time><strong>{{ textoEstado(h.estado) }}</strong>@if (h.observaciones) { <span>{{ h.observaciones }}</span> }</li> }
        </ul>
        <h3>Equipo</h3>
        <ul class="lista-simple">@for (m of o.equipo; track $index) { <li>{{ m.nombre }} ({{ m.numero_empleado }}){{ m.responsable_principal ? ', responsable' : ', apoyo' }}</li> }</ul>
      </section>
    </div>
  }`
})
export class OrdenTrabajoComponent implements OnChanges {
  @Input() folio = '';
  r: DetalleReporte | null = null; o: OrdenDeReporte | null = null; mia: OrdenTrabajador | null = null;
  error = ''; ocupado = false; diagnostico = '';
  fin = { trabajoRealizado: '', resultado: 'resuelto', justificacionSinFoto: '' };
  pendientes: Record<string, File[]> = { antes: [], despues: [] };
  prioridades = PRIORIDADES; resultadosLista = Object.entries(RESULTADOS); textoEstado = textoEstado;
  constructor(private http: HttpClient, private avisos: AvisosService) {}
  ngOnChanges(): void { this.cargar(); }
  cargar(): void {
    const folio = this.folio.toUpperCase();
    this.http.get<DetalleReporte>(API + '/trabajador/ordenes/' + encodeURIComponent(folio)).subscribe({
      next: r => { this.r = r; this.o = r.ordenes.find(x => x.folio.toUpperCase() === folio) ?? null; },
      error: e => this.error = mensajeError(e)
    });
    this.http.get<OrdenTrabajador[]>(API + '/trabajador/ordenes').subscribe({ next: l => this.mia = l.find(x => x.folio.toUpperCase() === folio) ?? null, error: () => {} });
  }
  get terminada() { return this.o?.estado === 'cerrada' || this.o?.estado === 'cancelada' || !!this.mia?.asignacion_finalizada; }
  get puedeRecibir() { return !!this.mia && !this.mia.recibido_en && !this.terminada; }
  /** Reparación abierta (sin terminar). Si el administrador devolvió la orden, no hay y se registra una nueva. */
  get abierta() { const rep = this.o?.reparaciones?.at(-1); return rep && !rep.terminado_en ? rep : null; }
  get devuelta() { return this.o?.estado === 'en_proceso' && !this.abierta; }
  get puedeIniciar() { return !!this.mia?.responsable_principal && !this.puedeRecibir && (this.o?.estado === 'asignada' || this.devuelta); }
  get enProceso() { return !!this.mia?.responsable_principal && this.o?.estado === 'en_proceso' && !!this.abierta; }
  get titulo() {
    if (this.puedeRecibir) return 'Paso 1: confirma la orden';
    if (this.puedeIniciar) return this.devuelta ? 'La orden fue devuelta: registra un nuevo diagnóstico' : 'Paso 2: registra el diagnóstico';
    if (this.enProceso) return 'Paso 3: fotos y cierre del trabajo';
    if (this.o?.estado === 'reparacion_realizada') return 'En revisión del administrador';
    return 'Orden ' + textoEstado(this.o?.estado ?? '').toLowerCase();
  }
  get historial() { return [...(this.o?.historial ?? [])].sort((a, b) => b.secuencia - a.secuencia); }
  /** Fotos de la reparación abierta (la última) en la fase indicada. */
  fotosFase(fase: string) {
    const rep = this.abierta;
    return rep ? (rep.fotos ?? []).filter(f => f.fase === fase) : [];
  }
  accion(nombre: 'recibir' | 'iniciar', cuerpo: object): void {
    this.ocupado = true;
    this.http.post(API + '/trabajador/ordenes/' + this.o!.folio + '/' + nombre, cuerpo).pipe(finalize(() => this.ocupado = false)).subscribe({
      next: () => { this.avisos.exito(nombre === 'recibir' ? 'Orden confirmada.' : 'Reparación iniciada.'); this.cargar(); },
      error: e => this.avisos.error(mensajeError(e))
    });
  }
  subir(fase: string): void {
    const datos = new FormData();
    this.pendientes[fase].forEach(f => datos.append('fotos', f, f.name));
    this.ocupado = true;
    this.http.post(API + '/trabajador/ordenes/' + this.o!.folio + '/fotos', datos, { params: { fase } }).pipe(finalize(() => this.ocupado = false)).subscribe({
      next: () => { this.avisos.exito('Fotos guardadas.'); this.pendientes[fase] = []; this.cargar(); },
      error: e => this.avisos.error(mensajeError(e))
    });
  }
  terminar(): void {
    this.ocupado = true;
    const cuerpo = { ...this.fin, justificacionSinFoto: this.fin.justificacionSinFoto || null };
    this.http.post(API + '/trabajador/ordenes/' + this.o!.folio + '/terminar', cuerpo).pipe(finalize(() => this.ocupado = false)).subscribe({
      next: () => { this.avisos.exito('Trabajo registrado. Queda en revisión.'); this.cargar(); },
      error: e => this.avisos.error(mensajeError(e))
    });
  }
}
