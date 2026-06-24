package mx.tsj.connect.general.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.tsj.connect.general.dto.AccountProfileDto;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.dto.UpdatePasswordRequest;
import mx.tsj.connect.general.dto.UpdatePuestoRequest;
import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.repositories.UsuarioRepository;
import mx.tsj.connect.general.security.JwtService;

@Service
public class AccountService {
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JwtService jwtService;

    public AccountService(
            UsuarioRepository usuarioRepository,
            AuthorizationService authorizationService,
            NamedParameterJdbcTemplate jdbcTemplate,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
    }

    public AccountProfileDto getProfile(String username) {
        Usuario usuario = findUser(username);
        return buildProfile(usuario);
    }

    @Transactional
    public AccountProfileDto updatePuesto(String username, UpdatePuestoRequest request) {
        Usuario usuario = findUser(username);
        usuario.setPuesto(request.puesto().trim());
        usuarioRepository.save(usuario);
        List<ModuloAutorizado> modulos = authorizationService.getModulos(usuario.getId());
        return buildProfile(usuario, modulos, jwtService.createToken(usuario, modulos));
    }

    @Transactional
    public void updatePassword(String username, UpdatePasswordRequest request) {
        Usuario usuario = findUser(username);
        if (!passwordMatches(request.currentPassword(), usuario.getPassword())) {
            throw new AccountException("La contraseña actual no es correcta.");
        }
        usuario.setPassword(request.newPassword());
        usuarioRepository.save(usuario);
    }

    private AccountProfileDto buildProfile(Usuario usuario) {
        return buildProfile(usuario, authorizationService.getModulos(usuario.getId()), null);
    }

    private AccountProfileDto buildProfile(
            Usuario usuario,
            List<ModuloAutorizado> modulos,
            String token) {
        List<String> roles = modulos.stream()
                .flatMap(module -> module.roles().stream())
                .distinct()
                .sorted()
                .toList();

        return new AccountProfileDto(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getNombre(),
                null,
                usuario.getStatus(),
                roles,
                usuario.getUa(),
                findUnidadEjecutora(usuario.getUa()),
                usuario.getResponsable(),
                usuario.getArea(),
                usuario.getPuesto(),
                token);
    }

    private Usuario findUser(String username) {
        return usuarioRepository.findFirstByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new AccountException("No fue posible localizar la cuenta del usuario."));
    }

    private String findUnidadEjecutora(String ua) {
        if (ua == null || ua.isBlank()) {
            return null;
        }
        try {
            Integer uaId = Integer.valueOf(ua.trim());
            String sql = "SELECT TOP 1 Nombre FROM dbo.UA WHERE ID = :uaId";
            return jdbcTemplate.query(sql, new MapSqlParameterSource("uaId", uaId), resultSet ->
                    resultSet.next() ? resultSet.getString("Nombre") : null);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean passwordMatches(String supplied, String stored) {
        if (stored == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }
}
