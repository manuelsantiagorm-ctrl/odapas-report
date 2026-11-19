import { Component, Input, OnChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';
import { Pagina, ReporteResumen } from '../../nucleo/modelos';
import { EstadoComponent } from '../../compartido/estado.component';
import { MapaComponent } from '../../compartido/mapa.component';
import { ESTADOS, PRIORIDADES, TIPOS } from '../../nucleo/etiquetas';

/** Bandeja de reportes con filtros, búsqueda, paginación y vista de mapa. */
@Component({
  selector: 'app-reportes-admin', standalone: true, imports: [FormsModule, RouterLink, DatePipe, DecimalPipe, EstadoComponent, MapaComponent],
  template: `
  <header class="cabeza-pagina"><div><h1>Reportes</h1><p>{{ pagina?.total ?? 0 | number }} resultados</p></div>
    <div class="segmentado"><label [class.elegida]="vista === 'tabla'"><input type="radio" name="v" value="tabla" [(ngModel)]="vista"> Lista</label><label [class.elegida]="vista === 'mapa'"><input type="radio" name="v" value="mapa" [(ngModel)]="vista"> Mapa</label></div>
  </header>
  <form class="filtros" (ngSubmit)="filtrar()">
    <label class="campo"><span>Buscar</span><input type="search" name="q" [(ngModel)]="f.q" placeholder="Folio, calle, colonia o descripción"></label>
    <label class="campo"><span>Estado</span><select name="estado" [(ngModel)]="f.estado" (ngModelChange)="filtrar()">
      <option value="">Todos</option><option value="pendientes">Pendientes de orden</option><option value="activos">Abiertos</option>
      @for (s of estados; track s[0]) { <option [value]="s[0]">{{ s[1].texto }}</option> }</select></label>
    <label class="campo"><span>Tipo</span><select name="tipo" [(ngModel)]="f.tipo" (ngModelChange)="filtrar()"><option value="">Todos</option>@for (t of tipos; track t[0]) { <option [value]="t[0]">{{ t[1] }}</option> }</select></label>
    <label class="campo"><span>Zona</span><input name="zona" [(ngModel)]="f.zona" placeholder="ZN, ZC, sin_zona" maxlength="20"></label>
    <button class="boton secundario" type="submit">Filtrar</button>
  </form>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (vista === 'mapa') {
    <app-mapa [puntos]="pagina?.datos ?? []" alto="520px" (abrir)="abrir($event)" descripcion="Reportes de esta página en el mapa" />
    <p class="tenue">Se muestran los reportes de la página actual que tienen ubicación. Toca un punto para atenderlo.</p>
  } @else {
    <div class="tabla-envoltura">
      <table class="tabla">
        <thead><tr><th>Folio</th><th>Problema</th><th>Ubicación</th><th>Zona</th><th>Prioridad</th><th>Estado</th><th>Fecha</th></tr></thead>
        <tbody>
          @for (r of pagina?.datos ?? []; track r.folio) {
            <tr (click)="abrir(r.folio)" class="clicable">
              <td><a [routerLink]="['/admin/reportes', r.folio]" (click)="$event.stopPropagation()">{{ r.folio }}</a></td>
              <td><strong>{{ tiposMapa[r.tipo] || r.tipo }}</strong><span class="recorte">{{ r.descripcion }}</span></td>
              <td>{{ r.calle }}, {{ r.colonia }}</td><td>{{ r.zona || 'Sin zona' }}</td>
              <td>{{ r.prioridad ? prioridades[r.prioridad] : '' }}</td><td><app-estado [estado]="r.estado" /></td>
              <td>{{ r.fecha_creacion | date:'d MMM y' }}</td>
            </tr>
          } @empty { <tr><td colspan="7" class="vacio">No hay reportes con estos filtros.</td></tr> }
        </tbody>
      </table>
    </div>
  }
  @if (pagina && paginas > 1) {
    <nav class="paginacion" aria-label="Páginas">
      <button class="boton fantasma" (click)="ir(pagina.pagina - 1)" [disabled]="pagina.pagina <= 1">Anterior</button>
      <span>Página {{ pagina.pagina }} de {{ paginas | number }}</span>
      <button class="boton fantasma" (click)="ir(pagina.pagina + 1)" [disabled]="pagina.pagina >= paginas">Siguiente</button>
    </nav>
  }`
})
export class ReportesAdminComponent implements OnChanges {
  @Input() estado = ''; @Input() zona = ''; @Input() tipo = ''; @Input() q = ''; @Input() p = '';
  f = { estado: '', zona: '', tipo: '', q: '' };
  pagina: Pagina<ReporteResumen> | null = null; error = ''; vista = 'tabla';
  estados = Object.entries(ESTADOS).filter(([k]) => !['asignada', 'cancelada'].includes(k));
  tipos = Object.entries(TIPOS); tiposMapa = TIPOS; prioridades = PRIORIDADES;
  constructor(private http: HttpClient, private router: Router) {}
  ngOnChanges(): void {
    this.f = { estado: this.estado ?? '', zona: this.zona ?? '', tipo: this.tipo ?? '', q: this.q ?? '' };
    this.http.get<Pagina<ReporteResumen>>(API + '/admin/reportes', { params: { ...this.f, pagina: this.p || '1', tamano: '20' } }).subscribe({
      next: p => { this.pagina = p; this.error = ''; }, error: e => this.error = mensajeError(e)
    });
  }
  get paginas() { return this.pagina ? Math.max(1, Math.ceil(this.pagina.total / this.pagina.tamano)) : 1; }
  filtrar(p = 1): void {
    const qp: Record<string, string> = {};
    Object.entries(this.f).forEach(([k, v]) => { if (v) qp[k] = v.trim(); });
    if (p > 1) qp['p'] = String(p);
    void this.router.navigate([], { queryParams: qp });
  }
  ir(p: number) { this.filtrar(p); }
  abrir(folio: string) { void this.router.navigate(['/admin/reportes', folio]); }
}
