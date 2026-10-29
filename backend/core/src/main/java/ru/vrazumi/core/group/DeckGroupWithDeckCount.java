package ru.vrazumi.core.group;

/** Группа с количеством колод: собирается одним запросом, без N+1. */
public record DeckGroupWithDeckCount(DeckGroup group, Long deckCount) {}
