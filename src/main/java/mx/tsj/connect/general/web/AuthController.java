package mx.tsj.connect.general.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mx.tsj.connect.general.dto.LoginRequest;
import mx.tsj.connect.general.dto.LoginResponse;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.security.JwtService;
import mx.tsj.connect.general.services.AuthenticationService;
import mx.tsj.connect.general.services.AuthorizationService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión del sistema presupuestal")
public class AuthController {
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationService authenticationService,
            AuthorizationService authorizationService,
            JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con la tabla dbo.Usuarios")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = authenticationService.authenticate(request.usuario(), request.password());
        List<ModuloAutorizado> modulos = authorizationService.getModulos(usuario.getId());
        return ResponseEntity.ok(new LoginResponse(
                jwtService.createToken(usuario, modulos),
                "Bearer",
                jwtService.getExpirationSeconds()));
    }
}
