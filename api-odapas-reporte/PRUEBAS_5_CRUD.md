Actualizacion ARCO: los cinco CRUD ahora requieren administrador. Completa adminEmail y adminPassword en Variables y ejecuta primero 00 Iniciar sesion administrador. Consulta PRIVACIDAD_ARCO.md.

# Pruebas de los cinco CRUD

Reinicia Spring Boot y en Postman importa `ODAPAS_5_CRUD.postman_collection.json`.
La variable urlBase ya apunta a http://localhost:8080.

Las carpetas contienen Crear (POST), Listar (GET), Obtener (GET), Actualizar (PUT) y Eliminar (DELETE). En Personas y Municipios, Eliminar esta en la carpeta Limpieza final.
Ejecuta en ese orden. Crear guarda automaticamente el id para las peticiones siguientes.
POST debe devolver 201; GET y PUT 200; DELETE 204 sin cuerpo.

Para probar Colonias: crea un Municipio primero y conserva ese registro hasta eliminar la colonia.
Para probar Contactos: crea una Persona primero y conserva ese registro hasta eliminar el contacto.
Si ya eliminaste el padre al terminar su carpeta, vuelve a ejecutar su peticion Crear.
Puedes usar Runner con toda la coleccion en su orden original: primero crea los padres, prueba las relaciones y al final ejecuta Limpieza final.

Los campos *_id se envian como numeros, no como texto.
PUT requiere todos los campos del recurso, igual que POST.
No envies id en el cuerpo; la base lo genera.
404 significa registro inexistente; 400 datos invalidos; 409 duplicado o relacion que impide eliminar.
Las peticiones eliminan solamente el registro de prueba cuyo id guardaron.

Archivos: los cinco ControladorPersonas, ControladorMunicipios, ControladorColonias,
ControladorTiposReporte y ControladorContactos estan en src/main/java/mx/odapas/reporte/controlador.
CrudCatalogo comparte las operaciones HTTP; RepositorioCatalogos ejecuta SQL parametrizado.
Se conservan los controladores originales de zonas y reportes.
Uso escolar local: estos endpoints aun no tienen autenticacion ni permisos de administrador.



