package ru.vrazumi.core.deck;

/** Проекция колоды с количеством карточек: собирается одним запросом, без N+1. */
public record DeckWithCardCount(Deck deck, Long cardCount) {}
