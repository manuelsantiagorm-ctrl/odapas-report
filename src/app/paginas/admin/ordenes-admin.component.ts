import { Component, Input, OnChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';
import { OrdenAdmin } from '../../nucleo/modelos';
import { EstadoComponent } from '../../compartido/estado.component';
import { PRIORIDADES, TIPOS } from '../../nucleo/etiquetas';

/** Todas las órdenes de trabajo, con filtro por estado y zona. */
@Component({
  selector: 'app-ordenes-admin', standalone: true, imports: [FormsModule, RouterLink, DatePipe, EstadoComponent],
  template: `
  <header class="cabeza-pagina"><div><h1>Órdenes de trabajo</h1><p>{{ ordenes.length }} órdenes{{ ordenes.length === 300 ? ' (se muestran las 300 más relevantes)' : '' }}</p></div></header>
  <form class="filtros" (ngSubmit)="filtrar()">
    <label class="campo"><span>Estado</span><select name="estado" [(ngModel)]="f.estado" (ngModelChange)="filtrar()">
      <option value="">Todas</option><option value="activas">Activas</option><option value="asignada">Asignadas</option><option value="en_proceso">En reparación</option>
      <option value="reparacion_realizada">Por cerrar</option><option value="cerrada">Cerradas</option><option value="cancelada">Canceladas</option></select></label>
    <label class="campo"><span>Zona</span><input name="zona" [(ngModel)]="f.zona" placeholder="Código, por ejemplo ZN" maxlength="20"></label>
    <button class="boton secundario">Filtrar</button>
  </form>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  <div class="tabla-envoltura">
    <table class="tabla">
      <thead><tr><th>Orden</th><th>Reporte</th><th>Problema</th><th>Zona</th><th>Responsable</th><th>Prioridad</th><th>Estado</th><th>Creada</th></tr></thead>
      <tbody>
        @for (o of ordenes; track o.folio) {
          <tr class="clicable" (click)="abrir(o)">
            <td><strong>{{ o.folio }}</strong></td><td><a [routerLink]="['/admin/reportes', o.reporte_folio]" (click)="$event.stopPropagation()">{{ o.reporte_folio }}</a></td>
            <td>{{ tipos[o.tipo] || o.tipo }}, {{ o.colonia }}</td><td>{{ o.zona }}</td><td>{{ o.responsable || 'Sin responsable' }}</td>
            <td><span class="prioridad" [attr.data-nivel]="o.prioridad">{{ prioridades[o.prioridad] }}</span></td><td><app-estado [estado]="o.estado" /></td>
            <td>{{ o.creado_en | date:'d MMM y' }}</td>
          </tr>
        } @empty { <tr><td colspan="8" class="vacio">No hay órdenes con estos filtros.</td></tr> }
      </tbody>
    </table>
  </div>`
})
export class OrdenesAdminComponent implements OnChanges {
  @Input() estado = ''; @Input() zona = '';
  f = { estado: '', zona: '' }; ordenes: OrdenAdmin[] = []; error = ''; prioridades = PRIORIDADES; tipos = TIPOS;
  constructor(private http: HttpClient, private router: Router) {}
  ngOnChanges(): void {
    this.f = { estado: this.estado ?? '', zona: this.zona ?? '' };
    this.http.get<OrdenAdmin[]>(API + '/admin/ordenes', { params: this.f }).subscribe({ next: o => this.ordenes = o, error: e => this.error = mensajeError(e) });
  }
  filtrar(): void {
    const qp: Record<string, string> = {};
    if (this.f.estado) qp['estado'] = this.f.estado;
    if (this.f.zona) qp['zona'] = this.f.zona.trim();
    void this.router.navigate([], { queryParams: qp });
  }
  abrir(o: OrdenAdmin) { void this.router.navigate(['/admin/reportes', o.reporte_folio]); }
}
