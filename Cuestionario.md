# Cuestionario — Parte A del examen de la Unidad IV

> **Cómo se llena este archivo.** Responda **dentro de este mismo archivo**, debajo de cada pregunta, en el bloque marcado como `**Respuesta:**`. No borre ni reescriba los enunciados: el evaluador compara pregunta por pregunta. No añada ni quite secciones.
>
> **Este archivo se versiona en el repositorio.** Debe existir en la raíz, llamarse exactamente `Cuestionario.md`, y sus respuestas deben llegar por *commits* sucesivos hechos cuando el docente lo indique. Un archivo que aparece completo en un único *commit* al final de la sesión no cumple el protocolo y se trata según el criterio de piso 4 del examen.
>
> Se valora la precisión técnica y la justificación, **no la extensión**. Una respuesta correcta de seis líneas vale más que una página imprecisa. Cuando la pregunta pida referirse al proyecto base, hágalo con nombres concretos de clases o de *endpoints*.

---

## Datos del estudiante

| Campo | Valor |
|---|---|
| Apellidos y nombres | Guerrero Kevin |
| Número de carnet | |
| Correo institucional | 0999595561kevin@gmail.com |
| Fecha | 2026-08-28 |
| URL del repositorio | https://github.com/TheAsesink/ExamenUnidad3y4.git |

---

## A1. Restricciones de REST aplicadas a un caso concreto — 8 puntos

**a) Enuncie las seis restricciones del estilo arquitectónico REST según Fielding. (3 puntos)**

**Respuesta:**

1. **Cliente-Servidor:** Separación de responsabilidades entre cliente (interfaz) y servidor (datos), permitiendo independencia y escalabilidad.
2. **Stateless (Sin estado):** Cada petición contiene toda la información necesaria para procesarla; el servidor no guarda estado de sesión entre peticiones.
3. **Cache (Almacenamiento en caché):** Las respuestas deben indicar si son cacheables o no, permitiendo al cliente reutilizar datos y reducir latencia.
4. **Interface Uniforme:** Interfaz uniforme para simplificar la arquitectura: identificación de recursos (URIs), manipulación a través de representaciones, auto-descripción de mensajes y HATEOAS.
5. **Layered System (Sistema en capas):** El cliente no distingue si se conecta directamente al servidor o a capas intermedias (proxies, gateways), facilitando escalabilidad y seguridad.
6. **Code on Demand (opcional):** El servidor puede extender temporalmente la funcionalidad del cliente enviando código ejecutable (JavaScript, Java applets).



**b) El proyecto base expone `GET /api/v1/autores` y guarda el estado de la sesión del usuario solo en el JWT que el cliente envía en cada petición. Explique qué restricción concreta se está cumpliendo con esa decisión y qué consecuencia práctica tiene para escalar el sistema a varios servidores detrás de un balanceador. (3 puntos)**

**Respuesta:**

Se cumple la restricción de **Stateless (sin estado)**. El servidor no almacena información de la sesión del usuario; toda la información de autenticación (usuario, rol, expiración) viaja en el JWT que el cliente envía en cada petición como cabecera `Authorization: Bearer <token>`.

**Consecuencia práctica para escalar:** Al no haber estado en el servidor, cualquier instancia detrás del balanceador puede procesar cualquier petición. Si se añaden 3 servidores detrás de un load balancer, el cliente puede enviar peticiones a cualquiera de ellas sin importar en cuál hizo login inicialmente. Esto elimina la necesidad de sesiones compartidas (como sticky sessions o una caché de sesiones distribuida) y permite escalar horizontalmente de forma sencilla.



**c) De las seis restricciones, indique cuál es opcional y dé un ejemplo real de una API que la use. (2 puntos)**

**Respuesta:**

La restricción opcional es **Code on Demand (Código bajo demanda)**. Es la única marcada como opcional en el estilo arquitectónico REST de Fielding: el servidor puede enviar código ejecutable al cliente para extender temporalmente sus funcionalidades.

