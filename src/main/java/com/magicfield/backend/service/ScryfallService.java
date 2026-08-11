package com.magicfield.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
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

            return extractPrice(prices, finishShortName);

        } catch (Exception e) {
            log.error("[Scryfall] error en getPrice scryfallId={}: {}", scryfallId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Precios + descripción de mercado para muchas cartas en pocas llamadas: Scryfall permite
     * hasta 75 identificadores por request en /cards/collection, en vez de un GET por carta
     * (útil para el import de CSV, donde puede haber hasta ~1000 filas, y para la actualización
     * periódica de precios). Las cartas no encontradas simplemente no aparecen en el resultado.
     */
    public Map<String, ScryfallCollectionData> getCollectionDataBulk(List<String> scryfallIds) {
        Map<String, ScryfallCollectionData> byCardId = new HashMap<>();
        for (List<String> chunk : chunk(scryfallIds, 75)) {
            try {
                acquireRateLimit();
                List<Map<String, String>> identifiers = chunk.stream()
                        .map(id -> Map.of("id", id))
                        .toList();

                RequestEntity<Map<String, Object>> request = RequestEntity
                        .post(URI.create("https://api.scryfall.com/cards/collection"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("identifiers", identifiers));

                Map response = restTemplate.exchange(request, Map.class).getBody();
                if (response == null) continue;

                List<Map> data = (List<Map>) response.get("data");
                if (data == null) continue;

                for (Map card : data) {
                    Object id = card.get("id");
                    if (!(id instanceof String cardId)) continue;
                    Object prices = card.get("prices");
                    String description = parseCardData(card).getDescription();
                    byCardId.put(cardId, new ScryfallCollectionData(prices instanceof Map ? (Map) prices : null, description));
                }
            } catch (Exception e) {
                log.error("[Scryfall] error en getCollectionDataBulk para un lote de {} ids: {}", chunk.size(), e.getMessage());
            }
        }
        return byCardId;
    }

    public static class ScryfallCollectionData {
        private final Map prices;
        private final String description;

        public ScryfallCollectionData(Map prices, String description) {
            this.prices = prices;
            this.description = description;
        }

        public Map getPrices() { return prices; }
        public String getDescription() { return description; }
    }

    // Scryfall no tiene un precio "usd_glossy" propio: usamos el de foil como
    // mejor aproximación disponible para ese finish (poco común, sin dato exacto).
    public BigDecimal extractPrice(Map prices, String finishShortName) {
        if (prices == null) return BigDecimal.ZERO;

        String priceKey = switch (finishShortName == null ? "NONFOIL" : finishShortName.toUpperCase()) {
            case "FOIL", "GLOSSY" -> "usd_foil";
            case "ETCHED" -> "usd_etched";
            default -> "usd";
        };

        Object priceStr = prices.get(priceKey);
        if (!(priceStr instanceof String s)) return BigDecimal.ZERO;

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    public List<String> getImageUrls(String scryfallId) {
        return getCardData(scryfallId).getImageUrls();
    }

    /**
     * Imágenes + texto de reglas (oracle text) de una carta, en una sola llamada a Scryfall.
     * El texto de reglas se usa como "descripción" del single al mostrarlo (siempre actualizada,
     * sin depender de lo guardado en la base) -- ver parseCardData para el fallback a flavor
     * text en cartas sin habilidades.
     */
    public ScryfallCardData getCardData(String scryfallId) {
        try {
            acquireRateLimit();
            String url = "https://api.scryfall.com/cards/" + scryfallId;
            Map response = restTemplate.getForObject(url, Map.class);
            return parseCardData(response);
        } catch (Exception e) {
            log.error("[Scryfall] error en getCardData scryfallId={}: {}", scryfallId, e.getMessage());
            return new ScryfallCardData(new ArrayList<>(), null);
        }
    }

    // Descripción = oracle text (texto de reglas). Si la carta no tiene (p.ej. una criatura
    // "vainilla" sin habilidades), se usa el flavor text como respaldo para no dejarla sin
    // ningún texto -- mejor eso que nada, tanto para mostrarla como para poder buscarla.
    private ScryfallCardData parseCardData(Map response) {
        List<String> urls = new ArrayList<>();
        String description = null;

        // Carta con dos caras
        Object faces = response.get("card_faces");
        if (faces instanceof List<?> faceList) {
            List<String> oracleTexts = new ArrayList<>();
            List<String> flavorTexts = new ArrayList<>();
            for (Object face : faceList) {
                if (face instanceof Map<?, ?> faceMap) {
                    Map<?, ?> imageUris = (Map<?, ?>) faceMap.get("image_uris");
                    if (imageUris != null) {
                        String normal = (String) imageUris.get("normal");
                        if (normal != null) urls.add(normal);
                    }
                    Object oracleText = faceMap.get("oracle_text");
                    if (oracleText instanceof String s && !s.isBlank()) oracleTexts.add(s);
                    Object flavorText = faceMap.get("flavor_text");
                    if (flavorText instanceof String s && !s.isBlank()) flavorTexts.add(s);
                }
            }
            if (!oracleTexts.isEmpty()) {
                description = String.join("\n---\n", oracleTexts);
            } else if (!flavorTexts.isEmpty()) {
                description = String.join("\n---\n", flavorTexts);
            }
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
            if (oracleText instanceof String s && !s.isBlank()) {
                description = s;
            } else {
                Object flavorText = response.get("flavor_text");
                if (flavorText instanceof String s && !s.isBlank()) description = s;
            }
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