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
import mx.tsj.connect.general.dto.ProveedorDto;
import mx.tsj.connect.general.dto.ProveedoresCatalogosDto;
import mx.tsj.connect.general.dto.ProveedoresPage;
import mx.tsj.connect.general.dto.SaveProveedorRequest;
import mx.tsj.connect.general.services.ProveedoresExcelService;
import mx.tsj.connect.general.services.ProveedoresService;

@RestController
@RequestMapping("/api/proveedores")
@Tag(name = "Proveedores", description = "Gestión del catálogo de proveedores")
public class ProveedoresController {
    private final ProveedoresService proveedoresService;
    private final ProveedoresExcelService excelService;

    public ProveedoresController(
            ProveedoresService proveedoresService,
            ProveedoresExcelService excelService) {
        this.proveedoresService = proveedoresService;
        this.excelService = excelService;
    }

    @GetMapping
    @Operation(summary = "Consulta proveedores registrados")
    public ResponseEntity<ProveedoresPage> findAll(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "nombre") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        return ResponseEntity.ok(proveedoresService.findAll(
                authentication.getName(),
                page,
                pageSize,
                search,
                sortBy,
                sortOrder));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exporta a Excel los proveedores registrados")
    public ResponseEntity<byte[]> export(
            Authentication authentication,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "nombre") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        byte[] file = excelService.createWorkbook(
                proveedoresService.findExportRows(authentication.getName(), search, sortBy, sortOrder));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("proveedores.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }

    @GetMapping("/catalogos")
    @Operation(summary = "Consulta catálogos para edición de proveedores")
    public ResponseEntity<ProveedoresCatalogosDto> getCatalogs(Authentication authentication) {
        return ResponseEntity.ok(proveedoresService.getCatalogs(authentication.getName()));
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Consulta un proveedor por código")
    public ResponseEntity<ProveedorDto> findOne(
            Authentication authentication,
            @PathVariable("codigo") String codigo) {
        return ResponseEntity.ok(proveedoresService.findOne(authentication.getName(), codigo));
    }

    @PostMapping
    @Operation(summary = "Crea un proveedor")
    public ResponseEntity<ProveedorDto> create(
            Authentication authentication,
            @Valid @RequestBody SaveProveedorRequest request) {
        return ResponseEntity.ok(proveedoresService.create(authentication.getName(), request));
    }

    @PatchMapping("/{codigo}")
    @Operation(summary = "Actualiza un proveedor")
    public ResponseEntity<ProveedorDto> update(
            Authentication authentication,
            @PathVariable("codigo") String codigo,
            @Valid @RequestBody SaveProveedorRequest request) {
        return ResponseEntity.ok(proveedoresService.update(authentication.getName(), codigo, request));
    }
}
