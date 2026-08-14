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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Finishes reales que existen para esa impresión puntual (ej. ["nonfoil","foil"]): una
    // carta no necesariamente tiene las 4 variantes de card_finish (hay promos foil-only,
    // por ejemplo). Se usa para validar el finish elegido al crear/editar un single a mano.
    public List<String> getFinishes(String scryfallId) {
        try {
            acquireRateLimit();
            String url = "https://api.scryfall.com/cards/" + scryfallId;
            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();
            return extractFinishes(response);
        } catch (Exception e) {
            log.error("[Scryfall] error en getFinishes scryfallId={}: {}", scryfallId, e.getMessage());
            return List.of();
        }
    }

    private List<String> extractFinishes(Map response) {
        Object finishes = response.get("finishes");
        if (finishes instanceof List<?> list) {
            return list.stream()
                    .filter(f -> f instanceof String)
                    .map(f -> ((String) f).toLowerCase())
                    .toList();
        }
        return List.of();
    }

    // Scryfall no reporta "glossy" como finish propio -- lo trata como una variante de foil
    // (mismo criterio que ya usa extractPrice, que tampoco tiene un precio "usd_glossy" separado).
    // Si no se pudo obtener el dato de Scryfall (lista vacía), no se bloquea: mejor permitir la
    // operación que romperla por una falla transitoria de una API externa.
    public boolean isFinishAvailable(List<String> scryfallFinishes, String finishShortName) {
        if (scryfallFinishes == null || scryfallFinishes.isEmpty()) return true;
        String key = "GLOSSY".equalsIgnoreCase(finishShortName) ? "foil" : finishShortName.toLowerCase();
        return scryfallFinishes.contains(key);
    }

    // Vocabulario curado de variantes de arte/marco (independiente de finish): Scryfall trae
    // muchos más `frame_effects` de los que interesa mostrar acá (la mayoría son detalles del
    // renderizado del frame estándar -- "legendary", "devoid", "colorshifted", etc. -- no
    // versiones distintas que alguien busque comprar). Solo se mapean las que sí son variantes
    // coleccionables reconocibles. Se guardan como códigos estables (ver Product.variantTags);
    // las labels de VARIANT_TAG_LABELS son solo para mostrar y se pueden retocar sin migrar datos.
    private static final Map<String, String> FRAME_EFFECT_TAGS = Map.of(
            "extendedart", "EXTENDED_ART",
            "showcase", "SHOWCASE"
    );

    public static final Map<String, String> VARIANT_TAG_LABELS = Map.of(
            "BORDERLESS", "Borderless",
            "EXTENDED_ART", "Extended Art",
            "SHOWCASE", "Showcase",
            "FULL_ART", "Full Art"
    );

    private List<String> extractVariantTags(Map response) {
        List<String> tags = new ArrayList<>();
        if ("borderless".equals(response.get("border_color"))) {
            tags.add("BORDERLESS");
        }
        Object frameEffects = response.get("frame_effects");
        if (frameEffects instanceof List<?> list) {
            for (Object fe : list) {
                if (fe instanceof String s) {
                    String tag = FRAME_EFFECT_TAGS.get(s.toLowerCase(Locale.ROOT));
                    if (tag != null && !tags.contains(tag)) tags.add(tag);
                }
            }
        }
        if (Boolean.TRUE.equals(response.get("full_art"))) {
            tags.add("FULL_ART");
        }
        return tags;
    }

    /**
     * Precios + descripción + tags de variante de arte/marco de mercado para muchas cartas en
     * pocas llamadas: Scryfall permite hasta 75 identificadores por request en /cards/collection,
     * en vez de un GET por carta (útil para el import de CSV, donde puede haber hasta ~1000
     * filas, para la actualización periódica de precios, y para el backfill de singles ya
     * creados sin variantTags). Las cartas no encontradas simplemente no aparecen en el resultado.
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
                    byCardId.put(cardId, buildSnapshot(card));
                }
            } catch (Exception e) {
                log.error("[Scryfall] error en getCollectionDataBulk para un lote de {} ids: {}", chunk.size(), e.getMessage());
            }
        }
        return byCardId;
    }

    /**
     * Igual que getCollectionDataBulk pero para una sola carta -- consolida en un único GET lo
     * que antes eran 2-3 llamadas sueltas (getFinishes + getCardData + getPrice) en el alta/
     * edición manual de un single, ya que las 4 cosas vienen en la misma respuesta de Scryfall.
     */
    public ScryfallCollectionData getFullCardData(String scryfallId) {
        try {
            acquireRateLimit();
            String url = "https://api.scryfall.com/cards/" + scryfallId;
            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null) return new ScryfallCollectionData(null, null, List.of(), List.of());
            return buildSnapshot(response);
        } catch (Exception e) {
            log.error("[Scryfall] error en getFullCardData scryfallId={}: {}", scryfallId, e.getMessage());
            return new ScryfallCollectionData(null, null, List.of(), List.of());
        }
    }

    private ScryfallCollectionData buildSnapshot(Map card) {
        Object prices = card.get("prices");
        String description = parseCardData(card).getDescription();
        List<String> variantTags = extractVariantTags(card);
        List<String> finishes = extractFinishes(card);
        return new ScryfallCollectionData(prices instanceof Map ? (Map) prices : null, description, variantTags, finishes);
    }

    public static class ScryfallCollectionData {
        private final Map prices;
        private final String description;
        private final List<String> variantTags;
        private final List<String> finishes;

        public ScryfallCollectionData(Map prices, String description, List<String> variantTags, List<String> finishes) {
            this.prices = prices;
            this.description = description;
            this.variantTags = variantTags;
            this.finishes = finishes;
        }

        public Map getPrices() { return prices; }
        public String getDescription() { return description; }
        public List<String> getVariantTags() { return variantTags; }
        public List<String> getFinishes() { return finishes; }
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

    // Imágenes + oracle text de una impresión puntual de Scryfall son inmutables (mismo
    // criterio que Product.variantTags) -- se cachean en memoria por scryfallId para que
    // listar el catálogo completo (listAll, usado por el slider de novedades y el de
    // relacionados) no dispare un GET a Scryfall por cada single en cada request. Solo se
    // cachean respuestas exitosas: si Scryfall falla o tarda, ese scryfallId simplemente
    // vuelve a intentarse en la próxima llamada en vez de quedar "envenenado" en el caché.
    private final Map<String, ScryfallCardData> cardDataCache = new ConcurrentHashMap<>();

    /**
     * Imágenes + texto de reglas (oracle text) de una carta, en una sola llamada a Scryfall.
     * El texto de reglas se usa como "descripción" del single al mostrarlo (siempre actualizada,
     * sin depender de lo guardado en la base) -- ver parseCardData para el fallback a flavor
     * text en cartas sin habilidades.
     */
    public ScryfallCardData getCardData(String scryfallId) {
        ScryfallCardData cached = cardDataCache.get(scryfallId);
        if (cached != null) return cached;

        try {
            acquireRateLimit();
            String url = "https://api.scryfall.com/cards/" + scryfallId;
            Map response = restTemplate.getForObject(url, Map.class);
            ScryfallCardData data = parseCardData(response);
            cardDataCache.put(scryfallId, data);
            return data;
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