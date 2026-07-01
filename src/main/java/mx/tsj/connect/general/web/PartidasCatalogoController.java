package mx.tsj.connect.general.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mx.tsj.connect.general.dto.PartidaCatalogoDto;
import mx.tsj.connect.general.dto.PartidasCatalogoPage;
import mx.tsj.connect.general.dto.PartidasCatalogosDto;
import mx.tsj.connect.general.dto.SavePartidaRequest;
import mx.tsj.connect.general.services.PartidasCatalogoExcelService;
import mx.tsj.connect.general.services.PartidasCatalogoService;

@RestController
@RequestMapping("/api/partidas")
@Tag(name = "Productos/Servicios", description = "Administración del catálogo de productos y servicios")
public class PartidasCatalogoController {
    private final PartidasCatalogoService partidasService;
    private final PartidasCatalogoExcelService excelService;

    public PartidasCatalogoController(
            PartidasCatalogoService partidasService,
            PartidasCatalogoExcelService excelService) {
        this.partidasService = partidasService;
        this.excelService = excelService;
    }

    @GetMapping
    @Operation(summary = "Consulta productos y servicios registrados")
    public ResponseEntity<PartidasCatalogoPage> findAll(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "partida") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        return ResponseEntity.ok(partidasService.findAll(
                authentication.getName(),
                page,
                pageSize,
                search,
                sortBy,
                sortOrder));
    }

    @GetMapping("/catalogos")
    @Operation(summary = "Consulta catálogos para edición de productos y servicios")
    public ResponseEntity<PartidasCatalogosDto> getCatalogs(Authentication authentication) {
        return ResponseEntity.ok(partidasService.getCatalogs(authentication.getName()));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exporta a Excel los productos y servicios registrados")
    public ResponseEntity<byte[]> export(
            Authentication authentication,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "partida") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        byte[] file = excelService.createWorkbook(
                partidasService.findExportRows(authentication.getName(), search, sortBy, sortOrder));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("productos-servicios.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un producto o servicio por identificador")
    public ResponseEntity<PartidaCatalogoDto> findOne(
            Authentication authentication,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(partidasService.findOne(authentication.getName(), id));
    }

    @PostMapping
    @Operation(summary = "Crea un producto o servicio")
    public ResponseEntity<PartidaCatalogoDto> create(
            Authentication authentication,
            @Valid @RequestBody SavePartidaRequest request) {
        return ResponseEntity.ok(partidasService.create(authentication.getName(), request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualiza un producto o servicio")
    public ResponseEntity<PartidaCatalogoDto> update(
            Authentication authentication,
            @PathVariable("id") Integer id,
            @Valid @RequestBody SavePartidaRequest request) {
        return ResponseEntity.ok(partidasService.update(authentication.getName(), id, request));
    }
}
