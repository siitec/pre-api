package mx.tsj.connect.general.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveProveedorRequest(
        @NotBlank
        @Size(max = 50)
        String codigo,

        @NotBlank
        @Size(max = 200)
        String nombre,

        @Size(max = 13)
        String rfc,

        @Size(max = 100)
        String calle,

        @Size(max = 10)
        String numExt,

        @Size(max = 10)
        String numInt,

        @Size(max = 50)
        String colonia,

        @Size(max = 10)
        String codigoP,

        @Size(max = 15)
        String telefono1,

        @Size(max = 15)
        String telefono2,

        @Size(max = 15)
        String telefono3,

        @Size(max = 15)
        String telefono4,

        @Size(max = 100)
        String email,

        @Size(max = 50)
        String municipio,

        @Size(max = 50)
        String ciudad,

        @Size(max = 50)
        String estado,

        @Size(max = 55)
        String pais,

        @Size(max = 100)
        String representante) {
}
