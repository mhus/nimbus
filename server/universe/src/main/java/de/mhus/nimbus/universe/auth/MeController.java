package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.security.CurrentUser;
import de.mhus.nimbus.universe.security.RequestUserHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MeController.BASE_PATH)
public class MeController {

    public static final String BASE_PATH = "/universe/user";

    private final RequestUserHolder userHolder;

    public MeController(RequestUserHolder userHolder) {
        this.userHolder = userHolder;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        CurrentUser cu = userHolder.get();
        if (cu == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new MeResponse(cu.userId(), cu.username()));
    }
}
