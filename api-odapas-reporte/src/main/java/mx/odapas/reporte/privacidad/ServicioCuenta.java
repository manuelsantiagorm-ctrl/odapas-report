package mx.odapas.reporte.privacidad;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class ServicioCuenta {
 private final JdbcTemplate jdbc;
 private final mx.odapas.reporte.servicio.ServicioEnlaces enlaces;
 private static final String HASH_SIMULADO=Claves.guardar("contraseña-simulada-para-tiempo-constante1");
 public ServicioCuenta(JdbcTemplate jdbc, mx.odapas.reporte.servicio.ServicioEnlaces enlaces) { this.jdbc=jdbc; this.enlaces=enlaces; }
 /** Usuario autenticado y sus roles vigentes. */
 public record Identidad(int id,boolean admin,boolean trabajador,boolean ciudadano) {
  public Identidad(int id,boolean admin){this(id,admin,false,!admin);}
 }
 public static String texto(Map<String,Object> d,String campo,int min,int max) {
  Object valor=d.get(campo);
  if(!(valor instanceof String s) || s.trim().length()<min || s.length()>max)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisa "+campo+" ("+min+" a "+max+" caracteres)");
  return ((String)valor).trim();
 }
 public static String clave(Map<String,Object>d,String campo) {
  Object v=d.get(campo);
  if(!(v instanceof String s) || s.length()<10 || s.length()>128)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contraseña debe tener entre 10 y 128 caracteres");
  String c=(String)v;
  if(!c.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*") || !c.matches(".*[0-9].*"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contraseña debe incluir letras y números");
  return c;
 }
 public static String correo(Map<String,Object>d) {
  String c=texto(d,"email",5,150).toLowerCase(Locale.ROOT);
  if(!c.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Correo inválido");
  return c;
 }
 private int entero(Map<String,Object>d,String campo) {
  Object n=d.get(campo);
  if(!(n instanceof Number v) || v.doubleValue()!=v.intValue() || v.intValue()<1)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revisa "+campo);
  return ((Number)n).intValue();
 }
 private int direccion(Map<String,Object>d) {
  int colonia=entero(d,"coloniaId");
  if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.colonia WHERE id=?)",Boolean.class,colonia)))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Colonia inexistente");
  String cp=texto(d,"codigoPostal",5,5);
  if(!cp.matches("[0-9]{5}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Código postal inválido");
  return jdbc.queryForObject("INSERT INTO public.direccion(colonia_id,calle,numero_exterior,codigo_postal) VALUES(?,?,?,?) RETURNING id",
   Integer.class,colonia,texto(d,"calle",1,150),texto(d,"numeroExterior",1,20),cp);
 }
 public void aceptaciones(int id,Map<String,Object>d) {
  if(!Boolean.TRUE.equals(d.get("terms")) || !Boolean.TRUE.equals(d.get("privacy")) ||
    !DocumentosLegales.VERSION.equals(d.get("version")))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Lee y acepta las versiones actuales de términos y aviso de privacidad");
  for(String tipo:List.of("terminos","privacidad"))
   jdbc.update("INSERT INTO public.aceptacion_legal(usuario_id,documento,version) VALUES(?,?,?) ON CONFLICT DO NOTHING",id,tipo,DocumentosLegales.VERSION);
 }
 @Transactional
 public Map<String,Object> registrar(Map<String,Object>d) {
  String email=correo(d), password=clave(d,"password");
  int persona=jdbc.queryForObject("INSERT INTO public.persona(nombre_completo) VALUES(?) RETURNING id",Integer.class,texto(d,"name",2,150));
  jdbc.update("INSERT INTO public.contacto(persona_id,correo,telefono) VALUES(?,?,?)",persona,email,texto(d,"telefono",5,20));
  int id=jdbc.queryForObject("INSERT INTO public.usuario(persona_id,password_hash,estado) VALUES(?,?,'activo') RETURNING id",
   Integer.class,persona,Claves.guardar(password));
  jdbc.update("INSERT INTO public.usuario_rol(usuario_id,rol) VALUES(?,'ciudadano')",id);
  int direccion=direccion(d);
  jdbc.update("INSERT INTO public.ciudadano(usuario_id,direccion_actual_id) VALUES(?,?)",id,direccion);
  aceptaciones(id,d);
  jdbc.update("INSERT INTO public.preferencia_privacidad(usuario_id,comunicaciones_opcionales) VALUES(?,?)",id,Boolean.TRUE.equals(d.get("opcionales")));
  evento(id,id,"registro_y_aceptacion");
  var respuesta=new LinkedHashMap<String,Object>(sesion(id));
  String url=enlaces.emitir(id,"verificar_correo",null,mx.odapas.reporte.servicio.ServicioEnlaces.VIGENCIA_VERIFICAR);
  respuesta.put("verificacion",enlaces.respuesta("Te enviamos un enlace para verificar tu correo. Debes verificarlo antes de enviar reportes.",url));
  return respuesta;
 }
 @Transactional
 public Map<String,Object> login(Map<String,Object>d) {
  var filas=jdbc.queryForList("SELECT u.id,u.password_hash,u.estado FROM public.usuario u JOIN public.contacto c ON c.persona_id=u.persona_id WHERE c.correo=? FOR UPDATE OF u",correo(d));
  String password=d.get("password") instanceof String s ? s : "";
  // Si el correo no existe se calcula igual una derivación, para no revelar cuentas por el tiempo de respuesta.
  if(filas.isEmpty()) Claves.coincide(password,HASH_SIMULADO);
  if(filas.isEmpty() || !"activo".equals(filas.get(0).get("estado")) ||
     !Claves.coincide(password,(String)filas.get(0).get("password_hash")))
   throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Correo o contraseña incorrectos, o cuenta dada de baja");
  return sesion(((Number)filas.get(0).get("id")).intValue());
 }
 /** Crea una sesión nueva (token aleatorio; en la base solo se guarda su huella). */
 public Map<String,Object> sesion(int id) {
  String token=Claves.token();
  jdbc.update("INSERT INTO public.sesion_usuario(usuario_id,token_hash) VALUES(?,?)",id,Claves.huella(token));
  var perfil=perfil(id);
  var u=new LinkedHashMap<String,Object>();
  u.put("id",String.valueOf(id));u.put("name",perfil.get("name"));u.put("email",perfil.get("email"));
  u.put("coloniaId",perfil.get("coloniaId")==null?"":perfil.get("coloniaId").toString());u.put("admin",esAdmin(id));
  u.put("legalVigente",estadoLegal(id).get("vigente"));
  u.put("roles",roles(id));
  u.put("correoVerificado",perfil.get("correoVerificado"));
  return Map.of("token",token,"user",u);
 }
 public Identidad autenticar(String authorization) {
  if(authorization==null || !authorization.startsWith("Bearer ") || authorization.length()>200)
   throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Inicia sesión");
  var ids=jdbc.queryForList("SELECT u.id FROM public.sesion_usuario s JOIN public.usuario u ON u.id=s.usuario_id WHERE s.token_hash=? AND s.revocada_en IS NULL AND s.expira_en>CURRENT_TIMESTAMP AND u.estado='activo'",Integer.class,Claves.huella(authorization.substring(7)));
  if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión vencida o cuenta dada de baja");
  var roles=roles(ids.get(0));
  return new Identidad(ids.get(0),roles.contains("administrador"),roles.contains("trabajador"),roles.contains("ciudadano"));
 }
 public List<String> roles(int id) {
  return jdbc.queryForList("SELECT rol::text FROM public.usuario_rol WHERE usuario_id=? ORDER BY rol",String.class,id);
 }
 public boolean esAdmin(int id) {
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT public.odapas_es_admin(?)",Boolean.class,id));
 }
 @Transactional
 public void aceptar(int id,Map<String,Object>d) { bloquear(id);aceptaciones(id,d);evento(id,id,"aceptacion_version_legal"); }
 public Map<String,Object> estadoLegal(int id) {
  Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM public.aceptacion_legal WHERE usuario_id=? AND version=?",Integer.class,id,DocumentosLegales.VERSION);
  return Map.of("version",DocumentosLegales.VERSION,"vigente",n!=null && n==2);
 }
 public Map<String,Object> perfil(int id) {
  return jdbc.queryForMap("""
   SELECT u.id,p.nombre_completo AS name,c.correo AS email,c.telefono,
    d.colonia_id AS "coloniaId",d.calle,d.numero_exterior AS "numeroExterior",d.codigo_postal AS "codigoPostal",
    u.estado, c.correo_verificado_en IS NOT NULL AS "correoVerificado",
    COALESCE(pp.comunicaciones_opcionales,FALSE) AS opcionales
   FROM public.usuario u JOIN public.persona p ON p.id=u.persona_id
   JOIN public.contacto c ON c.persona_id=p.id LEFT JOIN public.ciudadano ci ON ci.usuario_id=u.id
   LEFT JOIN public.direccion d ON d.id=ci.direccion_actual_id LEFT JOIN public.preferencia_privacidad pp ON pp.usuario_id=u.id
   WHERE u.id=?
   """,id);
 }
 private Map<String,Object> bloquear(int id) {
  var f=jdbc.queryForList("SELECT * FROM public.usuario WHERE id=? AND estado='activo' FOR UPDATE",id);
  if(f.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Cuenta no disponible");
  return f.get(0);
 }
 private void comprobarClave(Map<String,Object> cuenta,Map<String,Object>d) {
  if(!Claves.coincide(d.get("passwordActual") instanceof String s?s:"",(String)cuenta.get("password_hash")))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La contraseña actual no es correcta");
 }
 public void evento(int usuario,int actor,String evento) {
  jdbc.update("INSERT INTO public.evento_privacidad(usuario_id,actor_id,evento) VALUES(?,?,?)",usuario,actor,evento);
 }
 private void limpiarDomicilio(Integer direccion) {
  if(direccion!=null) jdbc.update("DELETE FROM public.direccion d WHERE d.id=? AND NOT EXISTS(SELECT 1 FROM public.ciudadano c WHERE c.direccion_actual_id=d.id) AND NOT EXISTS(SELECT 1 FROM public.reporte r WHERE r.direccion_incidente_id=d.id)",direccion);
 }
 @Transactional
 public Map<String,Object> actualizar(int id,Map<String,Object>d) {
  var cuenta=bloquear(id);comprobarClave(cuenta,d);
  String email=correo(d); int persona=((Number)cuenta.get("persona_id")).intValue();
  jdbc.update("UPDATE public.persona SET nombre_completo=? WHERE id=?",texto(d,"name",2,150),persona);
  jdbc.update("UPDATE public.contacto SET correo_verificado_en=CASE WHEN correo=? THEN correo_verificado_en ELSE NULL END,correo=?,telefono=? WHERE persona_id=?",
   email,email,texto(d,"telefono",5,20),persona);
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.ciudadano WHERE usuario_id=?)",Boolean.class,id))) {
   Integer anterior=jdbc.queryForObject("SELECT direccion_actual_id FROM public.ciudadano WHERE usuario_id=?",Integer.class,id);
   int nueva=direccion(d);
   jdbc.update("UPDATE public.ciudadano SET direccion_actual_id=? WHERE usuario_id=?",nueva,id);
   limpiarDomicilio(anterior);
  }
  evento(id,id,"rectificacion_perfil");
  return perfil(id);
 }
 @Transactional
 public void password(int id,Map<String,Object>d) {
  var cuenta=bloquear(id);comprobarClave(cuenta,d);
  jdbc.update("UPDATE public.usuario SET password_hash=? WHERE id=?",Claves.guardar(clave(d,"passwordNueva")),id);
  jdbc.update("UPDATE public.sesion_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE usuario_id=? AND revocada_en IS NULL",id);
  evento(id,id,"cambio_password_y_revocacion");
 }
 public void logout(int id,String token) {
  jdbc.update("UPDATE public.sesion_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE usuario_id=? AND token_hash=?",id,Claves.huella(token.substring(7)));
 }
 @Transactional
 public Map<String,Object> solicitud(int id,Map<String,Object>d) {
  bloquear(id);
  String tipo=texto(d,"tipo",1,20);
  if(!List.of("acceso","rectificacion","cancelacion","oposicion").contains(tipo))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo ARCO inválido");
  return jdbc.queryForMap("INSERT INTO public.solicitud_arco(usuario_id,tipo,detalle) VALUES(?,?,?) RETURNING *",
   id,tipo,texto(d,"detalle",5,2000));
 }
 public List<Map<String,Object>> solicitudes(int id) {
  return jdbc.queryForList("SELECT * FROM public.solicitud_arco WHERE usuario_id=? ORDER BY id DESC",id);
 }
 @Transactional
 public void preferencias(int id,Map<String,Object>d) {
  bloquear(id);
  if(!(d.get("opcionales") instanceof Boolean valor)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica opcionales como booleano");
  jdbc.update("INSERT INTO public.preferencia_privacidad(usuario_id,comunicaciones_opcionales) VALUES(?,?) ON CONFLICT(usuario_id) DO UPDATE SET comunicaciones_opcionales=EXCLUDED.comunicaciones_opcionales,actualizado_en=CURRENT_TIMESTAMP",id,d.get("opcionales"));
  evento(id,id,Boolean.TRUE.equals(d.get("opcionales"))?"autoriza_opcionales":"retira_opcionales");
 }
 @Transactional
 public Map<String,Object> baja(int id,Map<String,Object>d) {
  var cuenta=bloquear(id);comprobarClave(cuenta,d);
  if(!Boolean.TRUE.equals(d.get("confirmar"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirma la baja");
  if(esAdmin(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Un administrador debe solicitar su baja a otro responsable para conservar la atención de solicitudes");
  var abiertas=jdbc.queryForList("SELECT * FROM public.solicitud_arco WHERE usuario_id=? AND tipo='cancelacion' AND estado IN ('pendiente','en_revision') ORDER BY id LIMIT 1",id);
  var solicitud=abiertas.isEmpty()?jdbc.queryForMap("INSERT INTO public.solicitud_arco(usuario_id,tipo,detalle) VALUES(?,'cancelacion',?) RETURNING *",id,"Solicitud de cancelación generada al dar de baja la cuenta."):abiertas.get(0);
  jdbc.update("UPDATE public.usuario SET estado='suspendido' WHERE id=?",id);
  jdbc.update("UPDATE public.preferencia_privacidad SET comunicaciones_opcionales=FALSE,actualizado_en=CURRENT_TIMESTAMP WHERE usuario_id=?",id);
  jdbc.update("UPDATE public.sesion_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE usuario_id=? AND revocada_en IS NULL",id);
  evento(id,id,"baja_cuenta");
  return Map.of("folio",solicitud.get("id"),"mensaje","Cuenta dada de baja y sesiones revocadas. La cancelación de datos queda en revisión. Conserva el folio y contacta a admin@odapas.demo para seguimiento.");
 }
 public Map<String,Object> exportar(int id) {
  var datos=new LinkedHashMap<String,Object>(Map.of("perfil",perfil(id),"solicitudes",solicitudes(id),
   "aceptaciones",jdbc.queryForList("SELECT documento,version,aceptado_en FROM public.aceptacion_legal WHERE usuario_id=? ORDER BY id",id),
   "reportes",jdbc.queryForList("SELECT r.id,r.folio,r.descripcion,r.creado_en,t.codigo AS tipo FROM public.reporte r JOIN public.tipo_reporte t ON t.id=r.tipo_reporte_id WHERE r.ciudadano_id=? ORDER BY r.id",id),
   "fotos",jdbc.queryForList("SELECT f.id,f.reporte_id,f.foto_json::text AS metadatos FROM public.fotografia_reporte f JOIN public.reporte r ON r.id=f.reporte_id WHERE r.ciudadano_id=?",id),
   "eventos",jdbc.queryForList("SELECT evento,creado_en FROM public.evento_privacidad WHERE usuario_id=? ORDER BY id",id),
   "aviso",DocumentosLegales.PRIVACIDAD));
  datos.put("direcciones",jdbc.queryForList("SELECT d.* FROM public.direccion d WHERE d.id IN (SELECT direccion_incidente_id FROM public.reporte WHERE ciudadano_id=? UNION SELECT direccion_actual_id FROM public.ciudadano WHERE usuario_id=?)",id,id));
  datos.put("notificaciones",jdbc.queryForList("SELECT * FROM public.notificacion WHERE destinatario_id=? ORDER BY id",id));
  datos.put("historial_reportes",jdbc.queryForList("SELECT h.* FROM public.historial_reporte h JOIN public.reporte r ON r.id=h.reporte_id WHERE r.ciudadano_id=? ORDER BY h.id",id));
  return datos;
 }
 public List<Map<String,Object>> pendientes() {
  return jdbc.queryForList("SELECT s.*,p.nombre_completo AS solicitante FROM public.solicitud_arco s JOIN public.usuario u ON u.id=s.usuario_id JOIN public.persona p ON p.id=u.persona_id ORDER BY s.id DESC");
 }
 @Transactional
 public Map<String,Object> resolver(int admin,int solicitudId,Map<String,Object>d) {
  if(!esAdmin(admin)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Requiere administrador");
  var filas=jdbc.queryForList("SELECT * FROM public.solicitud_arco WHERE id=? FOR UPDATE",solicitudId);
  if(filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud inexistente");
  var s=filas.get(0);
  if(List.of("atendida","rechazada").contains(s.get("estado")))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya tiene resolución");
  String estado=texto(d,"estado",1,20),respuesta=texto(d,"respuesta",10,2000);
  if(!List.of("en_revision","atendida","rechazada").contains(estado))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado inválido");
  int id=((Number)s.get("usuario_id")).intValue();
  if("atendida".equals(estado) && "cancelacion".equals(s.get("tipo"))) {
   if(!Boolean.TRUE.equals(d.get("revisionConservacion")))
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirma revisión de conservación y explica los datos conservados en la respuesta");
   if(esAdmin(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Primero debe reasignarse el rol de administrador");
   var u=jdbc.queryForMap("SELECT persona_id FROM public.usuario WHERE id=? FOR UPDATE",id);
   jdbc.update("UPDATE public.usuario SET estado='suspendido',password_hash=NULL WHERE id=?",id);
   jdbc.update("UPDATE public.persona SET nombre_completo='Cuenta cancelada' WHERE id=?",u.get("persona_id"));
   jdbc.update("UPDATE public.contacto SET correo=?,telefono='No disponible',correo_verificado_en=NULL WHERE persona_id=?","cancelada-"+id+"-"+UUID.randomUUID()+"@example.invalid",u.get("persona_id"));
   var domicilios=jdbc.queryForList("SELECT direccion_actual_id FROM public.ciudadano WHERE usuario_id=?",Integer.class,id);
   jdbc.update("UPDATE public.ciudadano SET direccion_actual_id=NULL WHERE usuario_id=?",id);
   for(Integer domicilio:domicilios) limpiarDomicilio(domicilio);
   jdbc.update("UPDATE public.sesion_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE usuario_id=?",id);
   jdbc.update("UPDATE public.preferencia_privacidad SET comunicaciones_opcionales=FALSE WHERE usuario_id=?",id);
   evento(id,admin,"cancelacion_perfil_revision_datos_historicos");
  }
  if("atendida".equals(estado) && "oposicion".equals(s.get("tipo"))) {
   jdbc.update("INSERT INTO public.preferencia_privacidad(usuario_id,comunicaciones_opcionales) VALUES(?,FALSE) ON CONFLICT(usuario_id) DO UPDATE SET comunicaciones_opcionales=FALSE",id);
  }
  return jdbc.queryForMap("UPDATE public.solicitud_arco SET estado=?,respuesta=?,respondido_por=?,respondido_en=CASE WHEN ?='en_revision' THEN NULL ELSE CURRENT_TIMESTAMP END WHERE id=? RETURNING *",
   estado,respuesta,admin,estado,solicitudId);
 }
}

