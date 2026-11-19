import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { finalize, Observable } from 'rxjs';
import { API, mensajeError } from '../../nucleo/api';
import { RespuestaEnlace, Trabajador, Zona } from '../../nucleo/modelos';
import { AvisosService } from '../../compartido/avisos.service';
import { EnlaceDemoComponent } from '../../compartido/enlace-demo.component';

/** Alta del personal de campo, invitaciones, cambio de zona y suspensión. */
@Component({
  selector: 'app-personal-admin', standalone: true, imports: [FormsModule, DatePipe, EnlaceDemoComponent],
  template: `
  <header class="cabeza-pagina"><div><h1>Personal de campo</h1><p>Los trabajadores reciben una invitación para crear su contraseña.</p></div>
    <button class="boton" (click)="mostrarAlta = !mostrarAlta">{{ mostrarAlta ? 'Cerrar formulario' : 'Dar de alta' }}</button></header>
  @if (mostrarAlta) {
    <section class="panel paso-actual">
      <h2>Nuevo trabajador</h2>
      <form (ngSubmit)="alta()" #fa="ngForm">
        <div class="rejilla">
          <label class="campo"><span>Nombre completo</span><input name="nombre" [(ngModel)]="n.nombre" required minlength="2" maxlength="150"></label>
          <label class="campo"><span>Correo</span><input name="email" type="email" [(ngModel)]="n.email" required maxlength="150"></label>
          <label class="campo"><span>Teléfono</span><input name="tel" [(ngModel)]="n.telefono" required minlength="5" maxlength="20"></label>
          <label class="campo"><span>Número de empleado</span><input name="num" [(ngModel)]="n.numeroEmpleado" required maxlength="30"></label>
          <label class="campo"><span>Zona</span><select name="zona" [(ngModel)]="n.zonaId" required>@for (z of zonasActivas; track z.id) { <option [ngValue]="z.id">{{ z.codigo }}: {{ z.nombre }}</option> }</select></label>
        </div>
        <button class="boton" [disabled]="ocupado || fa.invalid">Registrar y generar invitación</button>
      </form>
    </section>
  }
  <app-enlace-demo [respuesta]="invitacion" />
  <div class="filtros">
    <label class="campo"><span>Buscar</span><input type="search" [(ngModel)]="busqueda" placeholder="Nombre, correo, número o zona"></label>
    <label class="campo"><span>Estado</span><select [(ngModel)]="filtroEstado"><option value="">Todos</option><option value="activo">Activos</option><option value="pendiente">Invitación pendiente</option><option value="suspendido">Suspendidos</option></select></label>
  </div>
  @if (error) { <p class="error-campo">{{ error }}</p> }
  <div class="tabla-envoltura">
    <table class="tabla">
      <thead><tr><th>Trabajador</th><th>Contacto</th><th>Zona</th><th>Órdenes activas</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>
        @for (t of filtrados; track t.id) {
          <tr>
            <td><strong>{{ t.nombre }}</strong><span class="recorte">{{ t.numero_empleado }}, alta {{ t.registrado_en | date:'d MMM y' }}</span></td>
            <td>{{ t.correo }}<span class="recorte">{{ t.telefono }}</span></td>
            <td>
              @if (editandoZona === t.id) {
                <select [(ngModel)]="zonaNueva" [attr.aria-label]="'Zona de ' + t.nombre">@for (z of zonasActivas; track z.id) { <option [ngValue]="z.id">{{ z.codigo }}</option> }</select>
                <button class="enlace" (click)="hacer(http.put(api + t.id + '/zona', { zonaId: zonaNueva }), 'Zona actualizada.'); editandoZona = 0">Guardar</button>
              } @else { {{ t.zona || 'Sin zona' }} <button class="enlace" (click)="editandoZona = t.id; zonaNueva = t.zona_id">Cambiar</button> }
            </td>
            <td>{{ t.ordenes_activas }}</td>
            <td><span class="estado" [attr.data-tono]="t.estado === 'activo' ? 'verde' : t.estado === 'pendiente' ? 'ambar' : 'rojo'">{{ estados[t.estado] || t.estado }}</span></td>
            <td class="acciones">
              @if (t.estado === 'pendiente') { <button class="enlace" (click)="reenviar(t)">Nueva invitación</button> }
              @if (t.estado === 'activo') { <button class="enlace peligro" (click)="hacer(http.put(api + t.id + '/estado', { estado: 'suspendido' }), 'Trabajador suspendido; sus sesiones se cerraron.')">Suspender</button> }
              @if (t.estado === 'suspendido') { <button class="enlace" (click)="hacer(http.put(api + t.id + '/estado', { estado: 'activo' }), 'Trabajador reactivado.')">Reactivar</button> }
            </td>
          </tr>
        } @empty { <tr><td colspan="6" class="vacio">No hay personal con esos filtros.</td></tr> }
      </tbody>
    </table>
  </div>`
})
export class PersonalAdminComponent implements OnInit {
  trabajadores: Trabajador[] = []; zonas: Zona[] = []; error = ''; ocupado = false; mostrarAlta = false;
  n = { nombre: '', email: '', telefono: '', numeroEmpleado: '', zonaId: null as number | null };
  invitacion: RespuestaEnlace | null = null; busqueda = ''; filtroEstado = ''; editandoZona = 0; zonaNueva: number | null = null;
  estados: Record<string, string> = { activo: 'Activo', pendiente: 'Invitación pendiente', suspendido: 'Suspendido' };
  api = API + '/admin/trabajadores/';
  constructor(public http: HttpClient, private avisos: AvisosService) {}
  ngOnInit(): void {
    this.cargar();
    this.http.get<Zona[]>(API + '/admin/zonas').subscribe({ next: z => this.zonas = z, error: () => {} });
  }
  get zonasActivas() { return this.zonas.filter(z => z.activa); }
  get filtrados() {
    const q = this.busqueda.trim().toLowerCase();
    return this.trabajadores.filter(t => (!this.filtroEstado || t.estado === this.filtroEstado) &&
      (!q || [t.nombre, t.correo, t.numero_empleado, t.zona ?? ''].some(x => x.toLowerCase().includes(q)))).slice(0, 200);
  }
  cargar(): void { this.http.get<Trabajador[]>(API + '/admin/trabajadores').subscribe({ next: t => this.trabajadores = t, error: e => this.error = mensajeError(e) }); }
  hacer(p: Observable<unknown>, exito: string): void {
    this.ocupado = true;
    p.pipe(finalize(() => this.ocupado = false)).subscribe({ next: () => { this.avisos.exito(exito); this.cargar(); }, error: e => this.avisos.error(mensajeError(e)) });
  }
  alta(): void {
    this.ocupado = true;
    this.http.post<RespuestaEnlace>(API + '/admin/trabajadores', this.n).pipe(finalize(() => this.ocupado = false)).subscribe({
      next: r => { this.invitacion = r; this.mostrarAlta = false; this.n = { nombre: '', email: '', telefono: '', numeroEmpleado: '', zonaId: null }; this.cargar(); },
      error: e => this.avisos.error(mensajeError(e))
    });
  }
  reenviar(t: Trabajador): void {
    this.http.post<RespuestaEnlace>(this.api + t.id + '/invitacion', {}).subscribe({ next: r => { this.invitacion = r; window.scrollTo({ top: 0 }); }, error: e => this.avisos.error(mensajeError(e)) });
  }
}