**Ejemplo real:** La API de **Google Maps JavaScript API** envía código JavaScript al navegador del cliente, el cual se ejecuta para renderizar mapas interactivos, marcadores y rutas. El cliente (navegador) descarga y ejecuta el código proporcionado por el servidor (Google) para ampliar sus capacidades más allá de lo que el navegador podía hacer por defecto.



---

## A2. Anatomía y ciclo de vida de un JWT — 8 puntos

**a) Un JWT tiene tres partes separadas por puntos. Nómbrelas en orden e indique qué contiene cada una. (3 puntos)**

**Respuesta:**

Un JWT tiene la estructura `Header.Payload.Signature`:

1. **Header (Cabecera):** Contiene el algoritmo de firma utilizado (ej: `{"alg":"HS256","typ":"JWT"}`) y opcionalmente el tipo de token. Indica al receptor cómo verificar la firma.

2. **Payload (Carga):** Contiene los claims (afirmaciones) con los datos del usuario: `sub` (sujeto/username), `rol` (rol del usuario), `jti` (ID único del token), `iat` (fecha de emisión) y `exp` (fecha de expiración). **No está cifrado**, solo codificado en Base64, por lo que cualquiera puede leerlo.

3. **Signature (Firma):** Se calcula hasheando el header, el payload y un secreto conocido solo por el servidor: `HMACSHA256(base64(header) + "." + base64(payload), secreto)`. Garantiza la integridad del token: si alguien modifica el payload, la firma no coincidirá.



**b) Un compañero afirma: «como el JWT va firmado, puedo guardar en el *payload* la contraseña del usuario sin riesgo». Explique por qué está equivocado, precisando la diferencia entre firmar y cifrar. (2 puntos)**

**Respuesta:**

El compañero está equivocado porque **firmar no es lo mismo que cifrar**:

- **Firmar** garantiza la **integridad y autenticidad**: verifica que el token fue emitido por el servidor y que nadie lo modificó. Pero el payload **NO está cifrado**: cualquiera que intercepte o descargue el token puede decodificar el Base64 y leer todos los claims, incluyendo la contraseña si estuviera ahí.

- **Cifrar** garantiza la **confidencialidad**: transforma el contenido en texto ilegible sin la clave de descifrado.

El JWT solo firma (HMAC-SHA256), no cifra. Por lo tanto, guardar la contraseña en el payload la expondría públicamente. Además, el payload se transmite en cada petición HTTP, aumentando aún más el riesgo de interceptación.



**c) El JWT es *stateless* por diseño, lo que genera un problema conocido: no se puede invalidar un token antes de que expire. Describa dos estrategias distintas para revocarlo y señale la desventaja de cada una. (3 puntos)**

**Respuesta:**

**Estrategia 1: Lista negra en Redis (Token Blacklist)**
Se almacena el `jti` (ID único) del token revocado en Redis con un TTL igual al tiempo restante de expiración. El `JwtAuthenticationFilter` verifica contra Redis antes de procesar cada petición.

*Desventaja:* Introduce una consulta a Redis en cada petición, lo cual añade latencia (~1-2ms) y rompe parcialmente la naturaleza stateless, ya que ahora se requiere un estado compartido (la lista negra) para validar tokens.

**Estrategia 2: Tokens de corta duración + Refresh Token**
Se emiten access tokens con expiración muy corta (5-15 minutos) y refresh tokens de larga duración. Para "revocar", simplemente no se renueva el refresh token cuando el usuario hace logout. Si el access token es robado, expira rápidamente.

*Desventaja:* No revoca inmediatamente un access token ya emitido; si el token fue robado, sigue siendo válido hasta que expire. También增加了 la complejidad del sistema al manejar dos tipos de tokens y su ciclo de vida.



---

## A3. SOAP frente a REST — 8 puntos

**a) Complete la tabla comparativa con seis criterios entre SOAP y REST. (5 puntos)**

**Respuesta:**

