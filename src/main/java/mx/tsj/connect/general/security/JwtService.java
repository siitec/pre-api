package mx.tsj.connect.general.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.entities.Usuario;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String createToken(Usuario usuario, List<ModuloAutorizado> modulos) {
        Instant issuedAt = Instant.now();
        var builder = Jwts.builder()
                .subject(usuario.getUsuario())
                .claim("id", usuario.getId())
                .claim("usuario", usuario.getUsuario())
                .claim("nombre", usuario.getNombre())
                .claim("modulos", modulos)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)));

        addClaim(builder, "ua", usuario.getUa());
        addClaim(builder, "area", usuario.getArea());
        addClaim(builder, "puesto", usuario.getPuesto());
        addClaim(builder, "permisos", usuario.getPermisos());
        return builder.signWith(key).compact();
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private void addClaim(io.jsonwebtoken.JwtBuilder builder, String name, String value) {
        if (value != null) {
            builder.claim(name, value);
        }
    }
}
