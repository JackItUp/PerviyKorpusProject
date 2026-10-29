import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCardsStore } from '../store/cardsStore';
import { useDecksStore } from '../store/decksStore';
import { Plus, X, FolderPlus } from 'lucide-react';
import type { Deck, Group } from '../types';

const deckSchema = z.object({
  name: z.string().min(1, 'Название обязательно').max(50, 'Максимум 50 символов'),
  description: z.string().max(200, 'Максимум 200 символов').optional(),
});

const groupSchema = z.object({
  name: z.string().min(1, 'Название группы обязательно').max(30, 'Максимум 30 символов'),
});

type DeckForm = z.infer<typeof deckSchema>;
type GroupForm = z.infer<typeof groupSchema>;

export default function DecksPage() {
  const cards = useCardsStore((state) => state.cards);
  const { decks, groups, addDeck, addGroup, moveDeckToGroup } = useDecksStore();
  
  const [isDeckModalOpen, setIsDeckModalOpen] = useState(false);
  const [isGroupModalOpen, setIsGroupModalOpen] = useState(false);

  // Считаем количество карточек
  const decksWithRealCounts = decks.map(deck => ({
    ...deck,
    cardsCount: cards.filter(c => c.deckId === deck.id).length
  }));

  // Формы
  const deckForm = useForm<DeckForm>({ resolver: zodResolver(deckSchema) });
  const groupForm = useForm<GroupForm>({ resolver: zodResolver(groupSchema) });

  const onCreateDeck = (data: DeckForm) => {
    const newDeck: Deck = {
      id: Date.now().toString(),
      name: data.name,
      description: data.description || '',
      cardsCount: 0,
    };
    addDeck(newDeck);
    deckForm.reset();
    setIsDeckModalOpen(false);
  };

  const onCreateGroup = (data: GroupForm) => {
    const newGroup: Group = {
      id: Date.now().toString(),
      name: data.name,
    };
    addGroup(newGroup);
    groupForm.reset();
    setIsGroupModalOpen(false);
  };

  // Компонент карточки колоды (вынесем отдельно для чистоты)
  const renderDeck = (deck: Deck) => (
    <div key={deck.id} className="bg-white rounded-lg shadow hover:shadow-md transition flex flex-col justify-between">
      <Link to={`/decks/${deck.id}`} className="p-4 block">
        <h3 className="font-semibold text-lg">{deck.name}</h3>
        <p className="text-gray-500 text-sm mb-2">{deck.description}</p>
        <p className="text-blue-600 text-sm">{deck.cardsCount} карточек</p>
      </Link>
      
      {/* Выпадающий список для переноса в группу */}
      <div className="px-4 pb-4 mt-auto">
        <select
          value={deck.groupId || ''}
          onChange={(e) => moveDeckToGroup(deck.id, e.target.value || undefined)}
          className="w-full text-xs border rounded p-1 text-gray-600 bg-gray-50 focus:outline-none"
        >
          <option value="">Без группы</option>
          {groups.map(g => (
            <option key={g.id} value={g.id}>{g.name}</option>
          ))}
        </select>
      </div>
    </div>
  );

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Мои колоды</h1>
        <div className="flex gap-2">
          <button
            onClick={() => setIsGroupModalOpen(true)}
            className="bg-gray-100 text-gray-700 px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-gray-200"
          >
            <FolderPlus size={18} /> Группа
          </button>
          <button
            onClick={() => setIsDeckModalOpen(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700"
          >
            <Plus size={18} /> Создать колоду
          </button>
        </div>
      </div>

      {decksWithRealCounts.length === 0 && groups.length === 0 ? (
        <p className="text-gray-500 text-center py-10">У вас пока ничего нет.</p>
      ) : (
        <div className="space-y-8">
          {/* Вывод по группам */}
          {groups.map(group => {
            const groupDecks = decksWithRealCounts.filter(d => d.groupId === group.id);
            return (
              <div key={group.id} className="bg-gray-100/50 p-4 rounded-xl border border-gray-200">
                <h2 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                  <span className="w-2 h-6 bg-blue-500 rounded-full inline-block"></span>
                  {group.name}
                </h2>
                {groupDecks.length === 0 ? (
                  <p className="text-sm text-gray-500 italic">В этой группе пока нет колод.</p>
                ) : (
                  <div className="grid gap-4 md:grid-cols-2">
                    {groupDecks.map(renderDeck)}
                  </div>
                )}
              </div>
            );
          })}

          {/* Вывод колод без группы */}
          {decksWithRealCounts.filter(d => !d.groupId).length > 0 && (
            <div>
              <h2 className="text-xl font-bold text-gray-800 mb-4">Без группы</h2>
              <div className="grid gap-4 md:grid-cols-2">
                {decksWithRealCounts.filter(d => !d.groupId).map(renderDeck)}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Модалка создания КОЛОДЫ */}
      {isDeckModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Новая колода</h2>
              <button onClick={() => setIsDeckModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={deckForm.handleSubmit(onCreateDeck)} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Название *</label>
                <input {...deckForm.register('name')} className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500 focus:outline-none" />
                {deckForm.formState.errors.name && <p className="text-red-500 text-sm mt-1">{deckForm.formState.errors.name.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Описание</label>
                <textarea {...deckForm.register('description')} className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500 focus:outline-none" rows={2} />
              </div>
              <div className="flex gap-2 justify-end">
                <button type="button" onClick={() => setIsDeckModalOpen(false)} className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg">Отмена</button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">Создать</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Модалка создания ГРУППЫ */}
      {isGroupModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Новая группа</h2>
              <button onClick={() => setIsGroupModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={groupForm.handleSubmit(onCreateGroup)} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Название группы *</label>
                <input {...groupForm.register('name')} placeholder="Например: Математика" className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500 focus:outline-none" />
                {groupForm.formState.errors.name && <p className="text-red-500 text-sm mt-1">{groupForm.formState.errors.name.message}</p>}
              </div>
              <div className="flex gap-2 justify-end">
                <button type="button" onClick={() => setIsGroupModalOpen(false)} className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg">Отмена</button>
                <button type="submit" className="px-4 py-2 bg-gray-800 text-white rounded-lg hover:bg-gray-900">Создать</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}