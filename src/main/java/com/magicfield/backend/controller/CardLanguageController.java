package com.magicfield.backend.controller;

import com.magicfield.backend.dto.CardLanguageResponse;
import com.magicfield.backend.repository.CardLanguageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/languages")
public class CardLanguageController {

    private final CardLanguageRepository cardLanguageRepository;

    public CardLanguageController(CardLanguageRepository cardLanguageRepository) {
        this.cardLanguageRepository = cardLanguageRepository;
    }

    @GetMapping
    public List<CardLanguageResponse> list() {
        return cardLanguageRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(l -> new CardLanguageResponse(l.getId(), l.getShortName(), l.getLongName()))
                .collect(Collectors.toList());
    }
}
