"""Prueba de punta a punta de ODAPAS Report contra una API en ejecución.
Uso: python flujo_completo.py [http://127.0.0.1:8080]
Necesita: la API con ODAPAS_MOSTRAR_ENLACES=true y un administrador (ADMIN_EMAIL / ADMIN_PASSWORD).
"""
import os, sys, time, random, requests
from urllib.parse import urlparse, parse_qs

API = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080") + "/api"
FOTO = os.path.join(os.path.dirname(__file__), "foto.jpg")
VERSION = None
ok = 0

def revisar(cond, texto):
    global ok
    if not cond:
        print("  FALLÓ:", texto); sys.exit(1)
    ok += 1; print("  ✔", texto)

def pedir(metodo, ruta, token=None, esperado=200, **kw):
    h = kw.pop("headers", {})
    if token: h["Authorization"] = "Bearer " + token
    r = requests.request(metodo, API + ruta, headers=h, timeout=30, **kw)
    if r.status_code != esperado:
        print(f"  FALLÓ {metodo} {ruta}: {r.status_code} (esperado {esperado}) {r.text[:300]}"); sys.exit(1)
    return r.json() if r.content and "json" in r.headers.get("Content-Type", "") else r

def token_de(enlace):
    return parse_qs(urlparse(enlace["enlaceDemo"]).query)["token"][0]

sufijo = f"{int(time.time())}{random.randint(100,999)}"
print("1. Público")
VERSION = pedir("GET", "/publico/documentos")["version"]
colonias = pedir("GET", "/publico/colonias", params={"q": "Centro"})
revisar(len(colonias) > 0, "buscador de colonias responde")
colonia = colonias[0]
tipos = pedir("GET", "/publico/tipos-reporte")
revisar(any(t["codigo"] == "fuga" for t in tipos), "catálogo de tipos de reporte")
pedir("GET", "/publico/mapa"); pedir("GET", "/publico/resumen")
revisar("folio" not in str(pedir("GET", "/publico/mapa")[:1]), "el mapa público no expone folios")

print("2. Seguridad básica")
pedir("GET", "/reportes", esperado=401)
pedir("GET", "/admin/estadisticas", esperado=401)
r = requests.get(API + "/publico/resumen")
revisar(r.headers.get("X-Content-Type-Options") == "nosniff", "encabezados de seguridad")

print("3. Registro y verificación del ciudadano")
correo = f"ciudadano.{sufijo}@example.test"
clave = "ClaveSegura2026"
reg = pedir("POST", "/auth/registro", json={"name": "Ana Prueba", "email": correo, "password": clave,
      "telefono": "5512345678", "coloniaId": colonia["id"], "calle": "Hidalgo", "numeroExterior": "12",
      "codigoPostal": "55000", "terms": True, "privacy": True, "version": VERSION}, esperado=201)
tc = reg["token"]
revisar(reg["user"]["correoVerificado"] is False, "la cuenta nace sin correo verificado")
datos_rep = {"tipo": "fuga", "descripcion": "Fuga fuerte en la banqueta frente a la tienda", "usarDomicilio": True,
             "latitud": 19.6, "longitud": -99.0}
pedir("POST", "/reportes", tc, esperado=403, json=datos_rep)
revisar(True, "sin correo verificado no puede reportar (403)")
pedir("POST", "/auth/verificar", json={"token": token_de(reg["verificacion"])})
pedir("POST", "/auth/verificar", json={"token": token_de(reg["verificacion"])}, esperado=410)
revisar(True, "el enlace de verificación es de un solo uso")

print("4. Crear reporte con foto")
rep = pedir("POST", "/reportes", tc, esperado=201, json=datos_rep)
folio = rep["folio"]
revisar(folio.startswith("R-"), f"folio generado por el servidor: {folio}")
dup = pedir("POST", "/reportes", tc, esperado=201, json=datos_rep)
revisar(dup["folio"] == folio, "doble clic no duplica el reporte")
with open(FOTO, "rb") as f:
    fotos = pedir("POST", f"/reportes/{folio}/fotos", tc, files=[("fotos", ("foto.jpg", f, "image/jpeg"))])
revisar(len(fotos["fotos"]) == 1, "foto adjunta")
falsa = pedir("POST", f"/reportes/{folio}/fotos", tc, esperado=400, files=[("fotos", ("x.jpg", b"<script>", "image/jpeg"))])
revisar(True, "rechaza archivos que no son imagen")
url_foto = fotos["fotos"][0]["url"].replace("/api", "")
revisar(pedir("GET", url_foto, tc).status_code == 200, "el dueño ve su foto")
pedir("GET", url_foto, esperado=401)
mis = pedir("GET", "/reportes", tc)
revisar(len(mis) == 1, "el ciudadano solo ve sus reportes")

print("5. Administrador")
adm = pedir("POST", "/auth/login", json={"email": os.environ["ADMIN_EMAIL"], "password": os.environ["ADMIN_PASSWORD"]})
ta = adm["token"]
revisar(adm["user"]["admin"] is True, "inicio de sesión de administrador")
pedir("GET", "/admin/estadisticas", tc, esperado=403)
revisar(True, "un ciudadano no entra al panel (403)")
est = pedir("GET", "/admin/estadisticas", ta)
revisar("reportes" in est, "estadísticas del panel")
lista = pedir("GET", "/admin/reportes", ta, params={"q": folio})
revisar(lista["total"] == 1, "búsqueda de reportes por folio")
det = pedir("GET", f"/reportes/{folio}/detalle", ta)
zona = det.get("zona") or (det.get("zonas") or [{}])[0]
pedir("PUT", f"/admin/reportes/{folio}/revision", ta, json={"estado": "validado", "observaciones": "Confirmado por teléfono"})
revisar(True, "reporte validado")

