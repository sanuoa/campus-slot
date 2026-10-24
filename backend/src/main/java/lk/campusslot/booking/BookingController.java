package lk.campusslot.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.List;

@RestController
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service) { this.service=service; }
    public record BookRequest(@NotNull Long slotId) {}
    @GetMapping("/api/bookings/mine") public List<BookingService.BookingView> mine(Authentication auth) { return service.mine(auth.getName()); }
    @PostMapping("/api/bookings") @ResponseStatus(HttpStatus.CREATED)
    public BookingService.BookingView create(Authentication auth,@Valid @RequestBody BookRequest request) { return service.create(auth.getName(),request.slotId()); }
    @PostMapping("/api/bookings/{id}/cancel") public BookingService.BookingView cancel(Authentication auth,@PathVariable Long id) { return service.cancel(auth.getName(),id,false); }
    @GetMapping("/api/admin/bookings") public List<BookingService.BookingView> all(@RequestParam LocalDate date) { return service.forDate(date); }
    @PostMapping("/api/admin/bookings/{id}/cancel") public BookingService.BookingView adminCancel(Authentication auth,@PathVariable Long id) { return service.cancel(auth.getName(),id,true); }
}
