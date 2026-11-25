package utils;

import java.util.regex.Pattern;

// small input checks used by the forms
public final class Validate {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()-]{7,20}$");

    private Validate() { }

    public static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    public static boolean email(String s) { return s != null && EMAIL.matcher(s.trim()).matches(); }
    public static boolean phone(String s) { return blank(s) || PHONE.matcher(s.trim()).matches(); }   // phone is optional

    // returns an error message, or null when the password is acceptable
    public static String password(String pw) {
        if (pw == null || pw.length() < 6) { return "Password must be at least 6 characters."; }
        return null;
    }
}
