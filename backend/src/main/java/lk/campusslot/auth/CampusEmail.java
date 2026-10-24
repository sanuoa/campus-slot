package lk.campusslot.auth;

import lk.campusslot.shared.ApiException;
import java.util.Locale;

public final class CampusEmail {
    private CampusEmail() {}
    public static String normalize(String input) {
        if (input == null) throw ApiException.invalid("Use your @iit.ac.lk email address.");
        String email = input.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !email.matches("[a-z0-9._%+\\-]+@iit\\.ac\\.lk")) {
            throw ApiException.invalid("Use your @iit.ac.lk email address.");
        }
        return email;
    }
}
