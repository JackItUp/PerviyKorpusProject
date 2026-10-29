import type { Deck, Card } from '../types';

export const mockDecks: Deck[] = [
  { id: '1', name: 'Математика', description: 'Подготовка к экзамену', cardsCount: 3 },
  { id: '2', name: 'История', description: 'Даты и события', cardsCount: 0 },
];

export const mockCards: Card[] = [
  { 
    id: '1', 
    deckId: '1', 
    type: 'text', 
    question: 'Что такое производная?', 
    answer: 'Скорость изменения функции' 
  },
  { 
    id: '2', 
    deckId: '1', 
    type: 'multiple_choice', 
    question: 'Чему равно число Пи?', 
    options: [
      { id: 'o1', text: '3.14', isCorrect: true },
      { id: 'o2', text: '2.71', isCorrect: false },
      { id: 'o3', text: '1.41', isCorrect: false }
    ]
  },
  { 
    id: '3', 
    deckId: '1', 
    type: 'input', 
    question: 'Как называется наука о числах?', 
    answer: 'Математика' 
  }
];