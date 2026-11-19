/** Textos en español para los valores que guarda la base de datos. */
export const ESTADOS: Record<string, { texto: string; tono: string; ayuda: string }> = {
  recibido: { texto: 'Recibido', tono: 'gris', ayuda: 'Tu reporte llegó y espera revisión.' },
  en_revision: { texto: 'En revisión', tono: 'ambar', ayuda: 'Personal de ODAPAS está confirmando el problema.' },
  validado: { texto: 'Validado', tono: 'azul', ayuda: 'El problema se confirmó; falta asignar una cuadrilla.' },
  asignado: { texto: 'Cuadrilla asignada', tono: 'azul', ayuda: 'Ya hay personal asignado para atenderlo.' },
  asignada: { texto: 'Asignada', tono: 'azul', ayuda: 'Orden asignada al personal.' },
  en_proceso: { texto: 'En reparación', tono: 'turquesa', ayuda: 'El personal está trabajando en el lugar.' },
  reparacion_realizada: { texto: 'Reparación hecha', tono: 'turquesa', ayuda: 'El trabajo terminó y está por revisarse.' },
  cerrada: { texto: 'Atendido', tono: 'verde', ayuda: 'El trabajo se revisó y quedó cerrado.' },
  rechazado: { texto: 'No procede', tono: 'rojo', ayuda: 'Después de revisarlo, no corresponde atenderlo.' },
  cancelado: { texto: 'Cancelado', tono: 'rojo', ayuda: 'El reporte se canceló.' },
  cancelada: { texto: 'Cancelada', tono: 'rojo', ayuda: 'La orden se canceló.' }
};
export const TIPOS: Record<string, string> = { fuga: 'Fuga', falta_agua: 'Falta de agua', baja_presion: 'Baja presión' };
export const PRIORIDADES: Record<string, string> = { baja: 'Baja', media: 'Media', alta: 'Alta', urgente: 'Urgente' };
export const RESULTADOS: Record<string, string> = { resuelto: 'Resuelto', parcial: 'Parcial', no_resuelto: 'No resuelto' };

/** Etapas del seguimiento que ve el ciudadano (la "tubería"). */
export const ETAPAS = [
  { clave: 'recibido', texto: 'Recibido' },
  { clave: 'revision', texto: 'Revisado' },
  { clave: 'asignado', texto: 'Cuadrilla' },
  { clave: 'reparacion', texto: 'Reparación' },
  { clave: 'cerrada', texto: 'Atendido' }
];
export function etapaDe(estado: string): number {
  switch (estado) {
    case 'recibido': return 0;
    case 'en_revision': case 'validado': return 1;
    case 'asignado': return 2;
    case 'en_proceso': case 'reparacion_realizada': return 3;
    case 'cerrada': return 4;
    default: return -1; // rechazado o cancelado
  }
}
export const textoEstado = (e: string) => ESTADOS[e]?.texto ?? e;
export const tonoEstado = (e: string) => ESTADOS[e]?.tono ?? 'gris';
export const textoTipo = (t: string) => TIPOS[t] ?? t;
