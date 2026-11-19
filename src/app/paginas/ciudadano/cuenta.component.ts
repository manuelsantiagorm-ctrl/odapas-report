import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';
import { API, mensajeError } from '../../nucleo/api';
import { Colonia, RespuestaEnlace } from '../../nucleo/modelos';
import { SesionService } from '../../nucleo/sesion.service';
import { AvisosService } from '../../compartido/avisos.service';
import { BuscadorColoniaComponent } from '../../compartido/buscador-colonia.component';
import { EnlaceDemoComponent } from '../../compartido/enlace-demo.component';

interface Perfil {
  name: string; email: string; telefono: string; coloniaId: number | null; calle: string | null; numeroExterior: string | null;
  codigoPostal: string | null; opcionales: boolean; correoVerificado: boolean; passwordActual?: string;
}
interface Solicitud { id: number; tipo: string; detalle: string; estado: string; respuesta: string | null; creado_en: string; }

const SECCIONES = [
  { id: 'datos', texto: 'Mis datos' }, { id: 'seguridad', texto: 'Contraseña' },
  { id: 'privacidad', texto: 'Privacidad y derechos ARCO' }, { id: 'baja', texto: 'Darme de baja' }
];

/** Datos personales, verificación de correo, contraseña y derechos de privacidad (ARCO). */
@Component({
  selector: 'app-cuenta', standalone: true, imports: [CommonModule, FormsModule, RouterLink, BuscadorColoniaComponent, EnlaceDemoComponent],
  template: `
  <header class="cabeza-pagina"><div><h1>Mi cuenta</h1><p>Tus datos solo los usa ODAPAS para atender tus reportes.</p></div></header>
  <nav class="pestanas" role="tablist">
    @for (s of secciones; track s.id) { <button role="tab" type="button" [attr.aria-selected]="vista === s.id" (click)="vista = s.id">{{ s.texto }}</button> }
  </nav>

  @if (!legalVigente) {
    <section class="panel nota">
      <h2>Actualizamos los documentos legales</h2>
      <p>Revisa la versión {{ version }} del <a routerLink="/legal/privacidad">aviso de privacidad</a> y los <a routerLink="/legal/terminos">términos</a>.</p>
      <label class="casilla"><input type="checkbox" [(ngModel)]="terms"> Acepto los términos y condiciones</label>
      <label class="casilla"><input type="checkbox" [(ngModel)]="privacy"> Acepto el aviso de privacidad</label>
      <button class="boton" (click)="aceptar()" [disabled]="!terms || !privacy">Registrar aceptación</button>
    </section>
  }

  @if (vista === 'datos' && perfil; as p) {
    <section class="panel">
      <header class="cabeza-panel"><h2>Correo</h2><span class="estado" [attr.data-tono]="p.correoVerificado ? 'verde' : 'ambar'">{{ p.correoVerificado ? 'Verificado' : 'Sin verificar' }}</span></header>
      @if (!p.correoVerificado) {
        <p>Verifica tu correo para poder enviar reportes.</p>
        @if (!enlace) { <button class="boton secundario" (click)="reenviar()" [disabled]="ocupado">Enviarme el enlace de verificación</button> }
        <app-enlace-demo [respuesta]="enlace" />
      } @else { <p class="tenue">{{ p.email }}</p> }
    </section>
    <section class="panel">
      <h2>Datos personales</h2>
      <form (ngSubmit)="guardar()" #datos="ngForm">
        <div class="rejilla">
          <label class="campo"><span>Nombre completo</span><input name="name" [(ngModel)]="p.name" required maxlength="150" autocomplete="name"></label>
          <label class="campo"><span>Correo</span><input name="email" type="email" [(ngModel)]="p.email" required maxlength="150" autocomplete="email"><small>Si lo cambias tendrás que verificarlo de nuevo.</small></label>
          <label class="campo"><span>Teléfono</span><input name="telefono" [(ngModel)]="p.telefono" required minlength="5" maxlength="20" autocomplete="tel"></label>
        </div>
        @if (tieneDomicilio) {
          <h3>Domicilio</h3>
          <app-buscador-colonia [(colonia)]="colonia" [requerido]="false" etiqueta="Cambiar colonia (déjalo vacío para conservar la actual)" />
          <div class="rejilla">
            <label class="campo"><span>Calle</span><input name="calle" [(ngModel)]="p.calle" required maxlength="150"></label>
            <label class="campo"><span>Número exterior</span><input name="numero" [(ngModel)]="p.numeroExterior" required maxlength="20"></label>
            <label class="campo"><span>Código postal</span><input name="cp" [(ngModel)]="p.codigoPostal" required pattern="[0-9]{5}" maxlength="5" inputmode="numeric"></label>
          </div>
        }
        <label class="campo corto"><span>Tu contraseña actual, para confirmar</span><input name="passwordActual" type="password" [(ngModel)]="p.passwordActual" required autocomplete="current-password"></label>
        <div class="acciones">
          <button class="boton" type="submit" [disabled]="ocupado || datos.invalid">Guardar cambios</button>
          <button class="boton secundario" type="button" (click)="descargar()">Descargar mis datos</button>
        </div>
      </form>
    </section>
  }

  @if (vista === 'seguridad') {
    <section class="panel">
      <h2>Cambiar contraseña</h2>
      <form (ngSubmit)="cambiarPassword()" #clave="ngForm" class="corto">
        <label class="campo"><span>Contraseña actual</span><input name="actual" type="password" [(ngModel)]="passwordActual" required autocomplete="current-password"></label>
        <label class="campo"><span>Nueva contraseña</span><input name="nueva" type="password" [(ngModel)]="passwordNueva" required minlength="10" maxlength="128" autocomplete="new-password"><small>Mínimo 10 caracteres, con letras y números.</small></label>
        <label class="campo"><span>Repite la nueva contraseña</span><input name="confirmacion" type="password" [(ngModel)]="passwordConfirmacion" required autocomplete="new-password"></label>
        <p class="tenue">Se cerrarán todas tus sesiones y tendrás que entrar de nuevo.</p>
        <button class="boton" type="submit" [disabled]="ocupado || clave.invalid">Cambiar contraseña</button>
      </form>
    </section>
  }

  @if (vista === 'privacidad') {
    @if (perfil; as p) {
      <section class="panel">
        <h2>Comunicaciones opcionales</h2>
        <label class="casilla"><input type="checkbox" [(ngModel)]="p.opcionales"> Autorizo recibir comunicaciones informativas de ODAPAS</label>
        <p class="tenue">Es opcional y no afecta la atención de tus reportes.</p>
        <button class="boton secundario" (click)="preferencias()">Guardar preferencia</button>
      </section>
    }
    <section class="panel">
      <h2>Ejercer mis derechos ARCO</h2>
      <p>Acceso: saber qué datos tenemos. Rectificación: corregirlos. Cancelación: pedir que se eliminen. Oposición: negarte a un uso específico.</p>
      <form (ngSubmit)="solicitar()" #arco="ngForm">
        <label class="campo corto"><span>Derecho</span><select name="tipo" [(ngModel)]="tipo"><option value="acceso">Acceso</option><option value="rectificacion">Rectificación</option><option value="cancelacion">Cancelación</option><option value="oposicion">Oposición</option></select></label>
        <label class="campo"><span>Describe tu solicitud</span><textarea name="detalle" [(ngModel)]="detalle" required minlength="5" maxlength="2000" rows="4"></textarea><small>No incluyas contraseñas ni identificaciones.</small></label>
        <button class="boton" type="submit" [disabled]="ocupado || arco.invalid">Enviar solicitud</button>
      </form>
      <h3>Mis solicitudes</h3>
      <ul class="lista-simple">
        @for (s of solicitudes; track s.id) {
          <li><strong>Folio {{ s.id }}, {{ s.tipo }}</strong> <span class="estado" [attr.data-tono]="s.estado === 'atendida' ? 'verde' : s.estado === 'rechazada' ? 'rojo' : 'ambar'">{{ s.estado }}</span>
            <p>{{ s.detalle }}</p><p class="tenue">{{ s.creado_en | date:'d MMM y' }}. Respuesta: {{ s.respuesta || 'pendiente' }}</p></li>
        } @empty { <li class="tenue">No has hecho solicitudes.</li> }
      </ul>
    </section>
  }

  @if (vista === 'baja') {
    <section class="panel peligro">
      <h2>Darme de baja</h2>
      <p>Tu cuenta se suspenderá y se cerrarán tus sesiones. Se abrirá una solicitud de cancelación para revisar los datos ligados a tus reportes, que no se borran de inmediato.</p>
      <form (ngSubmit)="baja()" #salida="ngForm" class="corto">
        <label class="campo"><span>Tu contraseña actual</span><input name="claveBaja" type="password" [(ngModel)]="claveBaja" required autocomplete="current-password"></label>
        <label class="casilla"><input name="confirmar" type="checkbox" [(ngModel)]="confirmarBaja"> Entiendo que perderé el acceso</label>
        <button class="boton rojo" type="submit" [disabled]="ocupado || !confirmarBaja || salida.invalid">Dar de baja mi cuenta</button>
      </form>
    </section>
  }`
})
export class CuentaComponent implements OnInit {
  secciones = SECCIONES; vista = 'datos';
  perfil: Perfil | null = null; colonia: Colonia | null = null; solicitudes: Solicitud[] = [];
  ocupado = false; version = ''; legalVigente = true; terms = false; privacy = false; enlace: RespuestaEnlace | null = null;
  tipo = 'acceso'; detalle = ''; passwordActual = ''; passwordNueva = ''; passwordConfirmacion = ''; claveBaja = ''; confirmarBaja = false;
  constructor(private http: HttpClient, private sesion: SesionService, private router: Router, private avisos: AvisosService) {}
  ngOnInit(): void {
    this.http.get<Perfil>(API + '/cuenta').subscribe({ next: p => { this.perfil = p; this.sesion.actualizarUsuario({ correoVerificado: p.correoVerificado }); }, error: e => this.fallo(e) });
    this.http.get<{ version: string; vigente: boolean }>(API + '/cuenta/aceptaciones').subscribe({ next: d => { this.version = d.version; this.legalVigente = d.vigente; }, error: () => {} });
    this.cargarSolicitudes();
  }
  get tieneDomicilio() { return this.perfil?.coloniaId != null; }
  private fallo(e: unknown) { this.avisos.error(mensajeError(e)); }
  private fin = () => this.ocupado = false;
  cargarSolicitudes(): void { this.http.get<Solicitud[]>(API + '/cuenta/arco').subscribe({ next: s => this.solicitudes = s, error: () => {} }); }
  reenviar(): void {
    this.ocupado = true;
    this.http.post<RespuestaEnlace>(API + '/auth/reenviar-verificacion', {}).pipe(finalize(this.fin)).subscribe({ next: r => this.enlace = r, error: e => this.fallo(e) });
  }
  guardar(): void {
    if (!this.perfil) return;
    this.ocupado = true;
    const cuerpo = { ...this.perfil, coloniaId: this.colonia?.id ?? this.perfil.coloniaId };
    this.http.put<Perfil>(API + '/cuenta', cuerpo).pipe(finalize(this.fin)).subscribe({
      next: p => { this.perfil = p; this.colonia = null; this.sesion.actualizarUsuario({ name: p.name, email: p.email, correoVerificado: p.correoVerificado, coloniaId: String(p.coloniaId ?? '') }); this.avisos.exito('Datos guardados.'); },
      error: e => this.fallo(e)
    });
  }
  descargar(): void {
    this.http.get(API + '/cuenta/exportar').subscribe({
      next: d => {
        const url = URL.createObjectURL(new Blob([JSON.stringify(d, null, 2)], { type: 'application/json' }));
        const a = document.createElement('a'); a.href = url; a.download = 'mis-datos-odapas.json'; a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      }, error: e => this.fallo(e)
    });
  }
  preferencias(): void {
    this.http.put(API + '/cuenta/preferencias', { opcionales: this.perfil?.opcionales === true }).subscribe({ next: () => this.avisos.exito('Preferencia guardada.'), error: e => this.fallo(e) });
  }
  solicitar(): void {
    this.ocupado = true;
    this.http.post<Solicitud>(API + '/cuenta/arco', { tipo: this.tipo, detalle: this.detalle }).pipe(finalize(this.fin)).subscribe({
      next: s => { this.avisos.exito('Solicitud registrada con folio ' + s.id + '.'); this.detalle = ''; this.cargarSolicitudes(); }, error: e => this.fallo(e)
    });
  }
  cambiarPassword(): void {
    if (this.passwordNueva !== this.passwordConfirmacion) { this.avisos.error('Las contraseñas nuevas no coinciden.'); return; }
    this.ocupado = true;
    this.http.put(API + '/cuenta/password', { passwordActual: this.passwordActual, passwordNueva: this.passwordNueva }).pipe(finalize(this.fin)).subscribe({
      next: () => { this.sesion.cerrarLocal(); void this.router.navigate(['/entrar'], { queryParams: { motivo: 'clave' } }); }, error: e => this.fallo(e)
    });
  }
  baja(): void {
    this.ocupado = true;
    this.http.post<{ folio: number }>(API + '/cuenta/baja', { passwordActual: this.claveBaja, confirmar: this.confirmarBaja }).pipe(finalize(this.fin)).subscribe({
      next: r => { this.sesion.cerrarLocal(); this.avisos.exito('Cuenta dada de baja. Folio de seguimiento: ' + r.folio); void this.router.navigate(['/']); }, error: e => this.fallo(e)
    });
  }
  aceptar(): void {
    this.http.post(API + '/cuenta/aceptaciones', { terms: this.terms, privacy: this.privacy, version: this.version }).subscribe({
      next: () => { this.legalVigente = true; this.sesion.actualizarUsuario({ legalVigente: true }); this.avisos.exito('Aceptación registrada.'); }, error: e => this.fallo(e)
    });
  }
}