| Criterio | SOAP | REST |
|---|---|---|
| Formato del mensaje | XML exclusivo (con envoltorio SOAPEnvelope) | Múltiple: JSON, XML, HTML, texto plano (JSON es el más común) |
| Contrato de descripción | WSDL (Web Services Description Language), contrato estricto y tipado | OpenAPI/Swagger, maisons ou URF. Contrato más flexible y legible |
| Sobrecarga de serialización | Alta: envoltorio XML + headers SOAPAction + encoding namespace | Baja: JSON sin metadatos adicionales, payload más compacto |
| Tipado | Fuerte tipado en compile-time gracias al WSDL y XSD | Débil o dinámico; JSON no impone tipos en el contrato |
| Facilidad de consumo desde un cliente móvil | Baja: pesado, requiere librerías SOAP complejas (kSOAP) | Alta: JSON es nativo en JavaScript/móviles, HTTP simple |
| Manejo de errores | Faults SOAP estructurados con Detail y FaultCode | Códigos HTTP estándar (4xx, 5xx) + ProblemDetail (RFC 9457) |

**b) El Servicio de Rentas Internas del Ecuador expone la autorización de comprobantes electrónicos mediante servicios SOAP. Explique dos razones técnicas por las que una institución de ese tipo mantiene SOAP en lugar de migrar a REST. (3 puntos)**

**Respuesta:**

1. **Estabilidad y compatibilidad retroactiva garantizada por WSDL:** El SRI define contratos WSDL que miles de sistemas contables, facturadores electrónicos y PASIVOS conectados durante más de una década dependen. Un WSDL es un contrato estricto que garantiza que un cliente compilado hace 8 años seguirá funcionando. REST con OpenAPI es más flexible pero no ofrece la misma garantía de compatibilidad a largo plazo; un cambio de estructura JSON podría romper clientes existentes sin previo aviso.

2. **Seguridad y no-repudio nativo de WS-Security y XML Digital Signatures:** El SRI requiere que los comprobantes electrónicos tengan firma digitalXML (XMLDSig) y sellado de tiempo para que tengan validez legal. SOAP tiene soporte nativo para WS-Security, XML Encryption y XML Digital Signatures integrados en el estándar. En REST, esta funcionalidad requiere implementaciones externas (JWT + JWS) que añaden complejidad y no tienen la misma madurez legal en Ecuador.



---

## A4. Cache-aside sobre un servicio externo — 8 puntos

> El proyecto base define en `CacheConfig` dos espacios de caché: `libros` con TTL de 2 minutos y `openlibrary` con TTL de 24 horas.

**a) Describa el patrón *cache-aside* en sus cuatro pasos, desde que llega la petición hasta que se responde. (3 puntos)**

**Respuesta:**

1. **Verificar caché:** Cuando llega la petición, primero se busca la clave en Redis (caché). Si el dato está cacheado y no ha expirado, se retorna directamente sin tocar la base de datos ni el servicio externo.

2. **Cache miss:** Si el dato NO está en caché (cache miss), se consulta la fuente original (base de datos, API externa, etc.).

3. **Almacenar en caché:** Una vez obtenido el dato de la fuente original, se almacena en Redis con un TTL (Time To Live) apropiado antes de retornarlo al cliente.

4. **Retornar respuesta:** Se devuelve el dato al cliente. Las siguientes peticiones para la misma clave encontrarán el dato en caché (cache hit) hasta que el TTL expire, momento en que el ciclo comienza de nuevo.



**b) Justifique técnicamente por qué el TTL de `openlibrary` es doce veces mayor que el de `libros`, y qué criterio general debe guiar la elección de un TTL. (3 puntos)**

**Respuesta:**

El TTL de `openlibrary` (24 horas) es 12x mayor que el de `libros` (2 minutos) debido a la **volatilidad del dato**:

- **`libros` (TTL = 2 min):** Es el catálogo propio del sistema. Los libros se crean, actualizan o desactivan frecuentemente (nuevos ingresos, cambios de ejemplares, etc.). Un TTL corto garantiza que los usuarios vean datos relativamente frescos.

- **`openlibrary` (TTL = 24 h):** Son metadatos bibliográficos externos (título, portada, páginas de un ISBN). Estos datos son prácticamente inmutables: un libro publicado en 1999 no cambiará su título ni su portada mañana. Al ser datos estáticos, se puede cachear por mucho más tiempo sin riesgo de servir información obsoleta.

