import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, ViewChild } from '@angular/core';
import * as L from 'leaflet';
import { textoEstado, textoTipo, tonoEstado } from '../nucleo/etiquetas';

export interface PuntoEnMapa { latitud: number | null; longitud: number | null; tipo?: string; estado?: string; colonia?: string; folio?: string; }

const COLORES: Record<string, string> = { gris: '#6B7F86', ambar: '#B7791F', azul: '#1F6FB2', turquesa: '#088395', verde: '#2F855A', rojo: '#B42318' };

/** Mapa de OpenStreetMap con puntos de reportes. Si "seleccionable" es true, un clic marca la ubicación. */
@Component({
  selector: 'app-mapa', standalone: true,
  template: `<div #lienzo class="mapa" [style.height]="alto" role="region" [attr.aria-label]="descripcion"></div>`
})
export class MapaComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('lienzo') lienzo!: ElementRef<HTMLDivElement>;
  @Input() puntos: PuntoEnMapa[] = [];
  @Input() seleccion: { latitud: number; longitud: number } | null = null;
  @Input() seleccionable = false;
  @Input() alto = '420px';
  @Input() descripcion = 'Mapa de reportes';
  @Output() elegir = new EventEmitter<{ latitud: number; longitud: number }>();
  @Output() abrir = new EventEmitter<string>();
  private mapa?: L.Map;
  private capa = L.layerGroup();
  private marca?: L.CircleMarker;

  ngAfterViewInit(): void {
    this.mapa = L.map(this.lienzo.nativeElement, { zoomControl: true, attributionControl: true }).setView([19.45, -99.13], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap' }).addTo(this.mapa);
    this.capa.addTo(this.mapa);
    if (this.seleccionable) this.mapa.on('click', (e: L.LeafletMouseEvent) =>
      this.elegir.emit({ latitud: +e.latlng.lat.toFixed(6), longitud: +e.latlng.lng.toFixed(6) }));
    this.dibujar(true);
    setTimeout(() => this.mapa?.invalidateSize(), 50);
  }
  ngOnChanges(): void { this.dibujar(false); }

  private dibujar(encuadrar: boolean): void {
    if (!this.mapa) return;
    this.capa.clearLayers();
    const validos = this.puntos.filter(p => p.latitud != null && p.longitud != null);
    for (const p of validos) {
      const color = COLORES[tonoEstado(p.estado ?? '')] ?? COLORES['azul'];
      const m = L.circleMarker([p.latitud!, p.longitud!], { radius: 8, color: '#fff', weight: 2, fillColor: color, fillOpacity: .95 });
      const titulo = (p.folio ? p.folio + ' — ' : '') + textoTipo(p.tipo ?? '');
      m.bindTooltip(`${titulo}<br>${p.colonia ?? ''}<br><b>${textoEstado(p.estado ?? '')}</b>`);
      if (p.folio) m.on('click', () => this.abrir.emit(p.folio!));
      m.addTo(this.capa);
    }
    this.marca?.remove();
    if (this.seleccion) {
      this.marca = L.circleMarker([this.seleccion.latitud, this.seleccion.longitud], { radius: 11, color: '#0A4D68', weight: 4, fillColor: '#fff', fillOpacity: 1 }).addTo(this.mapa);
      if (encuadrar || this.seleccionable) this.mapa.setView([this.seleccion.latitud, this.seleccion.longitud], Math.max(this.mapa.getZoom(), 16));
    } else if (encuadrar && validos.length) {
      this.mapa.fitBounds(L.latLngBounds(validos.map(p => [p.latitud!, p.longitud!] as L.LatLngTuple)).pad(.2), { maxZoom: 15 });
    }
  }
  ngOnDestroy(): void { this.mapa?.remove(); }
}
