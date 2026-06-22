package mx.tsj.connect.general.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mx.tsj.connect.general.dto.DashboardPresupuestoDto;
import mx.tsj.connect.general.services.PartidasPresupuestalesService;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores presupuestales autorizados para el usuario")
public class DashboardController {
    private final PartidasPresupuestalesService partidasService;

    public DashboardController(PartidasPresupuestalesService partidasService) {
        this.partidasService = partidasService;
    }

    @GetMapping("/presupuesto")
    @Operation(summary = "Consulta los indicadores presupuestales del usuario")
    public ResponseEntity<DashboardPresupuestoDto> getBudgetDashboard(Authentication authentication) {
        return ResponseEntity.ok(partidasService.getDashboard(authentication.getName()));
    }
}
