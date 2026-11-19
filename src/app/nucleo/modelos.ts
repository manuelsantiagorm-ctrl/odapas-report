/** Tipos de datos que devuelve la API de ODAPAS Report. */
export type Rol = 'ciudadano' | 'trabajador' | 'administrador';

export interface Usuario {
  id: string; name: string; email: string; coloniaId: string;
  admin: boolean; legalVigente: boolean; roles: Rol[]; correoVerificado: boolean;
}
export interface Sesion { token: string; user: Usuario; }

/** Respuesta de operaciones que emiten un enlace (verificar, recuperar, activar). */
export interface RespuestaEnlace { mensaje: string; enlaceDemo?: string; aviso?: string; }

export interface Colonia { id: number; nombre: string; municipio: string; zona_id?: number | null; zona?: string | null; }
export interface TipoReporte { id: number; codigo: string; nombre: string; }

export interface ReporteResumen {
  folio: string; tipo: string; descripcion: string; colonia: string; calle: string; zona: string | null;
  latitud: number | null; longitud: number | null; estado: string; prioridad: string | null;
  fechaCreacion?: string; fecha_creacion?: string;
}
export interface PuntoMapa { tipo: string; colonia: string; estado: string; zona: string | null; latitud: number; longitud: number; fecha_creacion: string; folio?: string; }

export interface PasoHistorial { secuencia: number; estado: string; observaciones: string | null; fecha: string; actor?: string; }
export interface Foto { id?: number; url: string; nombre?: string; fase?: string; }
export interface Reparacion {
  id: number; diagnostico: string; trabajo_realizado: string | null; resultado: string | null;
  iniciado_en: string; terminado_en: string | null; fotos: Foto[];
}
export interface MiembroEquipo { id?: number; nombre?: string; numero_empleado?: string; responsable_principal: boolean; recibido_en: string | null; finalizado_en: string | null; }
export interface OrdenDeReporte {
  id: number; folio: string; prioridad: string; fecha_programada: string | null; creado_en: string; finalizado_en: string | null;
  zona: string; estado: string; historial: PasoHistorial[]; equipo: MiembroEquipo[]; reparaciones: Reparacion[];
}
export interface DetalleReporte {
  id: number; folio: string; tipo: string; tipo_nombre: string; descripcion: string; creado_en: string;
  colonia: string; municipio: string; calle: string; numero_exterior: string; numero_interior: string | null;
  codigo_postal: string; referencias: string | null; latitud: number | null; longitud: number | null;
  estado: string; zona: string | null; prioridad: string | null;
  historial: PasoHistorial[]; fotos: Foto[]; ordenes: OrdenDeReporte[];
  ciudadano?: { nombre: string; telefono: string };
}
export interface Aviso { id: number; tipo: string; creado_en: string; leido_en: string | null; reporte_folio: string | null; orden_folio: string | null; titulo: string; }

export interface OrdenTrabajador {
  folio: string; reporte_folio: string; tipo: string; tipo_nombre: string; descripcion: string; colonia: string; calle: string;
  latitud: number | null; longitud: number | null; prioridad: string; fecha_programada: string | null; creado_en: string;
  finalizado_en: string | null; estado: string; responsable_principal: boolean; recibido_en: string | null; asignacion_finalizada: string | null;
}
export interface Trabajador {
  id: number; nombre: string; correo: string; telefono: string; numero_empleado: string; estado: string;
  registrado_en: string; zona_id: number | null; zona: string | null; zona_nombre: string | null; ordenes_activas: number;
}
export interface Zona { id: number; codigo: string; nombre: string; descripcion: string | null; activa: boolean; trabajadores_disponibles: number; colonias: { id: number; nombre: string }[]; }
export interface OrdenAdmin {
  folio: string; reporte_folio: string; zona: string; prioridad: string; fecha_programada: string | null; estado: string;
  creado_en: string; finalizado_en: string | null; tipo: string; colonia: string; responsable: string | null;
}
export interface Pagina<T> { tamano: number; pagina: number; total: number; datos: T[]; }
