package utils;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// password hashing with PBKDF2 (no extra library needed)
// stored format: PBKDF2$iterations$salt$hash
public final class PasswordUtil {

    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;

    private PasswordUtil() { }

    // hash a new password with a random salt
    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "PBKDF2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    // check a password against a stored hash
    public static boolean verify(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
            int diff = expected.length ^ actual.length;
            for (int i = 0; i < Math.min(expected.length, actual.length); i++) { diff |= expected[i] ^ actual[i]; }
            return diff == 0;
        } catch (RuntimeException ex) {
            return false;   // malformed hash
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)).getEncoded();
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is not available", ex);
        }
    }
}
