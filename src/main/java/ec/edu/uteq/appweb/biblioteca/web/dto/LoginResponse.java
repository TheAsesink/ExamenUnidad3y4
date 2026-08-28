package ec.edu.uteq.appweb.biblioteca.web.dto;

public record LoginResponse(String username, String rol, String token, String tokenType, long expiresInSeconds) {
}
