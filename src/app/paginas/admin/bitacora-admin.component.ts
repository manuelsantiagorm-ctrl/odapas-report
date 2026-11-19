import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';

interface Movimiento { fecha: string; origen: string; accion: string; entidad: string | null; referencia: string | null; detalle: string | null; actor: string | null; ip: string | null; }

/** Registro de auditoría: quién hizo qué y cuándo. No se puede modificar ni borrar. */
@Component({
  selector: 'app-bitacora-admin', standalone: true, imports: [FormsModule, DatePipe],
  template: `
  <header class="cabeza-pagina"><div><h1>Bitácora</h1><p>Registro inalterable de operaciones y accesos.</p></div>
    <label class="campo corto"><span>Mostrar</span><select [(ngModel)]="limite" (ngModelChange)="cargar()"><option [ngValue]="50">50</option><option [ngValue]="150">150</option><option [ngValue]="500">500</option></select></label></header>
  <div class="filtros"><label class="campo"><span>Filtrar</span><input type="search" [(ngModel)]="q" placeholder="Acción, folio, persona o IP"></label></div>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  <div class="tabla-envoltura">
    <table class="tabla compacta">
      <thead><tr><th>Fecha</th><th>Acción</th><th>Sobre</th><th>Detalle</th><th>Quién</th><th>IP</th></tr></thead>
      <tbody>
        @for (m of filtrados; track $index) {
          <tr [class.falta]="m.accion === 'login_fallido'">
            <td>{{ m.fecha | date:'d MMM, HH:mm:ss' }}</td><td>{{ m.accion.replaceAll('_', ' ') }}<span class="recorte">{{ m.origen }}</span></td>
            <td>{{ m.entidad }} {{ m.referencia }}</td><td>{{ m.detalle }}</td><td>{{ m.actor || 'Sistema o anónimo' }}</td><td>{{ m.ip }}</td>
          </tr>
        } @empty { <tr><td colspan="6" class="vacio">Sin movimientos.</td></tr> }
      </tbody>
    </table>
  </div>`
})
export class BitacoraAdminComponent implements OnInit {
  movimientos: Movimiento[] = []; limite = 150; q = ''; error = '';
  constructor(private http: HttpClient) {}
  ngOnInit(): void { this.cargar(); }
  cargar(): void { this.http.get<Movimiento[]>(API + '/admin/bitacora', { params: { limite: this.limite } }).subscribe({ next: m => this.movimientos = m, error: e => this.error = mensajeError(e) }); }
  get filtrados() {
    const q = this.q.trim().toLowerCase();
    return q ? this.movimientos.filter(m => Object.values(m).some(v => String(v ?? '').toLowerCase().includes(q))) : this.movimientos;
  }
}
