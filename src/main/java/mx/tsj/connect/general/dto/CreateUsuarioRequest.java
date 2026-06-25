package mx.tsj.connect.general.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUsuarioRequest(
        @NotBlank
        @Size(max = 50)
        String usuario,

        @NotBlank
        @Size(max = 200)
        String nombre,

        @NotBlank
        @Size(max = 2)
        String ua,

        @Size(max = 100)
        String area,

        @Size(max = 100)
        String puesto,

        @Size(max = 200)
        String responsable,

        @NotNull
        Integer roleId) {
}