print("6. Alta y activación de trabajador")
zonas = pedir("GET", "/admin/zonas", ta)
zona_rep = pedir("GET", f"/admin/reportes/{folio}/elegibles", ta)
zona_id = zona_rep["zona"]["id"]
alta = pedir("POST", "/admin/trabajadores", ta, esperado=201, json={"nombre": "Luis Fontanero", "email": f"trab.{sufijo}@example.test",
       "telefono": "5598765432", "numeroEmpleado": f"T-{sufijo}", "zonaId": zona_id})
tt_inv = token_de(alta)
pedir("POST", "/auth/activar", json={"token": tt_inv, "password": "Fontanero2026", "terms": False, "privacy": True, "version": VERSION}, esperado=400)
act = pedir("POST", "/auth/activar", json={"token": tt_inv, "password": "Fontanero2026", "terms": True, "privacy": True, "version": VERSION})
tt = act["token"]
revisar("trabajador" in act["user"]["roles"], "cuenta de trabajador activada")
pedir("GET", "/admin/estadisticas", tt, esperado=403)
pedir("GET", "/trabajador/resumen", tc, esperado=403)
revisar(True, "un ciudadano no entra al panel del trabajador")

print("7. Orden de trabajo")
eleg = pedir("GET", f"/admin/reportes/{folio}/elegibles", ta)
yo = next(t for t in eleg["trabajadores"] if t["id"] == alta["id"])
orden = pedir("POST", "/admin/ordenes", ta, esperado=201, json={"reporteFolio": folio, "prioridad": "alta",
        "responsableId": yo["id"], "colaboradores": [], "observaciones": "Llevar llave stilson"})
of = orden["folio"]
revisar(of.startswith("OT-"), f"orden creada: {of}")
pedir("POST", "/admin/ordenes", ta, esperado=409, json={"reporteFolio": folio, "prioridad": "alta", "responsableId": yo["id"]})
revisar(True, "no se permiten dos órdenes activas para el mismo reporte")

print("8. Trabajo de campo")
mias = pedir("GET", "/trabajador/ordenes", tt)
revisar(any(o["folio"] == of for o in mias), "el trabajador ve su orden")
pedir("GET", f"/trabajador/ordenes/{of}", tt)
pedir("POST", f"/trabajador/ordenes/{of}/recibir", tt)
pedir("POST", f"/trabajador/ordenes/{of}/terminar", tt, esperado=409, json={"trabajoRealizado": "Nada aún", "resultado": "resuelto"})
pedir("POST", f"/trabajador/ordenes/{of}/iniciar", tt, json={"diagnostico": "Tubo de PVC fracturado de 1/2 pulgada"})
with open(FOTO, "rb") as f:
    pedir("POST", f"/trabajador/ordenes/{of}/fotos", tt, params={"fase": "antes"}, files=[("fotos", ("a.jpg", f, "image/jpeg"))])
pedir("POST", f"/trabajador/ordenes/{of}/terminar", tt, esperado=400, json={"trabajoRealizado": "Cambio de tramo", "resultado": "resuelto"})
revisar(True, "no se termina sin foto 'después' ni justificación")
with open(FOTO, "rb") as f:
    pedir("POST", f"/trabajador/ordenes/{of}/fotos", tt, params={"fase": "despues"}, files=[("fotos", ("d.jpg", f, "image/jpeg"))])
pedir("POST", f"/trabajador/ordenes/{of}/terminar", tt, json={"trabajoRealizado": "Se cambió el tramo dañado y se probó presión", "resultado": "resuelto"})
revisar(True, "reparación registrada")

print("9. Cierre y seguimiento del ciudadano")
pedir("PUT", f"/admin/ordenes/{of}/estado", ta, json={"accion": "cerrar", "observaciones": "Verificado en sitio"})
final = pedir("GET", f"/reportes/{folio}/detalle", tc)
revisar(final["estado"] == "cerrada", f"el ciudadano ve su reporte atendido y cerrado (estado={final['estado']})")
avisos = pedir("GET", "/notificaciones", tc)
n = pedir("GET", "/notificaciones/contador", tc)["noLeidas"]
revisar(len(avisos) >= 2 and n >= 2, f"el ciudadano recibió avisos ({n} sin leer)")
pedir("PUT", "/notificaciones/leer-todas", tc)
revisar(pedir("GET", "/notificaciones/contador", tc)["noLeidas"] == 0, "marcar avisos como leídos")
bit = pedir("GET", "/admin/bitacora", ta)
revisar(len(bit) > 0, "bitácora de auditoría")

print("10. Recuperar contraseña y límite de intentos")
rec = pedir("POST", "/auth/recuperar", json={"email": correo})
pedir("POST", "/auth/restablecer", json={"token": token_de(rec), "password": "NuevaClave2026x"})
pedir("GET", "/reportes", tc, esperado=401)
revisar(True, "al restablecer la contraseña se cierran las sesiones anteriores")
for _ in range(5):
    pedir("POST", "/auth/login", json={"email": correo, "password": "mala"}, esperado=401)
pedir("POST", "/auth/login", json={"email": correo, "password": "NuevaClave2026x"}, esperado=429)
revisar(True, "bloqueo temporal tras 5 intentos fallidos (429)")

print(f"\nTODO CORRECTO: {ok} comprobaciones superadas.")
