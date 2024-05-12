package info.vizhanyo.parkingmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class PMController {

	private static final Logger logger = LoggerFactory.getLogger(PMController.class);

    public PMController(PMService s) {
        svc = s;
    }

    private final PMService svc;

    @PostMapping("/enter/{licPlate}")
    public ResponseEntity<String> enter(@PathVariable String licPlate, @RequestBody String bodyString) {
        try {
            String lot = svc.enter(licPlate);
            return ResponseEntity.ok(lot);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @PostMapping("/exit/{licPlate}")
    public ResponseEntity<Object> exit(@PathVariable String licPlate) {
        try {
            svc.exit(licPlate);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
