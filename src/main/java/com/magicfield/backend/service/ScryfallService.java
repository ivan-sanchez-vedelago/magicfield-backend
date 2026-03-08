package com.magicfield.backend.service;

import com.magicfield.backend.dto.ScryfallCardResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Service
public class ScryfallService {

    private static final String SCRYFALL_API_URL = "https://api.scryfall.com/cards/named";
    private final RestTemplate restTemplate;

    public ScryfallService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ScryfallCardResponse getCardByName(String cardName) throws RuntimeException {
        String url = SCRYFALL_API_URL + "?exact=" + encodeCardName(cardName);
        try {
            return restTemplate.getForObject(url, ScryfallCardResponse.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Card not found in Scryfall: " + cardName, e);
        }
    }

    private String encodeCardName(String cardName) {
        return java.net.URLEncoder.encode(cardName, java.nio.charset.StandardCharsets.UTF_8);
    }
}
