import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { API, mensajeError } from '../../nucleo/api';
import { AvisosService } from '../../compartido/avisos.service';

interface Expediente {
  id: number; usuario_id: number; solicitante: string; tipo: string; estado: string; detalle: string; respuesta: string;
  revisionConservacion?: boolean; ocupado?: boolean; estadoNuevo?: string;
}

/** Solicitudes de derechos ARCO (acceso, rectificación, cancelación y oposición) de los usuarios. */
@Component({
  selector: 'app-arco-admin', standalone: true, imports: [FormsModule],
  template: `
  <header class="cabeza-pagina"><div><h1>Solicitudes de privacidad</h1><p>Derechos ARCO pendientes de respuesta.</p></div><button class="boton secundario" (click)="cargar()">Actualizar</button></header>
  @for (s of solicitudes; track s.id) {
    <article class="panel">
      <header class="cabeza-panel"><h2>Folio {{ s.id }}: {{ tipos[s.tipo] || s.tipo }}</h2><span class="estado" [attr.data-tono]="s.estado === 'atendida' ? 'verde' : s.estado === 'rechazada' ? 'rojo' : 'ambar'">{{ s.estado }}</span></header>
      <p>{{ s.solicitante }}, cuenta {{ s.usuario_id }}</p>
      <p class="cita">{{ s.detalle }}</p>
      @if (s.estado === 'atendida' || s.estado === 'rechazada') { <p><strong>Respuesta.</strong> {{ s.respuesta }}</p> }
      @else {
        <form (ngSubmit)="guardar(s)" #f="ngForm">
          <label class="campo corto"><span>Estado</span><select name="estado" [(ngModel)]="s.estadoNuevo" required><option value="en_revision">En revisión</option><option value="atendida">Atendida</option><option value="rechazada">Rechazada</option></select></label>
          <label class="campo"><span>Respuesta y acciones realizadas</span><textarea name="respuesta" [(ngModel)]="s.respuesta" required minlength="10" maxlength="2000" rows="3"></textarea></label>
          @if (s.tipo === 'cancelacion') {
            <label class="casilla"><input type="checkbox" name="rev" [(ngModel)]="s.revisionConservacion"> Revisé qué datos deben conservarse. Al atenderla se anonimiza el perfil y se bloquea la cuenta.</label>
          }
          <button class="boton" [disabled]="f.invalid || s.ocupado">Guardar respuesta</button>
        </form>
      }
    </article>
  } @empty { <p class="vacio">No hay solicitudes pendientes.</p> }`
})
export class ArcoAdminComponent implements OnInit {
  solicitudes: Expediente[] = [];
  tipos: Record<string, string> = { acceso: 'Acceso', rectificacion: 'Rectificación', cancelacion: 'Cancelación', oposicion: 'Oposición' };
  constructor(private http: HttpClient, private avisos: AvisosService) {}
  ngOnInit(): void { this.cargar(); }
  cargar(): void { this.http.get<Expediente[]>(API + '/admin/arco').subscribe({ next: s => this.solicitudes = s, error: e => this.avisos.error(mensajeError(e)) }); }
  guardar(s: Expediente): void {
    s.ocupado = true;
    this.http.put(API + '/admin/arco/' + s.id, { estado: s.estadoNuevo, respuesta: s.respuesta, revisionConservacion: s.revisionConservacion === true }).subscribe({
      next: () => { this.avisos.exito('Respuesta registrada.'); this.cargar(); }, error: e => { s.ocupado = false; this.avisos.error(mensajeError(e)); }
    });
  }
}
