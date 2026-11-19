import { Component, Input, OnChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { API } from '../../nucleo/api';

/** Aviso de privacidad y términos, servidos por la API (siempre la versión vigente). */
@Component({
  selector: 'app-legal', standalone: true, imports: [RouterLink],
  template: `
  <article class="documento">
    <nav class="migas"><a routerLink="/legal/privacidad">Aviso de privacidad</a> <a routerLink="/legal/terminos">Términos y condiciones</a></nav>
    <h1>{{ tipo === 'terminos' ? 'Términos y condiciones' : 'Aviso de privacidad' }}</h1>
    @if (version) { <p class="tenue">Versión {{ version }}</p> }
    @if (error) { <p class="error-campo">{{ error }}</p> }
    <div class="texto-legal">{{ contenido }}</div>
  </article>`
})
export class LegalComponent implements OnChanges {
  @Input() tipo = 'privacidad';
  contenido = ''; version = ''; error = '';
  constructor(private http: HttpClient) {}
  ngOnChanges(): void {
    const clave = this.tipo === 'terminos' ? 'terminos' : 'privacidad';
    this.http.get<Record<string, string>>(API + '/publico/documentos').subscribe({
      next: d => { this.contenido = d[clave]; this.version = d['version']; },
      error: () => this.error = 'No se pudo cargar el documento. Revisa que la API esté encendida.'
    });
  }
}
