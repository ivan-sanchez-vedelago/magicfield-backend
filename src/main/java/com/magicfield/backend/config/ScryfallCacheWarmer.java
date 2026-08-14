package com.magicfield.backend.config;

import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.service.ScryfallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Precalienta el caché en memoria de ScryfallService.getCardData apenas termina de arrancar
 * la app, así el primer usuario real que pega contra /api/products, /api/products/newest o
 * /api/products/catalog después de un deploy no paga el costo de un GET a Scryfall (rate-
 * limitado a ~8/s) por cada single sin cachear. Corre en un hilo daemon de background --no
 * bloquea el arranque ni los health checks-- y si algún request real llega antes de que
 * termine, ese scryfallId puntual simplemente se cachea on-demand como siempre, sin romper
 * nada ni duplicar trabajo de forma incorrecta.
 */
@Component
public class ScryfallCacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(ScryfallCacheWarmer.class);

    private final ProductRepository productRepository;
    private final ScryfallService scryfallService;

    public ScryfallCacheWarmer(ProductRepository productRepository, ScryfallService scryfallService) {
        this.productRepository = productRepository;
        this.scryfallService = scryfallService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStartup() {
        Thread warmupThread = new Thread(this::warmUp, "scryfall-cache-warmup");
        warmupThread.setDaemon(true);
        warmupThread.start();
    }

    private void warmUp() {
        List<String> scryfallIds = productRepository.findDistinctScryfallIdsInStock();
        log.info("[ScryfallCacheWarmer] Precalentando caché para {} scryfallIds distintos...", scryfallIds.size());

        int warmed = 0;
        for (String scryfallId : scryfallIds) {
            try {
                scryfallService.getCardData(scryfallId);
                warmed++;
            } catch (Exception e) {
                log.warn("[ScryfallCacheWarmer] Error precalentando scryfallId={}: {}", scryfallId, e.getMessage());
            }
        }

        log.info("[ScryfallCacheWarmer] Listo: {}/{} scryfallIds cacheados.", warmed, scryfallIds.size());
    }
}
