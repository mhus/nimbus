package de.mhus.nimbus.shared.api;

import de.mhus.nimbus.shared.storage.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/shared/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/content/{id}")
    public void getContent(
            @PathVariable String id,
            HttpServletResponse response

    ) {
        var info = storageService.info(id);
        if (info == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404), "Storage id not found");
        }
        response.setContentType(findContentType(info.path()));
        var stream = storageService.load(id);
        if (stream == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404), "Storage content not found");
        }
        try (stream) {
            stream.transferTo(response.getOutputStream());
        } catch (Exception e) {
            log.warn("Cannot stream storage id {}", id, e);
            throw new ResponseStatusException(HttpStatusCode.valueOf(500), "Cannot stream content");
        }
    }

    private String findContentType(String path) {
        if (path == null) return "application/octet-stream";
        path = path.toLowerCase();
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg")) return "image/jpeg";
        if (path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".txt")) return "text/plain";
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".pdf")) return "application/pdf";
        if (path.endsWith(".off")) return "audio/ogg";
        return "application/octet-stream";
    }

}
