package mx.odapas.reporte.privacidad;
import java.util.*;
public final class DocumentosLegales {
 public static final String VERSION="2026-10-21";
 public static final String PRIVACIDAD="""
AVISO DE PRIVACIDAD — ODAPAS Report
Versión 2026-10-21. Proyecto escolar independiente; no es un servicio oficial de ODAPAS.

Responsable del proyecto: Jose Manuel Santiago Osorio.
Contacto para privacidad y solicitudes ARCO: admin@odapas.demo.
Domicilio del responsable: pendiente de configurar antes de un uso público con datos reales.

Datos tratados: nombre, correo, teléfono, domicilio y colonia; reportes, fotografías y ubicación del incidente cuando se proporcionen; historial de solicitudes ARCO, aceptación de documentos y sesiones. Las contraseñas se guardan mediante una derivación criptográfica, nunca en texto plano. No deben adjuntarse identificaciones oficiales, datos sensibles ni datos de terceros en fotografías o solicitudes.

Finalidades necesarias: crear la cuenta, identificar a su titular, gestionar reportes y su atención, mantener la seguridad y atender solicitudes de privacidad. Los administradores autorizados acceden a lo necesario para esas tareas.
Finalidad opcional: comunicaciones informativas adicionales. Está desactivada por defecto; rechazarla o retirarla no impide utilizar la cuenta. Esta versión no envía correo promocional.

Derechos: desde Mi cuenta puede consultar y descargar sus datos, rectificar su perfil y presentar solicitudes de acceso, rectificación, cancelación u oposición. La sesión y, para cambios sensibles o baja, la contraseña actual verifican la identidad. Para aclaraciones o seguimiento después de la baja escriba al correo del responsable indicando el folio; se verificará su identidad antes de divulgar datos. El administrador registra una respuesta motivada y el usuario consulta el estado mientras su cuenta esté activa.

Baja y cancelación: darse de baja suspende inmediatamente la cuenta y revoca sus sesiones. No equivale a borrar automáticamente todos los datos. Una solicitud de cancelación se revisa para determinar bloqueo, supresión y conservación justificada. Al aprobarse, este prototipo anonimiza el perfil y desvincula su domicilio actual; los reportes, direcciones históricas, fotografías y expedientes asociados pueden conservar información identificable y requieren revisión adicional antes de afirmar una supresión completa. La resolución debe describir cualquier conservación, su motivo y seguimiento. No se establece un plazo legal universal en este prototipo.

No se venden datos ni se habilitan transferencias comerciales. El mapa carga recursos de OpenStreetMap y fuentes de imágenes configuradas por la aplicación; esos proveedores reciben datos técnicos de conexión como la dirección IP. No publique domicilios particulares ni fotografías identificables en reportes de prueba. Las copias de seguridad y archivos externos deben incluirse en la revisión de cancelación.

Almacenamiento del navegador: se usa sessionStorage para la sesión, sin cookies publicitarias. Puede cerrar sesión para borrar el token del navegador; la baja invalida también las sesiones del servidor. Una sesión caduca a las ocho horas.
Cambios: la versión del aviso se muestra aquí y queda registrada al crear la cuenta. Los cambios que requieran nueva aceptación se muestran al volver a utilizar el sistema.

Este aviso describe un prototipo educativo. Antes de usarlo como servicio real deben completarse domicilio, política de conservación y procedimientos aplicables al responsable; no acredita por sí solo cumplimiento legal integral.
""";
 public static final String TERMINOS="""
TÉRMINOS Y CONDICIONES — ODAPAS Report
Versión 2026-10-21.

Servicio: proyecto escolar para registrar y consultar incidencias de agua. No es una aplicación oficial de ODAPAS, no garantiza tiempos de reparación y no sustituye canales de emergencia.
Responsable y contacto: Jose Manuel Santiago Osorio, admin@odapas.demo.

Cuenta: el registro público crea únicamente cuentas ciudadanas. No concede permisos de trabajador o administrador. Debe proporcionar datos correctos, proteger su contraseña y no compartir su sesión. La aplicación no ha implementado recuperación de contraseña por correo; contacte al responsable, quien deberá verificar su identidad.
Uso permitido: reportes veraces y relacionados con el servicio de agua. No se permiten suplantaciones, contenido ilícito, spam, fotografías de terceros sin autorización ni publicación de datos personales innecesarios.
Contenido: aporte únicamente información y fotografías que tenga derecho a proporcionar; su uso se limita a gestionar la incidencia conforme al aviso de privacidad.
Funcionamiento: la información puede contener errores y el servicio escolar puede interrumpirse. El mapa no acredita verificaciones oficiales. Las limitaciones del servicio no suprimen derechos legalmente reconocidos.
Privacidad: consulte el aviso separado. La autorización de comunicaciones opcionales es independiente de la aceptación de estos términos y puede retirarse.
Baja: puede suspender su cuenta desde Mi cuenta confirmando su contraseña. Se revocarán las sesiones y se generará una solicitud de cancelación con folio. La revisión de datos asociados continúa por el responsable.
Cambios y contacto: se identifica cada versión aceptada. Puede consultar los documentos sin iniciar sesión y enviar aclaraciones al contacto indicado.
""";
 public static Map<String,Object> documentos() {
  return Map.of("version",VERSION,"privacidad",PRIVACIDAD,"terminos",TERMINOS,
   "responsable","Jose Manuel Santiago Osorio","correo","admin@odapas.demo");
 }
}

