import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, distinctUntilChanged, switchMap, of, catchError } from 'rxjs';
import { API } from '../nucleo/api';
import { Colonia } from '../nucleo/modelos';

/** Campo para buscar y elegir una colonia escribiendo su nombre (hay miles de colonias). */
@Component({
  selector: 'app-buscador-colonia', standalone: true, imports: [FormsModule],
  template: `
  <label class="campo">
    <span>{{ etiqueta }}</span>
    <input type="search" [ngModel]="texto" (ngModelChange)="escribir($event)" [placeholder]="colonia ? colonia.nombre : 'Escribe al menos 2 letras'"
      autocomplete="off" role="combobox" [attr.aria-expanded]="abiertas" aria-autocomplete="list" [required]="requerido && !colonia" (keydown.escape)="abiertas=false">
  </label>
  @if (colonia && !abiertas) { <p class="elegida">Elegiste <strong>{{ colonia.nombre }}</strong>, {{ colonia.municipio }} <button type="button" class="enlace" (click)="limpiar()">Cambiar</button></p> }
  @if (abiertas) {
    <ul class="opciones" role="listbox">
      @for (c of resultados; track c.id) {
        <li role="option" tabindex="0" (click)="elegir(c)" (keydown.enter)="elegir(c)">
          <strong>{{ c.nombre }}</strong> <span>{{ c.municipio }}@if (c.zona) {, zona {{ c.zona }}}</span>
        </li>
      } @empty { <li class="sin">No hay colonias con ese nombre.</li> }
    </ul>
  }`
})
export class BuscadorColoniaComponent implements OnDestroy {
  @Input() etiqueta = 'Colonia';
  @Input() requerido = true;
  @Input() ruta = API + '/publico/colonias';
  @Input() colonia: Colonia | null = null;
  @Output() coloniaChange = new EventEmitter<Colonia | null>();
  texto = ''; resultados: Colonia[] = []; abiertas = false;
  private consulta = new Subject<string>();
  private sub: Subscription;
  constructor(http: HttpClient) {
    this.sub = this.consulta.pipe(debounceTime(250), distinctUntilChanged(),
      switchMap(q => q.trim().length < 2 ? of(null) : http.get<Colonia[]>(this.ruta, { params: { q: q.trim() } }).pipe(catchError(() => of([])))))
      .subscribe(r => { this.resultados = r ?? []; this.abiertas = r !== null; });
  }
  escribir(t: string) { this.texto = t; this.consulta.next(t); }
  elegir(c: Colonia) { this.colonia = c; this.coloniaChange.emit(c); this.abiertas = false; this.texto = ''; }
  limpiar() { this.colonia = null; this.coloniaChange.emit(null); }
  ngOnDestroy() { this.sub.unsubscribe(); }
}
