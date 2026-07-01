package mx.tsj.connect.general.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavePartidaRequest(
        @NotBlank
        @Size(max = 4)
        String partida,

        @NotBlank
        @Size(max = 250)
        String descripcion,

        @Size(max = 10)
        String um,

        @Size(max = 2)
        String muestra) {
}
