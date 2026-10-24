package lk.campusslot.auth;

/** Abstraction: the registration service does not need to know how mail is delivered. */
public interface VerificationSender {
    void send(String email, String verificationUrl);
}
