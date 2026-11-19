import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { API, mensajeError } from '../../nucleo/api';
import { ReporteResumen } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';
import { EstadoComponent } from '../../compartido/estado.component';
import { textoTipo } from '../../nucleo/etiquetas';

/** Lista de los reportes que ha enviado el ciudadano. */
@Component({
  selector: 'app-mis-reportes', standalone: true, imports: [RouterLink, DatePipe, EstadoComponent],
  template: `
  <header class="cabeza-pagina">
    <div><h1>Mis reportes</h1><p>Toca un reporte para ver en qué va.</p></div>
    <a class="boton" routerLink="/nuevo-reporte">Nuevo reporte</a>
  </header>
  @if (!sesion.usuario?.correoVerificado) {
    <p class="nota">Verifica tu correo para poder enviar reportes. <a routerLink="/cuenta">Ver cómo</a></p>
  }
  @if (error) { <p class="error-campo">{{ error }}</p> }
  @if (cargando) { <p class="tenue">Cargando…</p> }
  <ul class="lista-reportes">
    @for (r of reportes; track r.folio) {
      <li>
        <a [routerLink]="['/reporte', r.folio]">
          <span class="folio">{{ r.folio }}</span>
          <strong>{{ tipo(r.tipo) }}</strong>
          <span class="lugar">{{ r.calle }}, {{ r.colonia }}</span>
          <time>{{ (r.fechaCreacion || r.fecha_creacion) | date:'d MMM y' }}</time>
          <app-estado [estado]="r.estado" />
        </a>
      </li>
    } @empty {
      @if (!cargando) {
        <li class="vacio"><p>Aún no has enviado reportes. Si ves una fuga o te falta el agua, avísanos.</p><a class="boton" routerLink="/nuevo-reporte">Hacer mi primer reporte</a></li>
      }
    }
  </ul>`
})
export class MisReportesComponent implements OnInit {
  reportes: ReporteResumen[] = []; cargando = true; error = '';
  constructor(private http: HttpClient, public sesion: SesionService) {}
  ngOnInit(): void {
    this.http.get<ReporteResumen[]>(API + '/reportes').subscribe({
      next: r => { this.reportes = r.sort((a, b) => (b.fechaCreacion ?? '').localeCompare(a.fechaCreacion ?? '')); this.cargando = false; },
      error: e => { this.error = mensajeError(e); this.cargando = false; }
    });
  }
  tipo = textoTipo;
}
