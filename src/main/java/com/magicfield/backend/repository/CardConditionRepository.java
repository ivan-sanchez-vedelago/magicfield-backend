package com.magicfield.backend.repository;

import com.magicfield.backend.entity.CardCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardConditionRepository extends JpaRepository<CardCondition, Long> {

    List<CardCondition> findByApplicableType(String applicableType);
}
