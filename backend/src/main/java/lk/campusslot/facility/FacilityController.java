package lk.campusslot.facility;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;

@RestController
public class FacilityController {
    private final FacilityService service;
    public FacilityController(FacilityService service) { this.service=service; }
    public record FacilityRequest(@NotBlank @Size(max=100) String name,@NotNull Facility.Type type,
                                  @NotBlank @Size(max=160) String location,@Min(1) @Max(10000) int capacity, boolean active) {}
    public record SlotRequest(@NotNull LocalDate date,@Min(0) @Max(23) int openingHour,@Min(1) @Max(24) int closingHour) {}
    public record BlockRequest(boolean blocked) {}
    @GetMapping("/api/facilities") public List<FacilityService.FacilityView> list(Authentication auth) {
        return service.list(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
    @GetMapping("/api/facilities/{id}/slots") public List<FacilityService.SlotView> slots(@PathVariable Long id,@RequestParam LocalDate date) { return service.slots(id,date); }
    @PostMapping("/api/admin/facilities") @ResponseStatus(HttpStatus.CREATED)
    public FacilityService.FacilityView create(@Valid @RequestBody FacilityRequest r) { return service.create(r.name(),r.type(),r.location(),r.capacity()); }
    @PutMapping("/api/admin/facilities/{id}") public FacilityService.FacilityView update(@PathVariable Long id,@Valid @RequestBody FacilityRequest r) { return service.update(id,r.name(),r.type(),r.location(),r.capacity(),r.active()); }
    @PostMapping("/api/admin/facilities/{id}/slots") public Map<String,Integer> generate(@PathVariable Long id,@Valid @RequestBody SlotRequest r) { return Map.of("created",service.generate(id,r.date(),r.openingHour(),r.closingHour())); }
    @PatchMapping("/api/admin/slots/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long id,@RequestBody BlockRequest r) { service.block(id,r.blocked()); }
}
