import { Routes } from '@angular/router';
import { requiereRol, requiereSesion, soloSinSesion } from './nucleo/guardias';

/** Mapa de pantallas. Cada pantalla se carga solo cuando se visita (lazy loading). */
export const rutas: Routes = [
  // Públicas
  { path: '', title: 'ODAPAS Report', loadComponent: () => import('./paginas/publico/inicio.component').then(m => m.InicioComponent) },
  { path: 'entrar', title: 'Entrar · ODAPAS Report', canActivate: [soloSinSesion], loadComponent: () => import('./paginas/publico/entrar.component').then(m => m.EntrarComponent) },
  { path: 'registro', title: 'Crear cuenta · ODAPAS Report', canActivate: [soloSinSesion], loadComponent: () => import('./paginas/publico/registro.component').then(m => m.RegistroComponent) },
  { path: 'verificar', title: 'Verificar correo', loadComponent: () => import('./paginas/publico/verificar.component').then(m => m.VerificarComponent) },
  { path: 'recuperar', title: 'Recuperar contraseña', loadComponent: () => import('./paginas/publico/recuperar.component').then(m => m.RecuperarComponent) },
  { path: 'restablecer', title: 'Nueva contraseña', loadComponent: () => import('./paginas/publico/restablecer.component').then(m => m.RestablecerComponent) },
  { path: 'activar', title: 'Activar cuenta de trabajador', loadComponent: () => import('./paginas/publico/activar.component').then(m => m.ActivarComponent) },
  { path: 'legal/:tipo', title: 'Documentos legales', loadComponent: () => import('./paginas/publico/legal.component').then(m => m.LegalComponent) },

  // Cualquier usuario con sesión
  { path: 'avisos', title: 'Avisos', canActivate: [requiereSesion], loadComponent: () => import('./paginas/ciudadano/avisos.component').then(m => m.AvisosComponent) },
  { path: 'cuenta', title: 'Mi cuenta', canActivate: [requiereSesion], loadComponent: () => import('./paginas/ciudadano/cuenta.component').then(m => m.CuentaComponent) },

  // Ciudadano
  { path: 'mis-reportes', title: 'Mis reportes', canActivate: [requiereRol('ciudadano')], loadComponent: () => import('./paginas/ciudadano/mis-reportes.component').then(m => m.MisReportesComponent) },
  { path: 'nuevo-reporte', title: 'Nuevo reporte', canActivate: [requiereRol('ciudadano')], loadComponent: () => import('./paginas/ciudadano/nuevo-reporte.component').then(m => m.NuevoReporteComponent) },
  { path: 'reporte/:folio', title: 'Seguimiento del reporte', canActivate: [requiereSesion], loadComponent: () => import('./paginas/ciudadano/detalle-reporte.component').then(m => m.DetalleReporteComponent) },

  // Trabajador
  { path: 'trabajo', title: 'Mi trabajo', canActivate: [requiereRol('trabajador')], loadComponent: () => import('./paginas/trabajador/panel-trabajo.component').then(m => m.PanelTrabajoComponent) },
  { path: 'trabajo/:folio', title: 'Orden de trabajo', canActivate: [requiereRol('trabajador')], loadComponent: () => import('./paginas/trabajador/orden-trabajo.component').then(m => m.OrdenTrabajoComponent) },

  // Administrador
  { path: 'admin', title: 'Panel', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/panel-admin.component').then(m => m.PanelAdminComponent) },
  { path: 'admin/reportes', title: 'Reportes', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/reportes-admin.component').then(m => m.ReportesAdminComponent) },
  { path: 'admin/reportes/:folio', title: 'Atender reporte', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/reporte-admin.component').then(m => m.ReporteAdminComponent) },
  { path: 'admin/ordenes', title: 'Órdenes de trabajo', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/ordenes-admin.component').then(m => m.OrdenesAdminComponent) },
  { path: 'admin/personal', title: 'Personal', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/personal-admin.component').then(m => m.PersonalAdminComponent) },
  { path: 'admin/zonas', title: 'Zonas y colonias', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/zonas-admin.component').then(m => m.ZonasAdminComponent) },
  { path: 'admin/arco', title: 'Solicitudes de privacidad', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/arco-admin.component').then(m => m.ArcoAdminComponent) },
  { path: 'admin/bitacora', title: 'Bitácora', canActivate: [requiereRol('administrador')], loadComponent: () => import('./paginas/admin/bitacora-admin.component').then(m => m.BitacoraAdminComponent) },

  // Rutas de la versión anterior
  { path: 'dashboard', redirectTo: 'mis-reportes' },
  { path: '**', redirectTo: '' }
];
