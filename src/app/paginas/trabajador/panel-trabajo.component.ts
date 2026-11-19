import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';
import { OrdenTrabajador } from '../../nucleo/modelos';
import { EstadoComponent } from '../../compartido/estado.component';
import { MapaComponent, PuntoEnMapa } from '../../compartido/mapa.component';
import { PRIORIDADES } from '../../nucleo/etiquetas';

interface Resumen {
  zona: { id: number; codigo: string; nombre: string; descripcion: string | null; colonias: string[] } | null;
  perfil: { nombre: string; numero_empleado: string };
  conteos: { activas: number; sin_recibir: number; terminadas: number };
  incidentesZona: (PuntoEnMapa & { folio: string; fecha_creacion: string })[];
}

/** Panel del trabajador: su zona, sus órdenes pendientes y los incidentes de la zona. */
@Component({
  selector: 'app-panel-trabajo', standalone: true, imports: [RouterLink, DatePipe, EstadoComponent, MapaComponent],
  template: `
  <header class="cabeza-pagina">
    <div><h1>Mis órdenes</h1>
      @if (r) { <p>{{ r.perfil.nombre }}, empleado {{ r.perfil.numero_empleado }}. @if (r.zona) { Trabajas en {{ r.zona.nombre }} ({{ r.zona.codigo }}). } @else { Sin zona asignada. }</p> }
    </div>
  </header>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (r) {
    <dl class="contadores">
      <div [class.alerta]="r.conteos.sin_recibir > 0"><dt>Por confirmar</dt><dd>{{ r.conteos.sin_recibir }}</dd></div>
      <div><dt>Activas</dt><dd>{{ r.conteos.activas }}</dd></div>
      <div><dt>Terminadas</dt><dd>{{ r.conteos.terminadas }}</dd></div>
    </dl>
  }
  <section class="bloque">
    <h2>Pendientes</h2>
    <ul class="lista-ordenes">
      @for (o of activas; track o.folio) {
        <li [attr.data-prioridad]="o.prioridad">
          <a [routerLink]="['/trabajo', o.folio]">
            <span class="folio">{{ o.folio }}</span>
            <strong>{{ o.tipo_nombre }}: {{ o.calle }}, {{ o.colonia }}</strong>
            <span class="tenue">{{ o.descripcion }}</span>
            <span class="meta"><span class="prioridad" [attr.data-nivel]="o.prioridad">{{ prioridades[o.prioridad] }}</span>
              @if (!o.recibido_en) { <span class="estado" data-tono="ambar">Sin confirmar</span> } @else { <app-estado [estado]="o.estado" /> }
              @if (!o.responsable_principal) { <span class="tenue">Apoyo</span> }</span>
          </a>
        </li>
      } @empty { <li class="vacio"><p>No tienes órdenes pendientes.</p></li> }
    </ul>
  </section>
  @if (r?.zona) {
    <section class="bloque">
      <h2>Incidentes abiertos en tu zona</h2>
      <app-mapa [puntos]="r!.incidentesZona" alto="340px" descripcion="Incidentes abiertos en tu zona" />
      <p class="tenue">Colonias de la zona: {{ r!.zona!.colonias.join(', ') }}</p>
    </section>
  }
  @if (terminadas.length) {
    <section class="bloque">
      <h2>Historial</h2>
      <ul class="lista-simple">
        @for (o of terminadas; track o.folio) {
          <li><a [routerLink]="['/trabajo', o.folio]">{{ o.folio }}</a> {{ o.tipo_nombre }} en {{ o.colonia }} <app-estado [estado]="o.estado" /> <span class="tenue">{{ (o.finalizado_en || o.asignacion_finalizada) | date:'d MMM y' }}</span></li>
        }
      </ul>
    </section>
  }`
})
export class PanelTrabajoComponent implements OnInit {
  r: Resumen | null = null; ordenes: OrdenTrabajador[] = []; error = ''; prioridades = PRIORIDADES;
  constructor(private http: HttpClient, private router: Router) {}
  ngOnInit(): void {
    this.http.get<Resumen>(API + '/trabajador/resumen').subscribe({ next: r => this.r = r, error: e => this.error = mensajeError(e) });
    this.http.get<OrdenTrabajador[]>(API + '/trabajador/ordenes').subscribe({ next: o => this.ordenes = o, error: e => this.error = mensajeError(e) });
  }
  get activas() { return this.ordenes.filter(o => !o.finalizado_en && !o.asignacion_finalizada); }
  get terminadas() { return this.ordenes.filter(o => o.finalizado_en || o.asignacion_finalizada); }
}
