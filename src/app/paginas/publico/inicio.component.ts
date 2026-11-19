import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { API } from '../../nucleo/api';
import { PuntoMapa } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';
import { MapaComponent } from '../../compartido/mapa.component';
import { textoTipo } from '../../nucleo/etiquetas';

/** Página de inicio: qué es el servicio, el estado de la red hoy y el mapa público de incidentes. */
@Component({
  selector: 'app-inicio', standalone: true, imports: [RouterLink, MapaComponent, DecimalPipe],
  template: `
  <section class="portada">
    <div class="portada-texto">
      <h1>¿Hay una fuga o te falta el agua?</h1>
      <p class="entrada">Repórtalo en dos minutos con una foto y la ubicación. Te avisamos cuando la cuadrilla llegue y cuando el problema quede resuelto.</p>
      <div class="acciones">
        @if (sesion.activa) {
          <a class="boton grande" [routerLink]="sesion.tiene('ciudadano') ? '/nuevo-reporte' : sesion.inicio">{{ sesion.tiene('ciudadano') ? 'Hacer un reporte' : 'Ir a mi panel' }}</a>
        } @else {
          <a class="boton grande" routerLink="/registro">Crear cuenta y reportar</a>
          <a class="boton secundario grande" routerLink="/entrar">Ya tengo cuenta</a>
        }
      </div>
    </div>
    <dl class="medidor" aria-label="Reportes en el municipio">
      <div class="lectura"><dt>Reportes abiertos</dt><dd>{{ resumen.abiertos | number }}</dd></div>
      <div><dt>En atención ahora</dt><dd>{{ resumen.en_atencion | number }}</dd></div>
      <div><dt>Resueltos</dt><dd>{{ resumen.resueltos | number }}</dd></div>
    </dl>
  </section>

  <section class="bloque">
    <div class="bloque-cabeza">
      <h2>Incidentes abiertos en el mapa</h2>
      <p>Solo se muestra el tipo de problema, la colonia y su avance. Nunca datos de quien reporta.</p>
    </div>
    <app-mapa [puntos]="puntos" alto="460px" descripcion="Mapa público de incidentes abiertos" />
    <ul class="leyenda">
      @for (t of conteoTipos; track t.tipo) { <li><strong>{{ t.total }}</strong> {{ t.texto }}</li> }
      @empty { <li>No hay incidentes abiertos con ubicación en este momento.</li> }
    </ul>
  </section>

  <section class="bloque pasos">
    <h2>Cómo se atiende tu reporte</h2>
    <ol>
      <li><strong>Lo envías</strong><span>Eliges el tipo de problema, la ubicación y agregas hasta 3 fotos.</span></li>
      <li><strong>ODAPAS lo revisa</strong><span>Personal del organismo confirma el problema y lo asigna a la zona que corresponde.</span></li>
      <li><strong>La cuadrilla lo repara</strong><span>El trabajador registra el diagnóstico y fotos del antes y el después.</span></li>
      <li><strong>Te avisamos</strong><span>Recibes un aviso en cada cambio y puedes consultar el historial completo.</span></li>
    </ol>
  </section>`
})
export class InicioComponent implements OnInit {
  resumen = { abiertos: 0, en_atencion: 0, resueltos: 0, total: 0 };
  puntos: PuntoMapa[] = [];
  conteoTipos: { tipo: string; texto: string; total: number }[] = [];
  constructor(private http: HttpClient, public sesion: SesionService) {}
  ngOnInit(): void {
    this.http.get<typeof this.resumen>(API + '/publico/resumen').subscribe({ next: r => this.resumen = r, error: () => {} });
    this.http.get<PuntoMapa[]>(API + '/publico/mapa').subscribe({
      next: p => {
        this.puntos = p;
        const c: Record<string, number> = {};
        p.forEach(x => c[x.tipo] = (c[x.tipo] ?? 0) + 1);
        this.conteoTipos = Object.entries(c).map(([tipo, total]) => ({ tipo, total, texto: textoTipo(tipo).toLowerCase() }));
      }, error: () => {}
    });
  }
}
