package mx.tsj.connect.general.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank
        @Size(max = 50)
        String currentPassword,

        @NotBlank
        @Size(min = 4, max = 50)
        String newPassword) {
}
