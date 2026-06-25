package mx.tsj.connect.general.dto;

import jakarta.validation.constraints.Size;

public record UpdateUsuarioRequest(
        @Size(max = 200)
        String nombre,

        @Size(max = 2)
        String ua,

        @Size(max = 100)
        String area,

        @Size(max = 100)
        String puesto,

        @Size(max = 200)
        String responsable,

        Integer roleId) {
}
