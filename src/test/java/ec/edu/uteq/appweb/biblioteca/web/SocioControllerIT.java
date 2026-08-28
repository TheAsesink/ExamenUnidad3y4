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

class SocioControllerIT extends BaseIntegracionTest {

    @Test
    @DisplayName("GET /api/v1/socios responde 200 con envoltorio y metadatos")
    void listarSociosDevuelveEnvoltorio() throws Exception {
        mockMvc.perform(get("/api/v1/socios")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/socios/999999 responde 404 con ProblemDetail")
    void buscarSocioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/socios/999999")
                        .header("Authorization", "Bearer " + getToken("admin", "Admin123!")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login responde 200 con token en login response")
    void loginDevuelveToken() throws Exception {
        String body = """
                {
                    "username": "bibliotecario",
                    "password": "Biblio123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.rol").value("BIBLIOTECARIO"));
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
