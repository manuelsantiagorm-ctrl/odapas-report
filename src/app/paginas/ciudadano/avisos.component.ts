import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API, mensajeError } from '../../nucleo/api';
import { Aviso } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';

/** Avisos (notificaciones) de cambios en reportes y órdenes. */
@Component({
  selector: 'app-avisos', standalone: true, imports: [DatePipe, FormsModule],
  template: `
  <header class="cabeza-pagina">
    <div><h1>Avisos</h1><p>Te avisamos cada vez que cambia algo en tus reportes u órdenes.</p></div>
    <div class="acciones">
      <label class="casilla"><input type="checkbox" [(ngModel)]="soloNoLeidos" (ngModelChange)="cargar()"> Solo sin leer</label>
      <button class="boton secundario" (click)="leerTodos()" [disabled]="!hayNoLeidos">Marcar todos como leídos</button>
    </div>
  </header>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  <ul class="lista-avisos">
    @for (a of avisos; track a.id) {
      <li [class.nuevo]="!a.leido_en">
        <button type="button" (click)="abrir(a)">
          <strong>{{ a.titulo }}</strong>
          <time>{{ a.creado_en | date:'d MMM y, HH:mm' }}</time>
        </button>
      </li>
    } @empty { <li class="vacio"><p>{{ soloNoLeidos ? 'No tienes avisos sin leer.' : 'Todavía no tienes avisos.' }}</p></li> }
  </ul>`
})
export class AvisosComponent implements OnInit {
  avisos: Aviso[] = []; soloNoLeidos = false; error = '';
  constructor(private http: HttpClient, private sesion: SesionService, private router: Router) {}
  ngOnInit(): void { this.cargar(); }
  get hayNoLeidos() { return this.avisos.some(a => !a.leido_en); }
  cargar(): void {
    this.http.get<Aviso[]>(API + '/notificaciones', { params: { soloNoLeidas: this.soloNoLeidos } }).subscribe({
      next: a => this.avisos = a, error: e => this.error = mensajeError(e)
    });
  }
  leerTodos(): void { this.http.put(API + '/notificaciones/leer-todas', {}).subscribe(() => this.cargar()); }
  abrir(a: Aviso): void {
    if (!a.leido_en) this.http.put(API + '/notificaciones/' + a.id + '/leida', {}).subscribe({ error: () => {} });
    if (this.sesion.tiene('administrador') && a.reporte_folio) void this.router.navigate(['/admin/reportes', a.reporte_folio]);
    else if (this.sesion.tiene('trabajador') && a.orden_folio && ['trabajo_asignado', 'trabajo_devuelto', 'orden_cancelada', 'nuevo_reporte_zona'].includes(a.tipo)) void this.router.navigate(['/trabajo', a.orden_folio]);
    else if (this.sesion.tiene('ciudadano') && a.reporte_folio) void this.router.navigate(['/reporte', a.reporte_folio]);
    else if (this.sesion.tiene('trabajador') && a.orden_folio) void this.router.navigate(['/trabajo', a.orden_folio]);
    else this.cargar();
  }
}
