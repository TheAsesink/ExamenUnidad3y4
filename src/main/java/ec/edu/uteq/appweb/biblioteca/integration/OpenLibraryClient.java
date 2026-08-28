package ec.edu.uteq.appweb.biblioteca.integration;

import ec.edu.uteq.appweb.biblioteca.config.CacheConfig;
import ec.edu.uteq.appweb.biblioteca.exception.ServicioExternoException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenLibraryClient {

    private final RestClient restClient;

    public OpenLibraryClient(RestClient restClientExterno) {
        this.restClient = restClientExterno;
    }

    @Cacheable(value = CacheConfig.CACHE_OPENLIBRARY, key = "#isbn", unless = "#result == null")
    public OpenLibraryResponse consultarPorIsbn(String isbn) {
        try {
            return restClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .onStatus(estado -> estado.is4xxClientError() && estado.value() != 404,
                            (peticion, respuesta) -> {
                                throw new ServicioExternoException("Error 4xx de Open Library: " + peticion.getURI());
                            })
                    .onStatus(estado -> estado.is5xxServerError(),
                            (peticion, respuesta) -> {
                                throw new ServicioExternoException("Error 5xx de Open Library: " + peticion.getURI());
                            })
                    .body(OpenLibraryResponse.class);
        } catch (ServicioExternoException e) {
            throw e;
        } catch (Exception e) {
            throw new ServicioExternoException("Timeout o fallo de red con Open Library", e);
        }
    }
}
