package mx.tsj.connect.general.dto;

public record LoginResponse(String token, String tokenType, long expiresIn) {
}
