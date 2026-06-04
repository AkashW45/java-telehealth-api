package io.spring.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.Map;

@RestController
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        String service = "flask-contacts-api";
        String commit = System.getenv().getOrDefault("GIT_COMMIT", "unknown");
        String timestamp = Instant.now().toString();
        return ResponseEntity.ok(Map.of("service", service, "commit", commit, "timestamp", timestamp));
    }
}
