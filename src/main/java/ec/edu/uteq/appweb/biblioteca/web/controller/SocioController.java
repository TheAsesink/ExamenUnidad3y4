package ec.edu.uteq.appweb.biblioteca.web.controller;

import ec.edu.uteq.appweb.biblioteca.domain.Socio;
import ec.edu.uteq.appweb.biblioteca.service.SocioService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.PageMeta;
import ec.edu.uteq.appweb.biblioteca.web.dto.SocioRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.SocioResponse;
import ec.edu.uteq.appweb.biblioteca.web.mapper.SocioMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/socios")
@Tag(name = "Socios", description = "API REST de socios de la biblioteca")
public class SocioController {

    private final SocioService servicio;
    private final SocioMapper mapper;

    public SocioController(SocioService servicio, SocioMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Listar socios", description = "Listado paginado de socios activos")
    public ApiResponse<List<SocioResponse>> listar(@PageableDefault(size = 20) Pageable paginacion) {
        Page<Socio> pagina = servicio.listarActivos(paginacion);
        List<SocioResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Socios listados", PageMeta.de(pagina));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar socio por ID")
    public ApiResponse<SocioResponse> buscar(@PathVariable Long id) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscarPorId(id)), "Socio encontrado");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Crear socio", description = "Requiere rol ADMIN o BIBLIOTECARIO. Devuelve 201 con Location")
    public ResponseEntity<ApiResponse<SocioResponse>> crear(@Valid @RequestBody SocioRequest solicitud) {
        Socio creado = servicio.crear(solicitud);
        SocioResponse cuerpo = mapper.aRespuesta(creado);
        return ResponseEntity
                .created(URI.create("/api/v1/socios/" + creado.getId()))
                .body(ApiResponse.ok(cuerpo, "Socio creado"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Actualizar socio")
    public ApiResponse<SocioResponse> actualizar(@PathVariable Long id,
                                                 @Valid @RequestBody SocioRequest solicitud) {
        Socio actualizado = servicio.actualizar(id, solicitud);
        return ApiResponse.ok(mapper.aRespuesta(actualizado), "Socio actualizado");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar socio", description = "Borrado logico. Requiere rol ADMIN")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
