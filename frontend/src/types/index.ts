export interface Group {
  id: string;
  name: string;
}

export interface Deck {
  id: string;
  name: string;
  description: string;
  cardsCount: number;
  groupId?: string; // ID группы, к которой принадлежит колода
}

export type CardType = 'text' | 'multiple_choice' | 'input';

export interface Card {
  id: string;
  deckId: string;
  type: CardType;
  question: string;
  answer?: string;
  options?: { id: string; text: string; isCorrect: boolean }[];
}