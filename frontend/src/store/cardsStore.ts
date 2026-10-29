import { create } from 'zustand';
import type { Card } from '../types';
import { mockCards } from '../api/mockData';

interface CardsState {
  cards: Card[];
  addCard: (card: Card) => void;
}

export const useCardsStore = create<CardsState>((set) => ({
  cards: mockCards, // Изначально берем из моков
  addCard: (card) => set((state) => ({ cards: [...state.cards, card] })),
}));