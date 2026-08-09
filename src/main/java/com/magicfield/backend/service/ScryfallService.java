package com.magicfield.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ScryfallService {

    private static final Logger log = LoggerFactory.getLogger(ScryfallService.class);

    // Scryfall permite < 10 req/s. Forzamos >= 120ms entre llamadas (~8 req/s máximo).
    private static final long MIN_REQUEST_INTERVAL_MS = 120;
    private final Object rateLock = new Object();
    private long lastRequestMs = 0;

    private final RestTemplate restTemplate = new RestTemplate();

    private void acquireRateLimit() {
        synchronized (rateLock) {
            long now = System.currentTimeMillis();
            long wait = MIN_REQUEST_INTERVAL_MS - (now - lastRequestMs);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestMs = System.currentTimeMillis();
        }
    }

    // finishShortName: el short_name de CardFinish (NONFOIL/FOIL/ETCHED/GLOSSY).
    public BigDecimal getPrice(String scryfallId, String finishShortName) {
        try {
            acquireRateLimit();
            String url = "https://api.scryfall.com/cards/" + scryfallId;

            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.warn("[Scryfall] getPrice retornó null para scryfallId={}", scryfallId);
                return BigDecimal.ZERO;
            }

            Map prices = (Map) response.get("prices");
            if (prices == null) {
                log.warn("[Scryfall] sin mapa de precios para scryfallId={}", scryfallId);
                return BigDecimal.ZERO;
            }

            // Scryfall no tiene un precio "usd_glossy" propio: usamos el de foil como
            // mejor aproximación disponible para ese finish (poco común, sin dato exacto).
            String priceKey = switch (finishShortName == null ? "NONFOIL" : finishShortName.toUpperCase()) {
                case "FOIL", "GLOSSY" -> "usd_foil";
                case "ETCHED" -> "usd_etched";
                default -> "usd";
            };

            String priceStr = (String) prices.get(priceKey);
            if (priceStr == null) return BigDecimal.ZERO;

            return new BigDecimal(priceStr);

        } catch (Exception e) {
            log.error("[Scryfall] error en getPrice scryfallId={}: {}", scryfallId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public List<String> getImageUrls(String scryfallId) {
        return getCardData(scryfallId).getImageUrls();
    }

    /**
     * Imágenes + texto de reglas (oracle text) de una carta, en una sola llamada a Scryfall.
     * El texto de reglas se usa como "descripción" del single en vez de guardar una propia en
     * la base — así queda siempre actualizado y no depende de si el producto se creó a mano o
     * se importó por CSV (donde no hay ningún dato de descripción disponible).
     */
    public ScryfallCardData getCardData(String scryfallId) {
        List<String> urls = new ArrayList<>();
        String description = null;
        try {
            acquireRateLimit();
            String url = "https://api.scryfall.com/cards/" + scryfallId;
            Map response = restTemplate.getForObject(url, Map.class);

            // Carta con dos caras
            Object faces = response.get("card_faces");
            if (faces instanceof List<?> faceList) {
                List<String> texts = new ArrayList<>();
                for (Object face : faceList) {
                    if (face instanceof Map<?, ?> faceMap) {
                        Map<?, ?> imageUris = (Map<?, ?>) faceMap.get("image_uris");
                        if (imageUris != null) {
                            String normal = (String) imageUris.get("normal");
                            if (normal != null) urls.add(normal);
                        }
                        Object oracleText = faceMap.get("oracle_text");
                        if (oracleText instanceof String s && !s.isBlank()) texts.add(s);
                    }
                }
                if (!texts.isEmpty()) description = String.join("\n---\n", texts);
            }

            // Carta normal (una cara)
            if (urls.isEmpty()) {
                Map<?, ?> imageUris = (Map<?, ?>) response.get("image_uris");
                if (imageUris != null) {
                    String normal = (String) imageUris.get("normal");
                    if (normal != null) urls.add(normal);
                }
            }
            if (description == null) {
                Object oracleText = response.get("oracle_text");
                if (oracleText instanceof String s && !s.isBlank()) description = s;
            }

        } catch (Exception e) {
            log.error("[Scryfall] error en getCardData scryfallId={}: {}", scryfallId, e.getMessage());
        }
        return new ScryfallCardData(urls, description);
    }

    public static class ScryfallCardData {
        private final List<String> imageUrls;
        private final String description;

        public ScryfallCardData(List<String> imageUrls, String description) {
            this.imageUrls = imageUrls;
            this.description = description;
        }

        public List<String> getImageUrls() { return imageUrls; }
        public String getDescription() { return description; }
    }
}