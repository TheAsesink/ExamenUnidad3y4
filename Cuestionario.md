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
| Formato del mensaje | | |
| Contrato de descripción | | |
| Sobrecarga de serialización | | |
| Tipado | | |
| Facilidad de consumo desde un cliente móvil | | |
| Manejo de errores | | |

**b) El Servicio de Rentas Internas del Ecuador expone la autorización de comprobantes electrónicos mediante servicios SOAP. Explique dos razones técnicas por las que una institución de ese tipo mantiene SOAP en lugar de migrar a REST. (3 puntos)**

**Respuesta:**



---

## A4. Cache-aside sobre un servicio externo — 8 puntos

> El proyecto base define en `CacheConfig` dos espacios de caché: `libros` con TTL de 2 minutos y `openlibrary` con TTL de 24 horas.

**a) Describa el patrón *cache-aside* en sus cuatro pasos, desde que llega la petición hasta que se responde. (3 puntos)**

**Respuesta:**



**b) Justifique técnicamente por qué el TTL de `openlibrary` es doce veces mayor que el de `libros`, y qué criterio general debe guiar la elección de un TTL. (3 puntos)**

**Respuesta:**



**c) Explique por qué nunca debe almacenarse en caché la respuesta de un fallo del servicio externo, y describa qué le ocurriría al sistema si se hiciera. (2 puntos)**

**Respuesta:**



---

## A5. Diagnóstico de códigos de estado y contrato de errores — 8 puntos

> Todos los errores del proyecto base salen en formato *Problem Details* conforme a la RFC 9457, que obsoleta a la RFC 7807.

Para cada escenario indique el código HTTP correcto y explique en una línea por qué. **Cada fila vale 1 punto** (0,5 por el código y 0,5 por la justificación); el literal g) vale 2 puntos.

| # | Escenario | Código | Justificación (una línea) |
|---|---|---|---|
| a | `GET /api/v1/libros/999999` y ese identificador no existe | | |
| b | `POST /api/v1/libros` sin cabecera `Authorization` | | |
| c | Usuario autenticado con rol `LECTOR` envía `POST /api/v1/libros` | | |
| d | `POST /api/v1/libros` con el campo `titulo` vacío | | |
| e | Prestar un libro a un socio que ya tiene tres préstamos activos | | |
| f | La API de Open Library no responde dentro del *timeout* configurado | | |

**g) Explique por qué devolver `200 OK` con un cuerpo `{"success": false}` es un error de diseño, y qué restricción de REST se incumple al hacerlo. (2 puntos)**

**Respuesta:**



---

## Declaración de honestidad académica

Marque con una `x` y complete:

- [ ] Declaro que estas respuestas son de mi autoría, redactadas durante la sesión de examen, sin asistencia de inteligencia artificial ni comunicación con terceros.

Firma (nombre completo): ______________________________
