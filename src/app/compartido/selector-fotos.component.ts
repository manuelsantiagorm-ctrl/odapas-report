import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';

/** Permite elegir fotos (JPG, PNG o WEBP, máximo 5 MB cada una) y muestra una vista previa. */
@Component({
  selector: 'app-selector-fotos', standalone: true,
  template: `
  <div class="selector-fotos">
    <label class="soltar">
      <input type="file" accept="image/jpeg,image/png,image/webp" multiple (change)="agregar($event)" [disabled]="archivos.length >= max">
      <strong>{{ archivos.length >= max ? 'Llegaste al máximo de fotos' : etiqueta }}</strong>
      <small>JPG, PNG o WEBP, hasta 5 MB. Máximo {{ max }}.</small>
    </label>
    @if (error) { <p class="error-campo" role="alert">{{ error }}</p> }
    @if (archivos.length) {
      <ul class="miniaturas">
        @for (a of archivos; track a.url; let i = $index) {
          <li><img [src]="a.url" [alt]="'Foto ' + (i + 1)"><button type="button" (click)="quitar(i)" [attr.aria-label]="'Quitar foto ' + (i + 1)">Quitar</button></li>
        }
      </ul>
    }
  </div>`
})
export class SelectorFotosComponent implements OnDestroy {
  @Input() max = 3;
  @Input() etiqueta = 'Agregar fotos';
  @Output() cambio = new EventEmitter<File[]>();
  archivos: { archivo: File; url: string }[] = [];
  error = '';
  agregar(e: Event): void {
    const entrada = e.target as HTMLInputElement;
    this.error = '';
    for (const f of Array.from(entrada.files ?? [])) {
      if (this.archivos.length >= this.max) { this.error = 'Solo se permiten ' + this.max + ' fotos.'; break; }
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(f.type)) { this.error = f.name + ' no es una foto JPG, PNG o WEBP.'; continue; }
      if (f.size > 5 * 1024 * 1024) { this.error = f.name + ' pesa más de 5 MB.'; continue; }
      this.archivos.push({ archivo: f, url: URL.createObjectURL(f) });
    }
    entrada.value = '';
    this.cambio.emit(this.archivos.map(a => a.archivo));
  }
  quitar(i: number): void {
    URL.revokeObjectURL(this.archivos[i].url);
    this.archivos.splice(i, 1);
    this.cambio.emit(this.archivos.map(a => a.archivo));
  }
  limpiar(): void { this.archivos.forEach(a => URL.revokeObjectURL(a.url)); this.archivos = []; this.cambio.emit([]); }
  ngOnDestroy(): void { this.archivos.forEach(a => URL.revokeObjectURL(a.url)); }
}
