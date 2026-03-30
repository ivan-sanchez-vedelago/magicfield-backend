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

    private final RestTemplate restTemplate = new RestTemplate();

    public BigDecimal getPrice(String scryfallId, Boolean isFoil) {
        try {
            String url = "https://api.scryfall.com/cards/" + scryfallId;

            Map response = restTemplate.getForObject(url, Map.class);
            Map prices = (Map) response.get("prices");

            String usd = (String) prices.get("usd");
            String usdFoil = (String) prices.get("usd_foil");

            String priceStr = (isFoil != null && isFoil) ? usdFoil : usd;

            if (priceStr == null) return BigDecimal.ZERO;

            return new BigDecimal(priceStr);

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public List<String> getImageUrls(String scryfallId) {
        List<String> urls = new ArrayList<>();
        try {
            String url = "https://api.scryfall.com/cards/" + scryfallId;
            Map response = restTemplate.getForObject(url, Map.class);

            // Carta con dos caras
            Object faces = response.get("card_faces");
            if (faces instanceof List<?> faceList) {
                for (Object face : faceList) {
                    if (face instanceof Map<?, ?> faceMap) {
                        Map<?, ?> imageUris = (Map<?, ?>) faceMap.get("image_uris");
                        if (imageUris != null) {
                            String normal = (String) imageUris.get("normal");
                            if (normal != null) urls.add(normal);
                        }
                    }
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

        } catch (Exception e) {
            log.error("[Scryfall] error en getImageUrls scryfallId={}: {}", scryfallId, e.getMessage());
        }
        return urls;
    }
}