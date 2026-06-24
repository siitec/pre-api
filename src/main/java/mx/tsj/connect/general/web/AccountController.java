package mx.tsj.connect.general.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mx.tsj.connect.general.dto.AccountProfileDto;
import mx.tsj.connect.general.dto.UpdatePasswordRequest;
import mx.tsj.connect.general.dto.UpdatePuestoRequest;
import mx.tsj.connect.general.services.AccountService;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Cuenta", description = "Consulta y actualizacion de la cuenta autenticada")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta los datos de la cuenta autenticada")
    public ResponseEntity<AccountProfileDto> getProfile(Authentication authentication) {
        return ResponseEntity.ok(accountService.getProfile(authentication.getName()));
    }

    @PatchMapping("/me/puesto")
    @Operation(summary = "Actualiza el puesto del usuario autenticado")
    public ResponseEntity<AccountProfileDto> updatePuesto(
            Authentication authentication,
            @Valid @RequestBody UpdatePuestoRequest request) {
        return ResponseEntity.ok(accountService.updatePuesto(authentication.getName(), request));
    }

    @PostMapping("/me/password")
    @Operation(summary = "Actualiza la contraseña del usuario autenticado")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @Valid @RequestBody UpdatePasswordRequest request) {
        accountService.updatePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
