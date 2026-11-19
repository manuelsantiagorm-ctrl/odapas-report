import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap, of, map } from 'rxjs';
import { API, mensajeError } from '../../nucleo/api';
import { Colonia, DetalleReporte, RespuestaEnlace, TipoReporte } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';
import { BuscadorColoniaComponent } from '../../compartido/buscador-colonia.component';
import { MapaComponent } from '../../compartido/mapa.component';
import { SelectorFotosComponent } from '../../compartido/selector-fotos.component';
import { EnlaceDemoComponent } from '../../compartido/enlace-demo.component';
import { AvisosService } from '../../compartido/avisos.service';

const AYUDA: Record<string, string> = {
  fuga: 'Agua que sale de una tubería, toma o medidor.',
  falta_agua: 'No llega agua a tu domicilio o a la calle.',
  baja_presion: 'El agua llega, pero con muy poca fuerza.'
};

/** Formulario para enviar un reporte nuevo con ubicación y fotos. */
@Component({
  selector: 'app-nuevo-reporte', standalone: true,
  imports: [FormsModule, RouterLink, BuscadorColoniaComponent, MapaComponent, SelectorFotosComponent, EnlaceDemoComponent],
  template: `
  <header class="cabeza-pagina"><div><h1>Nuevo reporte</h1><p>Entre más precisa la ubicación, más rápido llega la cuadrilla.</p></div></header>
  @if (!verificado) {
    <div class="nota">
      <p>Antes de reportar necesitas verificar tu correo. Así evitamos reportes falsos.</p>
      @if (!enlace) { <button class="boton secundario" type="button" (click)="reenviar()" [disabled]="ocupado">Enviarme el enlace de verificación</button> }
      <app-enlace-demo [respuesta]="enlace" />
    </div>
  }
  <form class="formulario" (ngSubmit)="enviar()" #f="ngForm">
    <fieldset>
      <legend>¿Qué problema hay?</legend>
      <div class="opciones-tipo">
        @for (t of tipos; track t.codigo) {
          <label class="opcion-tipo" [class.elegida]="d.tipo === t.codigo">
            <input type="radio" name="tipo" [value]="t.codigo" [(ngModel)]="d.tipo" required>
            <strong>{{ t.nombre }}</strong><span>{{ ayuda[t.codigo] || '' }}</span>
          </label>
        }
      </div>
      <label class="campo"><span>Describe lo que ves</span>
        <textarea name="descripcion" [(ngModel)]="d.descripcion" required minlength="10" maxlength="2000" rows="4" placeholder="Ejemplo: sale mucha agua de la banqueta frente a la tienda desde la mañana"></textarea>
        <small>{{ d.descripcion.length }} de 2000 caracteres. Mínimo 10.</small>
      </label>
    </fieldset>

    <fieldset>
      <legend>¿Dónde está?</legend>
      <div class="segmentado" role="radiogroup">
        <label [class.elegida]="usarDomicilio"><input type="radio" name="donde" [value]="true" [(ngModel)]="usarDomicilio"> En mi domicilio</label>
        <label [class.elegida]="!usarDomicilio"><input type="radio" name="donde" [value]="false" [(ngModel)]="usarDomicilio"> En otra dirección</label>
      </div>
      @if (!usarDomicilio) {
        <app-buscador-colonia [(colonia)]="colonia" />
        <div class="rejilla">
          <label class="campo"><span>Calle</span><input name="calle" [(ngModel)]="d.calle" required maxlength="150"></label>
          <label class="campo"><span>Número exterior</span><input name="numeroExterior" [(ngModel)]="d.numeroExterior" required maxlength="20" placeholder="S/N si no tiene"></label>
          <label class="campo"><span>Número interior (opcional)</span><input name="numeroInterior" [(ngModel)]="d.numeroInterior" maxlength="20"></label>
          <label class="campo"><span>Código postal</span><input name="codigoPostal" [(ngModel)]="d.codigoPostal" required pattern="[0-9]{5}" maxlength="5" inputmode="numeric"></label>
        </div>
      }
      <label class="campo"><span>Referencias (opcional)</span><input name="referencias" [(ngModel)]="d.referencias" maxlength="500" placeholder="Entre qué calles, color de la casa, frente a qué negocio"></label>
      <div class="ubicacion">
        <p>Marca el punto exacto tocando el mapa{{ gpsDisponible ? ' o usa tu ubicación' : '' }}. Es opcional, pero ayuda mucho.</p>
        <div class="acciones">
          @if (gpsDisponible) { <button type="button" class="boton secundario" (click)="gps()" [disabled]="buscandoGps">{{ buscandoGps ? 'Buscando…' : 'Usar mi ubicación' }}</button> }
          @if (punto) { <button type="button" class="enlace" (click)="punto = null">Quitar punto</button> }
        </div>
        <app-mapa [seleccionable]="true" [seleccion]="punto" (elegir)="punto = $event" alto="320px" descripcion="Toca el mapa para marcar dónde está el problema" />
        @if (punto) { <p class="tenue">Punto marcado: {{ punto.latitud }}, {{ punto.longitud }}</p> }
      </div>
    </fieldset>

    <fieldset>
      <legend>Fotos (opcional)</legend>
      <app-selector-fotos [max]="3" (cambio)="fotos = $event" etiqueta="Tomar o elegir fotos" />
    </fieldset>

    @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
    <div class="acciones fin">
      <a routerLink="/mis-reportes" class="boton fantasma">Cancelar</a>
      <button class="boton grande" type="submit" [disabled]="ocupado || f.invalid || !verificado || (!usarDomicilio && !colonia)">{{ ocupado ? 'Enviando…' : 'Enviar reporte' }}</button>
    </div>
  </form>`
})
export class NuevoReporteComponent implements OnInit {
  tipos: TipoReporte[] = []; ayuda = AYUDA;
  d = { tipo: '', descripcion: '', calle: '', numeroExterior: '', numeroInterior: '', codigoPostal: '', referencias: '' };
  usarDomicilio = true; colonia: Colonia | null = null;
  punto: { latitud: number; longitud: number } | null = null;
  fotos: File[] = [];
  ocupado = false; error = ''; buscandoGps = false; enlace: RespuestaEnlace | null = null;
  gpsDisponible = typeof navigator !== 'undefined' && 'geolocation' in navigator;
  constructor(private http: HttpClient, private router: Router, private sesion: SesionService, private avisos: AvisosService) {}
  get verificado() { return this.sesion.usuario?.correoVerificado === true; }

