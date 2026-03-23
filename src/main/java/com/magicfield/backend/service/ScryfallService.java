package com.magicfield.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ScryfallService {

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
}