package ec.edu.uteq.appweb.biblioteca.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.uteq.appweb.biblioteca.BaseIntegracionTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class LibroControllerIT extends BaseIntegracionTest {

    @Test
    @DisplayName("GET /api/v1/libros responde 200 con envoltorio y metadatos de paginacion")
    void listarLibrosDevuelveEnvoltorio() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").value("Libros listados"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20));
    }

    @Test
    @DisplayName("GET /api/v1/libros/999999 responde 404 con ProblemDetail")
    void buscarLibroInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/libros/999999")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/libros con titulo vacio responde 400 con errors")
    void crearLibroConTituloVacioDevuelve400() throws Exception {
        String body = """
                {
                    "isbn": "978-1234567890",
                    "titulo": "",
                    "anioPublicacion": 2020,
                    "ejemplaresTotales": 5,
                    "autorId": 1,
                    "editorialId": 1,
                    "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/libros sin token responde 401")
    void crearLibroSinTokenDevuelve401() throws Exception {
        String body = """
                {
                    "isbn": "978-1234567890",
                    "titulo": "Test",
                    "anioPublicacion": 2020,
                    "ejemplaresTotales": 5,
                    "autorId": 1,
                    "editorialId": 1,
                    "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/libros con rol LECTOR responde 403")
    void crearLibroConRolLectorDevuelveProhibido() throws Exception {
        String body = """
                {
                    "isbn": "978-1234567890",
                    "titulo": "Test",
                    "anioPublicacion": 2020,
                    "ejemplaresTotales": 5,
                    "autorId": 1,
                    "editorialId": 1,
                    "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("lector", "Lector123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/libros con rol ADMIN y cuerpo valido responde 201 con Location")
    void crearLibroConRolAdminDevuelve201() throws Exception {
        String body = """
                {
                    "isbn": "978-9999999999",
                    "titulo": "Libro de prueba examen",
                    "anioPublicacion": 2023,
                    "ejemplaresTotales": 3,
                    "autorId": 1,
                    "editorialId": 1,
                    "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isbn").value("978-9999999999"));
    }

    @Test
    @DisplayName("GET /api/v1/libros con filtro titulo devuelve resultados filtrados")
    void listarLibrosConFiltroTitulo() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!"))
                        .param("titulo", "Cien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales correctas devuelve 200 y token")
    void loginConCredencialesCorrectas() throws Exception {
        String body = """
                {
                    "username": "admin",
                    "password": "Admin123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales invalidas devuelve 401")
    void loginConCredencialesInvalidas() throws Exception {
        String body = """
                {
                    "username": "admin",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Toda respuesta exitosa trae el envoltorio con las 5 claves")
    void respuestaExitosaTieneEnvoltorioCompleto() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    @DisplayName("GET /api/v1/libros con filtro categoriaId devuelve resultados")
    void listarLibrosConFiltroCategoria() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!"))
                        .param("categoriaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/libros con filtro anioDesde devuelve resultados")
    void listarLibrosConFiltroAnioDesde() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!"))
                        .param("anioDesde", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    private String getToken(String username, String password) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return org.springframework.test.web.servlet.result.JsonPath.parse(responseBody)
                .read("$.data.token", String.class);
    }
}