**Criterio general para elegir un TTL:** El TTL debe ser inversamente proporcional a la frecuencia de cambio del dato. Datos que cambian poco (catálogos externos, datos maestros) merecen TTLs largos. Datos que cambian frecuentemente (inventario, precios, disponibilidad) merecen TTLs cortos.



**c) Explique por qué nunca debe almacenarse en caché la respuesta de un fallo del servicio externo, y describa qué le ocurriría al sistema si se hiciera. (2 puntos)**

**Respuesta:**

Nunca se debe cachear un fallo porque **un error es un evento temporal, no un dato estable**. Si cacheamos una respuesta de error (ej: timeout, 500, 502), el sistema seguirá retornando ese error durante todo el TTL aunque el servicio externo ya se haya recuperado.

**Qué pasaría si se cacheara un fallo:** Si Open Library está caído por 5 minutos y se cachea el error con TTL de 24 horas, el sistema seguirá respondiendo "servicio externo no disponible" durante las próximas 24 horas, incluso cuando Open Library ya funciona correctamente. Esto crea una falsa impresión de que el servicio está roto cuando en realidad solo fue una intermitencia. La solución es no cachear fallos (usando `unless` o `condition` en `@Cacheable`) para que en cada petición se reintente la consulta al servicio externo.



---

## A5. Diagnóstico de códigos de estado y contrato de errores — 8 puntos

> Todos los errores del proyecto base salen en formato *Problem Details* conforme a la RFC 9457, que obsoleta a la RFC 7807.

Para cada escenario indique el código HTTP correcto y explique en una línea por qué. **Cada fila vale 1 punto** (0,5 por el código y 0,5 por la justificación); el literal g) vale 2 puntos.

| # | Escenario | Código | Justificación (una línea) |
|---|---|---|---|
| a | `GET /api/v1/libros/999999` y ese identificador no existe | 404 Not Found | Recurso no encontrado en la base de datos, lanzado por RecursoNoEncontradoException |
| b | `POST /api/v1/libros` sin cabecera `Authorization` | 401 Unauthorized | No se proporcionó token de autenticación, el filtro JWT no encuentra Bearer token |
| c | Usuario autenticado con rol `LECTOR` envía `POST /api/v1/libros` | 403 Forbidden | Autenticado pero sin permisos; @PreAuthorize("hasRole('ADMIN')") lo rechaza |
| d | `POST /api/v1/libros` con el campo `titulo` vacío | 400 Bad Request | @NotBlank en LibroRequest valida que titulo no esté vacío, GlobalExceptionHandler retorna 400 |
| e | Prestar un libro a un socio que ya tiene tres préstamos activos | 409 Conflict | ReglaNegocioException: el socio supera el límite de 3 préstamos activos |
| f | La API de Open Library no responde dentro del *timeout* configurado | 502 Bad Gateway | Timeout del RestClient, lanza ServicioExternoException → GlobalExceptionHandler retorna 502 |

**g) Explique por qué devolver `200 OK` con un cuerpo `{"success": false}` es un error de diseño, y qué restricción de REST se incumple al hacerlo. (2 puntos)**

**Respuesta:**

Devolver `200 OK` con `{"success": false}` es un error de diseño porque **viola la semántica de los códigos de estado HTTP**. El código 200 indica explícitamente "la petición fue procesada exitosamente", pero el cuerpo dice que falló. Esto genera ambigüedad: el cliente no puede confiar en el código HTTP para tomar decisiones (reintentar, mostrar error, redirigir).

La restricción de REST que se incumple es la **Interface Uniforme**: los códigos de estado HTTP son parte de la interfaz uniforme y deben comunicar el resultado real de la operación. Usar 200 para errores rompe el contrato semántico que todos los clientes HTTP esperan, dificultando el manejo de errores genérico (middlewares de retry, interceptores, etc.). El proyecto base lo hace correctamente: éxitos van en ApiResponse con 200/201, errores van en ProblemDetail con el código HTTP adecuado.



---

## Declaración de honestidad académica

Marque con una `x` y complete:

- [x] Declaro que estas respuestas son de mi autoría, redactadas durante la sesión de examen, sin asistencia de inteligencia artificial ni comunicación con terceros.

Firma (nombre completo): Guerrero Kevin
