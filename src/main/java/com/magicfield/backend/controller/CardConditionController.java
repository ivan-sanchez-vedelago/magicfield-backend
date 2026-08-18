package com.magicfield.backend.controller;

import com.magicfield.backend.dto.CardConditionResponse;
import com.magicfield.backend.entity.CardCondition;
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

    // Sin ?applicableType, devuelve todo (compatibilidad hacia atrás). Con él ("SIN"/"PSL"),
    // filtra a las condiciones válidas para ese tipo de producto -- NM/LP/... no tiene sentido
    // ofrecerlas para sellados, ni NEW/USD para singles.
    @GetMapping
    public List<CardConditionResponse> list(@RequestParam(required = false) String applicableType) {
        List<CardCondition> conditions =
                (applicableType == null || applicableType.isBlank())
                        ? cardConditionRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                        : cardConditionRepository.findByApplicableType(applicableType.toUpperCase());

        return conditions.stream()
                .map(c -> new CardConditionResponse(c.getId(), c.getShortName(), c.getLongName(), c.getPriceMultiplier()))
                .collect(Collectors.toList());
    }
}
