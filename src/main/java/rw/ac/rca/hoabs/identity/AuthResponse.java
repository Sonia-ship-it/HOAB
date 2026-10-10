package rw.ac.rca.hoabs.identity;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresInSeconds, User user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, UserResponse.from(user));
    }
}
