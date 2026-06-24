# Presupuesto API

Base del backend para el sistema presupuestal. Usa Java 21, Spring Boot, SQL Server y JWT.

## Configuracion

Crear el archivo `.env` a partir de `.env.example` y colocar los valores del entorno. `bootRun` carga este archivo automaticamente.

```bash
cp .env.example .env
```

`JWT_SECRET` debe tener al menos 32 caracteres y administrarse como secreto fuera del repositorio.

## Ejecucion

Desde una terminal ubicada en la carpeta `pre-api`, seleccionar Java 21, verificarlo y arrancar la aplicacion:

```bash
cd /Users/gabo/github/presupuesto/pre-api

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

java -version
./gradlew bootRun
```

`java -version` debe indicar una version 21. Para detener la aplicacion, presionar `Ctrl + C`.

La API queda disponible en `http://localhost:3012`.

En produccion se recomienda generar `build/libs/app.jar`, ejecutarlo con Java 21 y limitarlo a loopback mediante `SERVER_ADDRESS=127.0.0.1`. El archivo `../ecosystem.config.cjs` contiene la definicion de PM2 para el puerto `3029`.

### Compatibilidad temporal con SQL Server antiguo

Durante `bootRun`, las conexiones `jdbc:sqlserver:` habilitan TLS 1.0 de forma local mediante `config/java-legacy-tls.security`. Esta excepcion no modifica Java globalmente y no se activa al cambiar `DB_URL` a PostgreSQL. Debe eliminarse cuando termine la migracion porque TLS 1.0 es obsoleto.

La ejecucion del JAR no utiliza la configuracion especial de `bootRun`. En produccion, PM2 agrega explicitamente `-Djava.security.properties=config/java-legacy-tls.security` y `DB_URL` debe conservar `sslProtocol=TLSv1` mientras exista el servidor antiguo.

## Autenticacion

```http
POST /api/auth/login
Content-Type: application/json

{
  "usuario": "usuario_sia",
  "password": "contrasena"
}
```

El login consulta `SIA.dbo.Usuarios` y devuelve un JWT. Los endpoints futuros bajo `/api/**` requieren `Authorization: Bearer <token>`, excepto `/api/auth/**`.

Al iniciar sesion, el API combina `Perfiles`, `Roles`, `Permisos` y `Modulos`. El JWT incluye los modulos autorizados, su alcance `LOCAL` o `GLOBAL` y las acciones habilitadas con valor `S`. Para el modulo de partidas presupuestales obtiene las claves mediante la relacion `Roles -> PartidasRol -> Partidas`.

La columna heredada `Password` es `varchar(50)` y se compara con su valor actual para mantener compatibilidad. Debe planearse una migracion a hashes adaptativos, como BCrypt o Argon2.

## Partidas presupuestales

```http
GET /api/dashboard/presupuesto
Authorization: Bearer <token>
```

Devuelve el disponible (`Clave.Monto`), comprometido y ejercido totalizados. El presupuesto asignado corresponde a la suma de estos tres importes. Tambien incluye el numero de partidas con informacion y todas las partidas autorizadas para la grafica del dashboard. Los valores respetan roles, partidas autorizadas y alcance `LOCAL/GLOBAL`.

La misma respuesta incluye conteos agrupados por estatus. `KardexSolicitudes` y `KardexSolicitudesCaja` se cuentan por las partidas autorizadas para el rol y respetan el alcance `LOCAL/GLOBAL`; para perfiles locales tambien se filtran por la `UA` del usuario. `KardexSolicitudesCapituloCuatro` y `KardexViaticos` se mantienen por usuario autenticado porque sus indicadores actuales no dependen de las partidas mostradas en el dashboard. La comparacion del campo `Usuario` ignora mayusculas, minusculas y espacios laterales.

```http
GET /api/partidas-presupuestales?page=1&pageSize=25&search=&sortBy=cog&sortOrder=asc
Authorization: Bearer <token>
```

La respuesta principal contiene una fila agrupada por `COG` y los importes son la suma de las UEG permitidas por el alcance del usuario. UEG y unidad ejecutora se entregan exclusivamente en el endpoint de detalle.

```http
GET /api/partidas-presupuestales/{cog}/detalle
Authorization: Bearer <token>
```

El detalle se agrupa por UEG y se utiliza para desplegar la tabla interna de una partida.

```http
GET /api/partidas-presupuestales/exportar?search=
Authorization: Bearer <token>
```

La exportacion requiere `exportar = S` y genera un archivo `.xlsx` exclusivamente con el detalle agrupado por COG y UEG. Incluye todos los registros autorizados que coinciden con la busqueda, independientemente de la pagina visible.

El endpoint requiere `ejecutar = S` y `consultar = S` para el modulo `1`. Siempre limita `Clave.COG` a las claves relacionadas con el rol en `dbo.PartidasRol` y utiliza `Partidas.nombre` como descripcion.

- Un perfil `GLOBAL` consulta todas las UEG existentes en `dbo.UA`.
- Un perfil `LOCAL` interpreta `Usuarios.UA` como `UA.ID` y consulta solamente la UEG relacionada.
- La paginacion usa `ROW_NUMBER()` para mantener compatibilidad con SQL Server 2008.

## Documentacion y verificacion

OpenAPI esta en `http://localhost:3012/api/man` y utiliza Basic Auth con `DOCS_USERNAME` y `DOCS_PASSWORD`.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew test
./gradlew bootJar
```
