package com.magicfield.backend.controller;

import com.magicfield.backend.dto.CardFinishResponse;
import com.magicfield.backend.repository.CardFinishRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finishes")
public class CardFinishController {

    private final CardFinishRepository cardFinishRepository;

    public CardFinishController(CardFinishRepository cardFinishRepository) {
        this.cardFinishRepository = cardFinishRepository;
    }

    @GetMapping
    public List<CardFinishResponse> list() {
        return cardFinishRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(f -> new CardFinishResponse(f.getId(), f.getShortName(), f.getLongName()))
                .collect(Collectors.toList());
    }
}
