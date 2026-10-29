import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCardsStore } from '../store/cardsStore';
import { ArrowLeft, Plus, X, Trash2 } from 'lucide-react';
import type { CardType } from '../types';

// Убрали .min(1) для text, чтобы пустые поля не блокировали отправку при других типах
const cardSchema = z.object({
  question: z.string().min(1, 'Вопрос обязателен').max(500, 'Максимум 500 символов'),
  answer: z.string().optional(),
  options: z.array(
    z.object({
      text: z.string(),
      isCorrect: z.boolean(),
    })
  ).optional(),
});

type CardForm = z.infer<typeof cardSchema>;

export default function DeckDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { cards, addCard } = useCardsStore();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [cardType, setCardType] = useState<CardType>('text');

  const deckCards = cards.filter((c) => c.deckId === id);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<CardForm>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      options: [{ text: '', isCorrect: false }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'options',
  });

  const onSubmit = (data: CardForm) => {
    // 1. Проверка для текста и ввода
    if ((cardType === 'text' || cardType === 'input') && !data.answer) {
      alert('Введите правильный ответ');
      return;
    }

    // 2. Проверка для вариантов
    let validOptions = data.options;
    if (cardType === 'multiple_choice') {
      // Оставляем только те варианты, где есть текст
      validOptions = data.options?.filter(o => o.text.trim() !== '');
      
      if (!validOptions || validOptions.length < 2) {
        alert('Добавьте хотя бы два варианта ответа');
        return;
      }
      if (!validOptions.some(o => o.isCorrect)) {
        alert('Выберите хотя бы один правильный вариант');
        return;
      }
    }

    const newCard = {
      id: Date.now().toString(),
      deckId: id!,
      type: cardType,
      question: data.question,
      answer: cardType !== 'multiple_choice' ? data.answer : undefined,
      options: cardType === 'multiple_choice' 
        ? validOptions?.map((o, i) => ({ id: `opt_${i}_${Date.now()}`, text: o.text, isCorrect: o.isCorrect })) 
        : undefined,
    };

    addCard(newCard);
    closeModal();
  };

  const closeModal = () => {
    reset();
    setCardType('text');
    setIsModalOpen(false);
  };

  return (
    <div>
      <Link to="/" className="text-blue-600 flex items-center gap-1 mb-4 hover:underline">
        <ArrowLeft size={16} /> Назад к колодам
      </Link>

      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Карточки колоды</h1>
        <div className="flex gap-2">
          <Link to={`/decks/${id}/study`} className="bg-green-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-green-700">
            Учить колоду
          </Link>
          <button onClick={() => setIsModalOpen(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700">
            <Plus size={18} /> Добавить
          </button>
        </div>
      </div>

      {deckCards.length === 0 ? (
        <p className="text-gray-500 text-center py-10">В этой колоде пока нет карточек.</p>
      ) : (
        <div className="space-y-3">
          {deckCards.map((card) => (
            <div key={card.id} className="p-4 bg-white rounded-lg shadow border border-gray-100">
              <span className="text-xs font-bold text-blue-500 uppercase tracking-wider mb-2 block">
                {card.type === 'text' ? 'Текст' : card.type === 'multiple_choice' ? 'Варианты' : 'Ввод текста'}
              </span>
              <p className="font-medium text-gray-800">{card.question}</p>
              {card.type !== 'multiple_choice' && <p className="text-gray-500 mt-1 text-sm">Ответ: {card.answer}</p>}
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 overflow-y-auto">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg my-8">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Новая карточка</h2>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">Тип карточки</label>
              <select
                value={cardType}
                onChange={(e) => setCardType(e.target.value as CardType)}
                className="w-full border rounded-lg p-2 bg-gray-50"
              >
                <option value="text">Обычная (Вопрос-Ответ)</option>
                <option value="multiple_choice">С вариантами ответа</option>
                <option value="input">С вводом текста</option>
              </select>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Вопрос *</label>
                <textarea
                  {...register('question')}
                  className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  rows={2}
                />
                {errors.question && <p className="text-red-500 text-sm mt-1">{errors.question.message}</p>}
              </div>

              {(cardType === 'text' || cardType === 'input') && (
                <div>
                  <label className="block text-sm font-medium mb-1">Правильный ответ *</label>
                  <textarea
                    {...register('answer')}
                    className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    rows={2}
                  />
                </div>
              )}

              {cardType === 'multiple_choice' && (
                <div className="space-y-3">
                  <label className="block text-sm font-medium">Варианты ответа (отметьте верные)</label>
                  {fields.map((field, index) => (
                    <div key={field.id} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        {...register(`options.${index}.isCorrect`)}
                        className="w-5 h-5 text-green-600 rounded"
                      />
                      <input
                        {...register(`options.${index}.text`)}
                        placeholder={`Вариант ${index + 1}`}
                        className="flex-1 border rounded-lg p-2"
                      />
                      <button type="button" onClick={() => remove(index)} className="text-red-500 hover:bg-red-50 p-2 rounded">
                        <Trash2 size={18} />
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    onClick={() => append({ text: '', isCorrect: false })}
                    className="text-sm text-blue-600 flex items-center gap-1 hover:underline mt-2"
                  >
                    <Plus size={14} /> Добавить вариант
                  </button>
                </div>
              )}

              <div className="flex gap-2 justify-end pt-4 border-t mt-4">
                <button type="button" onClick={closeModal} className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg">
                  Отмена
                </button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                  Сохранить
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}