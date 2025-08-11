package de.mhus.nimbus.world.shared.client;

import de.mhus.nimbus.shared.dto.terrain.*;
import de.mhus.nimbus.shared.dto.terrain.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerrainServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${terrain.service.url:http://localhost:8083}")
    private String terrainServiceUrl;

    @Value("${world.auth.secret}")
    private String authSecret;

    private WebClient getWebClient() {
        return webClientBuilder
                .baseUrl(terrainServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-World-Auth", authSecret)
                .build();
    }

    // Material operations
    public Mono<MaterialDto> createMaterial(MaterialDto materialDto) {
        return getWebClient()
                .post()
                .uri("/materials")
                .bodyValue(materialDto)
                .retrieve()
                .bodyToMono(MaterialDto.class)
                .doOnError(error -> log.error("Error creating material", error));
    }

    public Mono<MaterialDto> getMaterial(Integer id) {
        return getWebClient()
                .get()
                .uri("/materials/{id}", id)
                .retrieve()
                .bodyToMono(MaterialDto.class)
                .doOnError(error -> log.error("Error getting material with id: " + id, error));
    }

    public Mono<Page<MaterialDto>> getMaterials(int page, int size) {
        return getWebClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/materials")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(Page.class)
                .doOnError(error -> log.error("Error getting materials", error));
    }

    public Mono<MaterialDto> updateMaterial(Integer id, MaterialDto materialDto) {
        return getWebClient()
                .put()
                .uri("/materials/{id}", id)
                .bodyValue(materialDto)
                .retrieve()
                .bodyToMono(MaterialDto.class)
                .doOnError(error -> log.error("Error updating material with id: " + id, error));
    }

    public Mono<Void> deleteMaterial(Integer id) {
        return getWebClient()
                .delete()
                .uri("/materials/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Error deleting material with id: " + id, error));
    }

    // Map operations
    public Mono<Void> createMap(MapCreateRequest request) {
        return getWebClient()
                .post()
                .uri("/maps")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Error creating map", error));
    }

    public Mono<ClusterDto> getMap(String world, Integer level, Integer x, Integer y) {
        return getWebClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/{x}/{y}")
                        .queryParam("world", world)
                        .queryParam("level", level)
                        .build(x, y))
                .retrieve()
                .bodyToMono(ClusterDto.class)
                .doOnError(error -> log.error("Error getting map cluster", error));
    }

    public Mono<List<ClusterDto>> getMapBatch(MapBatchRequest request) {
        return getWebClient()
                .post()
                .uri("/maps/batch")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(List.class)
                .doOnError(error -> log.error("Error getting map batch", error));
    }

    public Mono<Void> updateMap(MapCreateRequest request) {
        return getWebClient()
                .put()
                .uri("/maps")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Error updating map", error));
    }

    public Mono<Void> deleteMapLevel(String world, Integer level) {
        return getWebClient()
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/level")
                        .queryParam("world", world)
                        .queryParam("level", level)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Error deleting map level", error));
    }

    // Sprite operations
    public Mono<List<String>> createSprites(SpriteCreateRequest request) {
        return getWebClient()
                .post()
                .uri("/sprites")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(List.class)
                .doOnError(error -> log.error("Error creating sprites", error));
    }

    public Mono<SpriteDto> getSprite(String id) {
        return getWebClient()
                .get()
                .uri("/sprites/{id}", id)
                .retrieve()
                .bodyToMono(SpriteDto.class)
                .doOnError(error -> log.error("Error getting sprite with id: " + id, error));
    }

    public Mono<List<SpriteDto>> getSpritesInCluster(String world, Integer level, Integer x, Integer y) {
        return getWebClient()
                .get()
                .uri("/sprites/{world}/{level}/{x}/{y}", world, level, x, y)
                .retrieve()
                .bodyToMono(List.class)
                .doOnError(error -> log.error("Error getting sprites in cluster", error));
    }

    public Mono<SpriteDto> updateSprite(String id, SpriteDto spriteDto) {
        return getWebClient()
                .put()
                .uri("/sprites/{id}", id)
                .bodyValue(spriteDto)
                .retrieve()
                .bodyToMono(SpriteDto.class)
                .doOnError(error -> log.error("Error updating sprite with id: " + id, error));
    }

    public Mono<Void> deleteSprite(String id) {
        return getWebClient()
                .delete()
                .uri("/sprites/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Error deleting sprite with id: " + id, error));
    }

    // Asset operations
    public Mono<AssetDto> createAsset(AssetDto assetDto) {
        return getWebClient()
                .post()
                .uri("/assets")
                .bodyValue(assetDto)
                .retrieve()
                .bodyToMono(AssetDto.class)
                .doOnError(error -> log.error("Error creating asset", error));
    }

    public Mono<AssetDto> getAsset(String world, String name) {
        return getWebClient()
                .get()
                .uri("/assets/{world}/{name}", world, name)
                .retrieve()
                .bodyToMono(AssetDto.class)
                .doOnError(error -> log.error("Error getting asset", error));
    }

    public Mono<Page<AssetDto>> getAssets(String world, int page, int size) {
        return getWebClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/assets/{world}")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(world))
                .retrieve()
                .bodyToMono(Page.class)
                .doOnError(error -> log.error("Error getting assets", error));
    }
}
