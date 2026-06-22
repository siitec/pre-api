package mx.tsj.connect.general.web;

import java.util.List;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mx.tsj.connect.general.dto.PartidasPresupuestalesPage;
import mx.tsj.connect.general.dto.PartidaPresupuestalDetalleDto;
import mx.tsj.connect.general.services.PartidasPresupuestalesService;
import mx.tsj.connect.general.services.PartidasExcelService;

@RestController
@RequestMapping("/api/partidas-presupuestales")
@Tag(name = "Partidas presupuestales", description = "Consulta de partidas autorizadas para el usuario")
public class PartidasPresupuestalesController {
    private final PartidasPresupuestalesService partidasService;
    private final PartidasExcelService excelService;

    public PartidasPresupuestalesController(
            PartidasPresupuestalesService partidasService,
            PartidasExcelService excelService) {
        this.partidasService = partidasService;
        this.excelService = excelService;
    }

    @GetMapping
    @Operation(summary = "Consulta las partidas presupuestales autorizadas")
    public ResponseEntity<PartidasPresupuestalesPage> findAll(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "cog") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        return ResponseEntity.ok(partidasService.findAll(
                authentication.getName(),
                page,
                pageSize,
                search,
                sortBy,
                sortOrder));
    }

    @GetMapping("/{cog}/detalle")
    @Operation(summary = "Consulta el detalle por UEG de una partida autorizada")
    public ResponseEntity<List<PartidaPresupuestalDetalleDto>> findDetails(
            Authentication authentication,
            @PathVariable(name = "cog") String cog) {
        return ResponseEntity.ok(partidasService.findDetails(authentication.getName(), cog));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exporta a Excel el detalle autorizado por partida y UEG")
    public ResponseEntity<byte[]> export(
            Authentication authentication,
            @RequestParam(name = "search", required = false) String search) {
        byte[] file = excelService.createWorkbook(
                partidasService.findExportRows(authentication.getName(), search));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("partidas-presupuestales.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }
}
