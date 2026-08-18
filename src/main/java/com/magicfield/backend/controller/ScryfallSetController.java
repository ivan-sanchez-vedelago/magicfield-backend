package com.magicfield.backend.controller;

import com.magicfield.backend.dto.ScryfallSetIconResponse;
import com.magicfield.backend.dto.ScryfallSetResponse;
import com.magicfield.backend.service.ScryfallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Sets curados de Scryfall para el picker de "set" de productos sellados -- ver
// ScryfallService.getCuratedSets/getSetIconSvg.
@RestController
@RequestMapping("/api/scryfall/sets")
public class ScryfallSetController {

    private final ScryfallService scryfallService;

    public ScryfallSetController(ScryfallService scryfallService) {
        this.scryfallService = scryfallService;
    }

    @GetMapping
    public ResponseEntity<List<ScryfallSetResponse>> list() {
        List<ScryfallSetResponse> sets = scryfallService.getCuratedSets().stream()
                .map(s -> new ScryfallSetResponse(s.code(), s.name(), s.iconSvgUri()))
                .toList();
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=3600, stale-while-revalidate=86400")
                .body(sets);
    }

    // Símbolo del set (SVG crudo, normalizado a currentColor), bajo demanda -- no viene en la
    // lista de arriba, ver ScryfallService.getSetIconSvg.
    @GetMapping("/{code}/icon")
    public ResponseEntity<ScryfallSetIconResponse> icon(@PathVariable String code) {
        return scryfallService.getCuratedSets().stream()
                .filter(s -> s.code().equalsIgnoreCase(code))
                .findFirst()
                .map(s -> {
                    String svg = scryfallService.getSetIconSvg(s.code(), s.iconSvgUri());
                    return svg != null
                            ? ResponseEntity.ok(new ScryfallSetIconResponse(svg))
                            : ResponseEntity.notFound().<ScryfallSetIconResponse>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Refresco manual del caché de sets -- para cuando sale un set nuevo y no se quiere
    // esperar a que la app se reinicie sola (ver ScryfallService.refreshCuratedSetsCache).
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh() {
        scryfallService.refreshCuratedSetsCache();
        return ResponseEntity.noContent().build();
    }
}