  ngOnInit(): void {
    this.http.get<TipoReporte[]>(API + '/publico/tipos-reporte').subscribe({ next: t => this.tipos = t, error: e => this.error = mensajeError(e) });
    // Confirma con el servidor si el correo ya se verificó (pudo hacerse en otra pestaña).
    this.http.get<{ correoVerificado: boolean }>(API + '/cuenta').subscribe({ next: p => this.sesion.actualizarUsuario({ correoVerificado: p.correoVerificado }), error: () => {} });
  }
  gps(): void {
    this.buscandoGps = true;
    navigator.geolocation.getCurrentPosition(
      p => { this.punto = { latitud: +p.coords.latitude.toFixed(6), longitud: +p.coords.longitude.toFixed(6) }; this.buscandoGps = false; },
      () => { this.error = 'No se pudo obtener tu ubicación. Marca el punto en el mapa.'; this.buscandoGps = false; },
      { enableHighAccuracy: true, timeout: 10000 });
  }
  reenviar(): void {
    this.ocupado = true;
    this.http.post<RespuestaEnlace>(API + '/auth/reenviar-verificacion', {}).subscribe({
      next: r => { this.enlace = r; this.ocupado = false; },
      error: e => { this.ocupado = false; if (e.status === 409) this.sesion.actualizarUsuario({ correoVerificado: true }); else this.error = mensajeError(e); }
    });
  }
  enviar(): void {
    this.ocupado = true; this.error = '';
    const cuerpo: Record<string, unknown> = { tipo: this.d.tipo, descripcion: this.d.descripcion.trim(), referencias: this.d.referencias || null, usarDomicilio: this.usarDomicilio };
    if (!this.usarDomicilio) Object.assign(cuerpo, { coloniaId: this.colonia?.id, calle: this.d.calle, numeroExterior: this.d.numeroExterior, numeroInterior: this.d.numeroInterior || null, codigoPostal: this.d.codigoPostal });
    if (this.punto) Object.assign(cuerpo, this.punto);
    this.http.post<DetalleReporte>(API + '/reportes', cuerpo).pipe(
      switchMap(r => {
        if (!this.fotos.length) return of(r);
        const datos = new FormData();
        this.fotos.forEach(f => datos.append('fotos', f, f.name));
        return this.http.post(API + '/reportes/' + r.folio + '/fotos', datos).pipe(map(() => r));
      })
    ).subscribe({
      next: r => { this.avisos.exito('Reporte ' + r.folio + ' enviado. Te avisaremos de cada avance.'); void this.router.navigate(['/reporte', r.folio]); },
      error: e => { this.error = mensajeError(e); this.ocupado = false; }
    });
  }
}
