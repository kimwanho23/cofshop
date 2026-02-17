package kwh.cofshop.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

public final class RefreshTokenCookiePolicy {

    public static final String COOKIE_NAME = "refreshToken";
    public static final String COOKIE_PATH = "/api/auth";
    public static final long MAX_AGE_SECONDS = 7L * 24 * 60 * 60;

    private RefreshTokenCookiePolicy() {
    }

    public static ResponseCookie issue(String refreshToken, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(MAX_AGE_SECONDS)
                .build();
    }

    public static ResponseCookie expire(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    public static String extract(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
