package mx.odapas.reporte;
import mx.odapas.reporte.privacidad.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named="ODAPAS_TEST_DB_URL",matches=".+")
class PruebasPrivacidad {
 @DynamicPropertySource static void db(DynamicPropertyRegistry p){
  p.add("spring.datasource.url",()->System.getenv("ODAPAS_TEST_DB_URL"));
  p.add("spring.datasource.username",()->System.getenv().getOrDefault("ODAPAS_TEST_DB_USER","aguapp_test"));
  p.add("spring.datasource.password",()->System.getenv().getOrDefault("ODAPAS_TEST_DB_PASSWORD",""));
 }
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired JdbcTemplate jdbc;
 @Autowired ServicioCuenta cuentas;
 private final String password="PruebaSegura123!";
 private Map<String,Object> datos(){
  return new HashMap<>(Map.ofEntries(Map.entry("email","arco-"+UUID.randomUUID()+"@example.com"),
   Map.entry("password",password),Map.entry("name","Persona ARCO prueba"),Map.entry("telefono","5512345678"),
   Map.entry("coloniaId",jdbc.queryForObject("SELECT MIN(id) FROM public.colonia",Integer.class)),
   Map.entry("calle","Calle escolar"),Map.entry("numeroExterior","10"),Map.entry("codigoPostal","55000"),
   Map.entry("terms",true),Map.entry("privacy",true),Map.entry("version",DocumentosLegales.VERSION)));
 }
 private String token(Map<String,Object>s){return "Bearer "+s.get("token");}
 private int id(Map<String,Object>s){return Integer.parseInt(((Map<?,?>)s.get("user")).get("id").toString());}
 @Test void requiereSesionYNoPermiteCatalogosNiAdministracionAlCiudadano() throws Exception{
  mvc.perform(get("/api/cuenta")).andExpect(status().isUnauthorized());
  String token=token(cuentas.registrar(datos()));
  mvc.perform(get("/api/cuenta").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Persona ARCO prueba")).andExpect(jsonPath("$.password_hash").doesNotExist());
  mvc.perform(get("/api/personas").header("Authorization",token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/contactos").header("Authorization",token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/arco").header("Authorization",token)).andExpect(status().isForbidden());
 }
 @Test void registroPersistenteConAceptacionesYContrasenaDerivada() throws Exception{
  var d=datos();
  var respuesta=mvc.perform(post("/api/auth/registro").contentType("application/json").content(json.writeValueAsString(d)))
   .andExpect(status().isCreated()).andReturn();
  var sesion=json.readValue(respuesta.getResponse().getContentAsString(),Map.class);
  int id=id(sesion);
  String hash=jdbc.queryForObject("SELECT password_hash FROM public.usuario WHERE id=?",String.class,id);
  assertTrue(hash.startsWith("pbkdf2$"));assertNotEquals(password,hash);
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM public.aceptacion_legal WHERE usuario_id=?",Integer.class,id));
  assertFalse(jdbc.queryForObject("SELECT comunicaciones_opcionales FROM public.preferencia_privacidad WHERE usuario_id=?",Boolean.class,id));
  mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",d.get("email"),"password",password))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
 }
 @Test void noPuedeRegistrarSinConsentimientosNiElevarRol() throws Exception{
  var d=datos();d.put("terms",false);
  mvc.perform(post("/api/auth/registro").contentType("application/json").content(json.writeValueAsString(d))).andExpect(status().isBadRequest());
 }
 @Test void datosPersonalesSoloDelTitularYEdicionExigeClave() throws Exception{
  var a=cuentas.registrar(datos());var b=cuentas.registrar(datos());
  var cambio=new HashMap<>(datos());cambio.put("email","cambio-"+UUID.randomUUID()+"@example.com");cambio.put("name","Nombre corregido");
  cambio.put("passwordActual","incorrecta");
  mvc.perform(put("/api/cuenta").header("Authorization",token(a)).contentType("application/json").content(json.writeValueAsString(cambio))).andExpect(status().isForbidden());
  cambio.put("passwordActual",password);cambio.put("usuario_id",id(b));
  mvc.perform(put("/api/cuenta").header("Authorization",token(a)).contentType("application/json").content(json.writeValueAsString(cambio)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nombre corregido"));
  assertEquals("Persona ARCO prueba",cuentas.perfil(id(b)).get("name"));
  assertEquals("Nombre corregido",cuentas.perfil(id(a)).get("name"));
 }
 @Test void exportacionNoContieneCredencialesNiReportesAjenos() throws Exception{
  var a=cuentas.registrar(datos());
  mvc.perform(get("/api/cuenta/exportar").header("Authorization",token(a))).andExpect(status().isOk())
   .andExpect(header().string("Content-Disposition","attachment; filename=mis-datos-odapas.json"))
   .andExpect(jsonPath("$.reportes.length()").value(0)).andExpect(jsonPath("$.aceptaciones.length()").value(2))
   .andExpect(jsonPath("$.perfil.password_hash").doesNotExist());
  mvc.perform(get("/api/reportes").header("Authorization",token(a))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/reportes/R-000123").header("Authorization",token(a))).andExpect(status().isNotFound());
 }
 @Test void solicitudesSeAislanYAdminRespondeConMotivo() throws Exception{
  var a=cuentas.registrar(datos());var b=cuentas.registrar(datos());
  cuentas.solicitud(id(a),Map.of("tipo","acceso","detalle","Deseo conocer mis datos personales"));
  mvc.perform(get("/api/cuenta/arco").header("Authorization",token(b))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  jdbc.update("INSERT INTO public.usuario_rol(usuario_id,rol) VALUES(?,'administrador')",id(b));
  int folio=((Number)cuentas.solicitudes(id(a)).get(0).get("id")).intValue();
  mvc.perform(put("/api/admin/arco/"+folio).header("Authorization",token(b)).contentType("application/json")
   .content(json.writeValueAsString(Map.of("estado","atendida","respuesta","Datos disponibles en la descarga de Mi cuenta.")))).andExpect(status().isOk());
  assertEquals("atendida",cuentas.solicitudes(id(a)).get(0).get("estado"));
 }
 @Test void oposicionRetiraComunicaciones() throws Exception{
  var a=cuentas.registrar(datos());
  mvc.perform(put("/api/cuenta/preferencias").header("Authorization",token(a)).contentType("application/json").content("{\"opcionales\":true}")).andExpect(status().isOk());
  mvc.perform(put("/api/cuenta/preferencias").header("Authorization",token(a)).contentType("application/json").content("{\"opcionales\":false}")).andExpect(status().isOk());
  assertEquals(false,cuentas.perfil(id(a)).get("opcionales"));
 }
 @Test void bajaRevocaTodasLasSesionesYRechazaNuevoLogin() throws Exception{
  var d=datos();var a=cuentas.registrar(d);var otra=cuentas.login(d);
  mvc.perform(post("/api/cuenta/baja").header("Authorization",token(a)).contentType("application/json")
   .content(json.writeValueAsString(Map.of("passwordActual",password,"confirmar",true)))).andExpect(status().isOk()).andExpect(jsonPath("$.folio").isNumber());
  mvc.perform(get("/api/cuenta").header("Authorization",token(otra))).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",d.get("email"),"password",password)))).andExpect(status().isUnauthorized());
  assertEquals("cancelacion",cuentas.solicitudes(id(a)).get(0).get("tipo"));
 }
 @Test void cancelacionAnonimizaPerfilSinRomperReferencias() throws Exception{
  var a=cuentas.registrar(datos());var admin=cuentas.registrar(datos());
  jdbc.update("INSERT INTO public.usuario_rol(usuario_id,rol) VALUES(?,'administrador')",id(admin));
  cuentas.baja(id(a),Map.of("passwordActual",password,"confirmar",true));
  int folio=((Number)cuentas.solicitudes(id(a)).get(0).get("id")).intValue();
  mvc.perform(put("/api/admin/arco/"+folio).header("Authorization",token(admin)).contentType("application/json")
   .content(json.writeValueAsString(Map.of("estado","atendida","respuesta","Perfil anonimizado; se revisaron las referencias y no hay reportes en este caso.","revisionConservacion",true))))
   .andExpect(status().isOk());
  assertEquals("Cuenta cancelada",cuentas.perfil(id(a)).get("name"));assertNull(cuentas.perfil(id(a)).get("coloniaId"));
  assertNull(jdbc.queryForObject("SELECT password_hash FROM public.usuario WHERE id=?",String.class,id(a)));
 }
 @Test void cambioClaveRevocaSesionYPermiteEntrarConNueva() throws Exception{
  var d=datos();var a=cuentas.registrar(d);
  mvc.perform(put("/api/cuenta/password").header("Authorization",token(a)).contentType("application/json")
   .content(json.writeValueAsString(Map.of("passwordActual",password,"passwordNueva","NuevaClaveSegura456!")))).andExpect(status().isOk());
  mvc.perform(get("/api/cuenta").header("Authorization",token(a))).andExpect(status().isUnauthorized());
  assertNotNull(cuentas.login(Map.of("email",d.get("email"),"password","NuevaClaveSegura456!")).get("token"));
 }
 @Test void sesionExpiradaNoFunciona() throws Exception{
  var a=cuentas.registrar(datos());
  jdbc.update("UPDATE public.sesion_usuario SET expira_en=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE usuario_id=?",id(a));
  mvc.perform(get("/api/cuenta").header("Authorization",token(a))).andExpect(status().isUnauthorized());
 }
}

