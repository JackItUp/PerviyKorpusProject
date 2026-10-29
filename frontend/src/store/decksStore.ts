import { create } from 'zustand';
import type { Deck, Group } from '../types';
import { mockDecks } from '../api/mockData';

interface DecksState {
  decks: Deck[];
  groups: Group[];
  addDeck: (deck: Deck) => void;
  addGroup: (group: Group) => void;
  moveDeckToGroup: (deckId: string, groupId?: string) => void;
}

export const useDecksStore = create<DecksState>((set) => ({
  decks: mockDecks,
  groups: [], // Изначально групп нет
  addDeck: (deck) => set((state) => ({ decks: [...state.decks, deck] })),
  addGroup: (group) => set((state) => ({ groups: [...state.groups, group] })),
  moveDeckToGroup: (deckId, groupId) => set((state) => ({
    decks: state.decks.map(deck => 
      deck.id === deckId ? { ...deck, groupId } : deck
    )
  })),
}));