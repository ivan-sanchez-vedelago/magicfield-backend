package com.magicfield.backend.controller;

import com.magicfield.backend.dto.CardConditionResponse;
import com.magicfield.backend.repository.CardConditionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conditions")
public class CardConditionController {

    private final CardConditionRepository cardConditionRepository;

    public CardConditionController(CardConditionRepository cardConditionRepository) {
        this.cardConditionRepository = cardConditionRepository;
    }

    @GetMapping
    public List<CardConditionResponse> list() {
        return cardConditionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(c -> new CardConditionResponse(c.getId(), c.getShortName(), c.getLongName(), c.getPriceMultiplier()))
                .collect(Collectors.toList());
    }
}
