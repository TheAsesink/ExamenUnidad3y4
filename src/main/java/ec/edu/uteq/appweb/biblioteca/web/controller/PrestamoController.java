package ec.edu.uteq.appweb.biblioteca.web.controller;

import ec.edu.uteq.appweb.biblioteca.domain.EstadoPrestamo;
import ec.edu.uteq.appweb.biblioteca.domain.Prestamo;
import ec.edu.uteq.appweb.biblioteca.service.PrestamoService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.PageMeta;
import ec.edu.uteq.appweb.biblioteca.web.dto.PrestamoRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.PrestamoResponse;
import ec.edu.uteq.appweb.biblioteca.web.mapper.PrestamoMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prestamos")
@Tag(name = "Prestamos", description = "API REST de prestamos de libros")
public class PrestamoController {

    private final PrestamoService servicio;
    private final PrestamoMapper mapper;

    public PrestamoController(PrestamoService servicio, PrestamoMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Listar prestamos", description = "Listado paginado por estado (ACTIVO por defecto)")
    public ApiResponse<List<PrestamoResponse>> listar(
            @RequestParam(required = false) EstadoPrestamo estado,
            @PageableDefault(size = 20) Pageable paginacion) {
        EstadoPrestamo filtro = estado != null ? estado : EstadoPrestamo.ACTIVO;
        Page<Prestamo> pagina = servicio.listarPorEstado(filtro, paginacion);
        List<PrestamoResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Prestamos listados", PageMeta.de(pagina));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prestamo por ID")
    public ApiResponse<PrestamoResponse> buscar(@PathVariable Long id) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscarPorId(id)), "Prestamo encontrado");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Registrar prestamo", description = "Requiere rol ADMIN o BIBLIOTECARIO")
    public ResponseEntity<ApiResponse<PrestamoResponse>> registrar(@Valid @RequestBody PrestamoRequest solicitud) {
        Prestamo prestamo = servicio.registrar(solicitud.libroId(), solicitud.socioId(), solicitud.diasPrestamo());
        PrestamoResponse cuerpo = mapper.aRespuesta(prestamo);
        return ResponseEntity
                .created(URI.create("/api/v1/prestamos/" + prestamo.getId()))
                .body(ApiResponse.ok(cuerpo, "Prestamo registrado"));
    }

    @PostMapping("/{id}/devolucion")
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Devolver libro", description = "Registra la devolucion de un prestamo")
    public ApiResponse<PrestamoResponse> devolver(@PathVariable Long id) {
        Prestamo prestamo = servicio.devolver(id);
        return ApiResponse.ok(mapper.aRespuesta(prestamo), "Devolucion registrada");
    }
}
