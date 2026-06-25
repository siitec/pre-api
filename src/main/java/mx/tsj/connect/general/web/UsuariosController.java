package mx.tsj.connect.general.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mx.tsj.connect.general.dto.CreateUsuarioRequest;
import mx.tsj.connect.general.dto.UpdateUsuarioRequest;
import mx.tsj.connect.general.dto.UsuarioRowDto;
import mx.tsj.connect.general.dto.UsuariosCatalogosDto;
import mx.tsj.connect.general.dto.UsuariosPage;
import mx.tsj.connect.general.services.UsuariosExcelService;
import mx.tsj.connect.general.services.UsuariosService;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
public class UsuariosController {
    private final UsuariosService usuariosService;
    private final UsuariosExcelService excelService;

    public UsuariosController(
            UsuariosService usuariosService,
            UsuariosExcelService excelService) {
        this.usuariosService = usuariosService;
        this.excelService = excelService;
    }

    @GetMapping
    @Operation(summary = "Consulta usuarios registrados")
    public ResponseEntity<UsuariosPage> findAll(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "usuario") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        return ResponseEntity.ok(usuariosService.findAll(
                authentication.getName(),
                page,
                pageSize,
                search,
                sortBy,
                sortOrder));
    }

    @GetMapping("/catalogos")
    @Operation(summary = "Consulta catálogos para edición de usuarios")
    public ResponseEntity<UsuariosCatalogosDto> getCatalogs(Authentication authentication) {
        return ResponseEntity.ok(usuariosService.getCatalogs(authentication.getName()));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exporta a Excel los usuarios registrados")
    public ResponseEntity<byte[]> export(
            Authentication authentication,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sortBy", defaultValue = "usuario") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder) {
        byte[] file = excelService.createWorkbook(
                usuariosService.findExportRows(authentication.getName(), search, sortBy, sortOrder));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("usuarios.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un usuario por identificador")
    public ResponseEntity<UsuarioRowDto> findOne(
            Authentication authentication,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(usuariosService.findOne(authentication.getName(), id));
    }

    @PostMapping
    @Operation(summary = "Crea un usuario")
    public ResponseEntity<UsuarioRowDto> create(
            Authentication authentication,
            @Valid @RequestBody CreateUsuarioRequest request) {
        return ResponseEntity.ok(usuariosService.create(authentication.getName(), request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualiza datos generales de un usuario")
    public ResponseEntity<UsuarioRowDto> update(
            Authentication authentication,
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateUsuarioRequest request) {
        return ResponseEntity.ok(usuariosService.update(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactiva un usuario")
    public ResponseEntity<UsuarioRowDto> deactivate(
            Authentication authentication,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(usuariosService.deactivate(authentication.getName(), id));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activa un usuario")
    public ResponseEntity<UsuarioRowDto> activate(
            Authentication authentication,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(usuariosService.activate(authentication.getName(), id));
    }
}
