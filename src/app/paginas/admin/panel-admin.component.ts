import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';
import { textoEstado } from '../../nucleo/etiquetas';

interface Estadisticas {
  reportes: { total: number; por_revisar: number; validados_sin_orden: number; en_atencion: number; por_cerrar: number; cerrados: number; no_procedentes: number; sin_zona: number; ultimos_7_dias: number };
  porTipo: { codigo: string; nombre: string; total: number }[];
  porEstado: { estado: string; total: number }[];
  porDia: { dia: string; total: number }[];
  zonas: { codigo: string; nombre: string; abiertos: number; ordenes_activas: number; trabajadores_disponibles: number }[];
  personal: { activos: number; invitados: number; suspendidos: number };
}

/** Panel del administrador: lo que necesita atención hoy y cómo se comportan los reportes. */
@Component({
  selector: 'app-panel-admin', standalone: true, imports: [RouterLink, DatePipe, DecimalPipe],
  template: `
  <header class="cabeza-pagina"><div><h1>Panel de operación</h1><p>Lo que necesita atención ahora.</p></div>
    <button class="boton secundario" (click)="cargar()">Actualizar</button></header>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (e) {
    <section class="cola" aria-label="Pendientes">
      <a routerLink="/admin/reportes" [queryParams]="{ estado: 'recibido' }" [class.alerta]="e.reportes.por_revisar > 0"><strong>{{ e.reportes.por_revisar }}</strong><span>Por revisar</span><small>Reportes nuevos o en revisión</small></a>
      <a routerLink="/admin/reportes" [queryParams]="{ estado: 'validado' }" [class.alerta]="e.reportes.validados_sin_orden > 0"><strong>{{ e.reportes.validados_sin_orden }}</strong><span>Sin cuadrilla</span><small>Validados que esperan orden</small></a>
      <a routerLink="/admin/ordenes" [queryParams]="{ estado: 'reparacion_realizada' }" [class.alerta]="e.reportes.por_cerrar > 0"><strong>{{ e.reportes.por_cerrar }}</strong><span>Por cerrar</span><small>Reparaciones que revisar</small></a>
      <a routerLink="/admin/ordenes" [queryParams]="{ estado: 'activas' }"><strong>{{ e.reportes.en_atencion }}</strong><span>En atención</span><small>Órdenes en curso</small></a>
      @if (e.reportes.sin_zona) { <a routerLink="/admin/reportes" [queryParams]="{ zona: 'sin_zona' }" class="alerta"><strong>{{ e.reportes.sin_zona }}</strong><span>Sin zona</span><small>Colonias sin cobertura</small></a> }
    </section>

    <div class="dos-columnas">
      <section class="panel">
        <h2>Reportes de los últimos 14 días</h2>
        <div class="barras" role="img" [attr.aria-label]="'Reportes por día. Total últimos 7 días: ' + e.reportes.ultimos_7_dias">
          @for (d of e.porDia; track d.dia) {
            <div class="barra" [style.--h]="d.total / maxDia" [title]="(d.dia | date:'d MMM') + ': ' + d.total"><span class="valor">{{ d.total || '' }}</span><span class="dia">{{ d.dia | date:'d' }}</span></div>
          }
        </div>
        <p class="tenue">{{ e.reportes.ultimos_7_dias }} reportes en los últimos 7 días, {{ e.reportes.total | number }} en total.</p>
      </section>
      <section class="panel">
        <h2>Por tipo de problema</h2>
        <ul class="proporciones">
          @for (t of e.porTipo; track t.codigo) {
            <li><span>{{ t.nombre }}</span><span class="pista"><span [style.width.%]="100 * t.total / maxTipo"></span></span><strong>{{ t.total | number }}</strong></li>
          }
        </ul>
        <h2>Por estado</h2>
        <ul class="proporciones">
          @for (t of e.porEstado; track t.estado) {
            <li><span>{{ estado(t.estado) }}</span><span class="pista"><span [style.width.%]="100 * t.total / maxEstado"></span></span><strong>{{ t.total | number }}</strong></li>
          }
        </ul>
      </section>
    </div>
    <section class="panel">
      <header class="cabeza-panel"><h2>Zonas con más carga</h2><a routerLink="/admin/zonas">Administrar zonas</a></header>
      <table class="tabla">
        <thead><tr><th>Zona</th><th>Abiertos</th><th>Órdenes activas</th><th>Personal</th></tr></thead>
        <tbody>@for (z of e.zonas; track z.codigo) {
          <tr><td><a routerLink="/admin/reportes" [queryParams]="{ zona: z.codigo, estado: 'activos' }">{{ z.nombre }}</a> <span class="tenue">{{ z.codigo }}</span></td><td>{{ z.abiertos }}</td><td>{{ z.ordenes_activas }}</td>
            <td [class.falta]="z.trabajadores_disponibles === 0">{{ z.trabajadores_disponibles || 'Sin personal' }}</td></tr>
        }</tbody>
      </table>
      <p class="tenue">Personal: {{ e.personal.activos | number }} activos, {{ e.personal.invitados }} con invitación pendiente, {{ e.personal.suspendidos }} suspendidos.</p>
    </section>
  }`
})
export class PanelAdminComponent implements OnInit {
  e: Estadisticas | null = null; error = '';
  constructor(private http: HttpClient) {}
  ngOnInit(): void { this.cargar(); }
  cargar(): void { this.http.get<Estadisticas>(API + '/admin/estadisticas').subscribe({ next: e => this.e = e, error: x => this.error = mensajeError(x) }); }
  get maxDia() { return Math.max(1, ...(this.e?.porDia ?? []).map(d => d.total)); }
  get maxTipo() { return Math.max(1, ...(this.e?.porTipo ?? []).map(d => d.total)); }
  get maxEstado() { return Math.max(1, ...(this.e?.porEstado ?? []).map(d => d.total)); }
  estado = textoEstado;
}
