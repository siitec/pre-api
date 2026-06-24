package mx.tsj.connect.general.dto;

import java.util.List;

public record AccountProfileDto(
        Integer id,
        String usuario,
        String nombre,
        String correo,
        String status,
        List<String> roles,
        String ua,
        String unidadEjecutoraGasto,
        String responsableArea,
        String area,
        String puesto,
        String token) {
}
