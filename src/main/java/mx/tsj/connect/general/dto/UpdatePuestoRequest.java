package mx.tsj.connect.general.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePuestoRequest(
        @NotBlank
        @Size(max = 100)
        String puesto) {
}
