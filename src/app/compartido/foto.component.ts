import { Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/**
 * Muestra una foto protegida. Las fotos solo se entregan con sesión, por eso
 * se descargan con el token (HttpClient) y se muestran como imagen local.
 */
@Component({
  selector: 'app-foto', standalone: true,
  template: `
  @if (src) {
    <a [href]="src" target="_blank" rel="noopener" class="foto"><img [src]="src" [alt]="alt" loading="lazy"></a>
  } @else {
    <span class="foto foto-vacia" role="img" [attr.aria-label]="error ? 'No se pudo cargar la foto' : 'Cargando foto'">{{ error ? 'Sin acceso' : '' }}</span>
  }`
})
export class FotoComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) url = '';
  @Input() alt = 'Fotografía';
  src = ''; error = false;
  constructor(private http: HttpClient) {}
  ngOnChanges(): void {
    this.liberar();
    if (!this.url) return;
    this.http.get(this.url, { responseType: 'blob' }).subscribe({
      next: b => this.src = URL.createObjectURL(b),
      error: () => this.error = true
    });
  }
  private liberar() { if (this.src) URL.revokeObjectURL(this.src); this.src = ''; this.error = false; }
  ngOnDestroy(): void { this.liberar(); }
}
