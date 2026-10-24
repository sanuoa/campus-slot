package lk.campusslot.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;
    public AuthController(AuthService auth, UserRepository users) { this.auth = auth; this.users = users; }
    public record Registration(@NotBlank @Size(max=80) String name, @NotBlank @Email @Size(max=254) String email,
                               @NotBlank @Size(min=12,max=72) String password) {}
    public record VerifyRequest(@NotBlank String token) {}
    public record UserView(Long id, String name, String email, CampusUser.Role role) {}
    @GetMapping("/csrf") public CsrfToken csrf(CsrfToken token) { return token; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody Registration request) { auth.register(request.name(), request.email(), request.password()); }
    @PostMapping("/verify") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyRequest request) { auth.verify(request.token()); }
    @GetMapping("/me") public UserView me(Authentication authentication) {
        var user = users.findByEmail(authentication.getName()).orElseThrow();
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
