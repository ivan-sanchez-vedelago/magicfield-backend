package com.magicfield.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DollarService {

    private final RestTemplate restTemplate;

    public DollarService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private BigDecimal cachedRate;
    private LocalDateTime lastUpdate;

    public BigDecimal getRate() {
        if (cachedRate == null || lastUpdate == null ||
            lastUpdate.isBefore(LocalDateTime.now().minusHours(6))) {

            updateRate();
        }

        return cachedRate;
    }

    private void updateRate() {
        try {
            String url = "https://criptoya.com/api/dolar";
            System.out.println("Consultando API dolar...");

            Map response = restTemplate.getForObject(url, Map.class);

            BigDecimal mep = extractPrice(response, "mep");
            if (mep != null) {
                cachedRate = mep;
                lastUpdate = LocalDateTime.now();
                return;
            }

            BigDecimal blue = extractPrice(response, "blue");
            if (blue != null) {
                cachedRate = blue;
                lastUpdate = LocalDateTime.now();
                return;
            }
            throw new RuntimeException("No se pudo obtener MEP ni BLUE");
        } catch (Exception e) {
            e.printStackTrace();
            if (cachedRate == null) {
                throw new RuntimeException("Error obteniendo tipo de cambio", e);
            }
            System.out.println("Usando valor cacheado: " + cachedRate);
            lastUpdate = LocalDateTime.now();
        }
    }

    private BigDecimal extractPrice(Map response, String key) {
        try {
            if (response == null || response.get(key) == null) return null;

            Map data = (Map) response.get(key);
            Object askObj = data.get("ask");

            if (askObj == null) return null;

            double value = Double.parseDouble(askObj.toString());

            if (value <= 0) return null;

            return BigDecimal.valueOf(value);

        } catch (Exception e) {
            return null;
        }
    }

    @Scheduled(fixedRate = 1000 * 60 * 60 * 6) // cada 6 horas
    public void refreshRate() {
        updateRate();
    }
}