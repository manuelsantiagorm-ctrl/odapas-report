import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { API, mensajeError } from '../../nucleo/api';
import { Colonia, Zona } from '../../nucleo/modelos';
import { AvisosService } from '../../compartido/avisos.service';
import { BuscadorColoniaComponent } from '../../compartido/buscador-colonia.component';

/** Zonas de atención y qué colonias cubre cada una. Los reportes se asignan a la zona de su colonia. */
@Component({
  selector: 'app-zonas-admin', standalone: true, imports: [FormsModule, BuscadorColoniaComponent],
  template: `
  <header class="cabeza-pagina"><div><h1>Zonas y colonias</h1><p>Cada colonia pertenece a una zona; así el reporte llega al personal correcto.</p></div></header>
  <div class="dos-columnas">
    <section class="panel">
      <h2>Asignar una colonia a una zona</h2>
      <app-buscador-colonia [ruta]="api + '/colonias'" [(colonia)]="colonia" [requerido]="false" />
      @if (colonia) {
        <p>Zona actual: <strong>{{ colonia.zona || 'ninguna' }}</strong></p>
        <div class="rejilla">
          <label class="campo"><span>Nueva zona</span><select [(ngModel)]="zonaColonia"><option [ngValue]="null">Quitar de toda zona</option>@for (z of activas; track z.id) { <option [ngValue]="z.id">{{ z.codigo }}: {{ z.nombre }}</option> }</select></label>
        </div>
        <button class="boton" (click)="cobertura()" [disabled]="ocupado">Guardar cobertura</button>
      }
    </section>
    <section class="panel">
      <h2>Nueva zona</h2>
      <form (ngSubmit)="crear()" #fz="ngForm">
        <div class="rejilla">
          <label class="campo"><span>Código</span><input name="codigo" [(ngModel)]="nueva.codigo" required maxlength="20" placeholder="ZO"></label>
          <label class="campo"><span>Nombre</span><input name="nombre" [(ngModel)]="nueva.nombre" required minlength="2" maxlength="100" placeholder="Zona Oriente"></label>
        </div>
        <label class="campo"><span>Descripción (opcional)</span><input name="desc" [(ngModel)]="nueva.descripcion" maxlength="500"></label>
        <button class="boton secundario" [disabled]="ocupado || fz.invalid">Crear zona</button>
      </form>
    </section>
  </div>
  <div class="filtros"><label class="campo"><span>Buscar zona</span><input type="search" [(ngModel)]="busqueda" placeholder="Código o nombre"></label></div>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  <div class="tabla-envoltura">
    <table class="tabla">
      <thead><tr><th>Zona</th><th>Colonias</th><th>Personal</th><th>Estado</th><th></th></tr></thead>
      <tbody>
        @for (z of filtradas; track z.id) {
          <tr>
            @if (editando === z.id) {
              <td colspan="4"><div class="rejilla">
                <label class="campo"><span>Nombre</span><input [(ngModel)]="edicion.nombre" maxlength="100"></label>
                <label class="campo"><span>Descripción</span><input [(ngModel)]="edicion.descripcion" maxlength="500"></label>
                <label class="casilla"><input type="checkbox" [(ngModel)]="edicion.activa"> Activa</label>
              </div></td>
              <td class="acciones"><button class="boton" (click)="guardar(z)">Guardar</button><button class="enlace" (click)="editando = 0">Cancelar</button></td>
            } @else {
              <td><strong>{{ z.codigo }}</strong> {{ z.nombre }}<span class="recorte">{{ z.descripcion }}</span></td>
              <td>{{ nombresColonias(z) }}</td>
              <td [class.falta]="!z.trabajadores_disponibles">{{ z.trabajadores_disponibles || 'Sin personal' }}</td>
              <td><span class="estado" [attr.data-tono]="z.activa ? 'verde' : 'gris'">{{ z.activa ? 'Activa' : 'Inactiva' }}</span></td>
              <td><button class="enlace" (click)="editar(z)">Editar</button></td>
            }
          </tr>
        }
      </tbody>
    </table>
  </div>`
})
export class ZonasAdminComponent implements OnInit {
  zonas: Zona[] = []; error = ''; ocupado = false; busqueda = '';
  colonia: Colonia | null = null; zonaColonia: number | null = null;
  nueva = { codigo: '', nombre: '', descripcion: '' };
  editando = 0; edicion = { nombre: '', descripcion: '', activa: true };
  api = API + '/admin';
  constructor(private http: HttpClient, private avisos: AvisosService) {}
  ngOnInit(): void { this.cargar(); }
  cargar(): void { this.http.get<Zona[]>(this.api + '/zonas').subscribe({ next: z => this.zonas = z, error: e => this.error = mensajeError(e) }); }
  get activas() { return this.zonas.filter(z => z.activa); }
  get filtradas() {
    const q = this.busqueda.trim().toLowerCase();
    return this.zonas.filter(z => !q || z.codigo.toLowerCase().includes(q) || z.nombre.toLowerCase().includes(q)).slice(0, 100);
  }
  nombresColonias(z: Zona) {
    const c = z.colonias ?? [];
    return c.length ? c.slice(0, 6).map(x => x.nombre).join(', ') + (c.length > 6 ? ' y ' + (c.length - 6) + ' más' : '') : 'Sin colonias';
  }
  private hacer(p: Observable<unknown>, exito: string, despues?: () => void): void {
    this.ocupado = true;
    p.pipe(finalize(() => this.ocupado = false)).subscribe({ next: () => { this.avisos.exito(exito); despues?.(); this.cargar(); }, error: e => this.avisos.error(mensajeError(e)) });
  }
  cobertura(): void {
    this.hacer(this.http.put(this.api + '/colonias/' + this.colonia!.id + '/zona', { zonaId: this.zonaColonia }), 'Cobertura actualizada.', () => this.colonia = null);
  }
  crear(): void { this.hacer(this.http.post(this.api + '/zonas', this.nueva), 'Zona creada.', () => this.nueva = { codigo: '', nombre: '', descripcion: '' }); }
  editar(z: Zona): void { this.editando = z.id; this.edicion = { nombre: z.nombre, descripcion: z.descripcion ?? '', activa: z.activa }; }
  guardar(z: Zona): void { this.hacer(this.http.put(this.api + '/zonas/' + z.id, this.edicion), 'Zona actualizada.', () => this.editando = 0); }
}
