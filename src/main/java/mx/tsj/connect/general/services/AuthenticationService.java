package mx.tsj.connect.general.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.repositories.UsuarioRepository;

@Service
public class AuthenticationService {
    private static final Set<String> INACTIVE_STATUSES = Set.of("INACTIVO", "BAJA", "0", "N");
    private final UsuarioRepository usuarioRepository;

    public AuthenticationService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario authenticate(String username, String password) {
        Usuario usuario = usuarioRepository.findFirstByUsuarioIgnoreCase(username.trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (isInactive(usuario.getStatus()) || !passwordMatches(password, usuario.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return usuario;
    }

    private boolean passwordMatches(String supplied, String stored) {
        if (stored == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isInactive(String status) {
        return status != null && INACTIVE_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Credenciales incorrectas.");
        }
    }
}
