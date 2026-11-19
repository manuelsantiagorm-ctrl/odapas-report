import { Injectable } from '@angular/core';

export interface Mensaje { id: number; texto: string; tipo: 'exito' | 'error'; }

/** Mensajes breves que aparecen abajo de la pantalla después de una acción. */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  mensajes: Mensaje[] = [];
  private siguiente = 1;
  exito(texto: string) { this.mostrar(texto, 'exito'); }
  error(texto: string) { this.mostrar(texto, 'error'); }
  private mostrar(texto: string, tipo: Mensaje['tipo']) {
    const m = { id: this.siguiente++, texto, tipo };
    this.mensajes = [...this.mensajes.slice(-2), m];
    setTimeout(() => this.cerrar(m.id), tipo === 'error' ? 7000 : 4000);
  }
  cerrar(id: number) { this.mensajes = this.mensajes.filter(m => m.id !== id); }
}
