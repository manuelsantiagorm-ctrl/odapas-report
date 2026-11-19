import { HttpErrorResponse } from '@angular/common/http';

/** Todas las peticiones van a /api; en desarrollo el proxy de Angular las envía a Spring Boot (puerto 8080). */
export const API = '/api';

/** Mensaje legible a partir de un error HTTP de la API. */
export function mensajeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'No hay conexión con el servidor. Revisa que la API esté encendida.';
    const texto = e.error?.error || e.error?.message;
    if (typeof texto === 'string' && texto.trim()) return texto;
    if (e.status === 403) return 'No tienes permiso para esta acción.';
    if (e.status === 404) return 'No encontramos lo que buscas.';
    if (e.status === 413) return 'El archivo es demasiado grande (máximo 5 MB por foto).';
    if (e.status === 429) return 'Demasiados intentos. Espera unos minutos.';
    return 'Algo falló en el servidor (' + e.status + '). Intenta de nuevo.';
  }
  return 'Ocurrió un error inesperado.';
}
