import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
//import { mockCards } from '../api/mockData';
import { ArrowLeft } from 'lucide-react';
//import type { Card } from '../types';
import { useCardsStore } from '../store/cardsStore';

export default function StudyPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const allCards = useCardsStore((state) => state.cards);
    const cards = allCards.filter((c) => c.deckId === id);

  
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  
  // Состояния для ответов
  const [selectedOptions, setSelectedOptions] = useState<string[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isAnswered, setIsAnswered] = useState(false);
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null);

  // Состояние для подсчета правильных ответов
  const [score, setScore] = useState(0);

  if (cards.length === 0) {
    return <div className="text-center py-10">В колоде нет карточек. <Link to="/" className="text-blue-600">Вернуться</Link></div>;
  }

  // Экран окончания колоды
  if (currentIndex >= cards.length) {
    return (
      <div className="text-center py-10 bg-white p-8 rounded-xl shadow-lg border border-gray-100 max-w-lg mx-auto mt-10">
        <h2 className="text-2xl font-bold mb-4">Колода пройдена!</h2>
        <p className="text-xl mb-6">
          Правильных ответов: <span className="font-bold text-green-600">{score}</span> из {cards.length}
        </p>
        <div className="flex gap-4 justify-center">
          <button 
            onClick={() => {
              setCurrentIndex(0);
              setScore(0);
              setIsFlipped(false);
              setSelectedOptions([]);
              setInputValue('');
              setIsAnswered(false);
              setIsCorrect(null);
            }} 
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
          >
            Пройти еще раз
          </button>
          <button 
            onClick={() => navigate('/')} 
            className="bg-gray-200 text-gray-800 px-6 py-2 rounded-lg hover:bg-gray-300"
          >
            Вернуться к колодам
          </button>
        </div>
      </div>
    );
  }

  const currentCard = cards[currentIndex];

  const handleNext = () => {
    setCurrentIndex(prev => prev + 1);
    setIsFlipped(false);
    setSelectedOptions([]);
    setInputValue('');
    setIsAnswered(false);
    setIsCorrect(null);
  };

  // Проверка для обычной карточки (самооценка)
  const handleTextAnswer = (knowsIt: boolean) => {
    if (knowsIt) {
      setScore(prev => prev + 1);
    }
    handleNext();
  };

  // Проверка для вариантов ответа
  const checkMultipleChoice = () => {
    const correctOptions = currentCard.options?.filter(o => o.isCorrect).map(o => o.id) || [];
    const isAllCorrect = correctOptions.length === selectedOptions.length && 
                         correctOptions.every(id => selectedOptions.includes(id));
    setIsCorrect(isAllCorrect);
    
    // Если ответ верный, добавляем балл
    if (isAllCorrect) setScore(prev => prev + 1);
    
    setIsAnswered(true);
  };

  // Проверка для ввода текста
  const checkInput = () => {
    const normalize = (str: string) => str.trim().toLowerCase();
    const correct = normalize(currentCard.answer || '') === normalize(inputValue);
    setIsCorrect(correct);
    
    // Если ответ верный, добавляем балл
    if (correct) setScore(prev => prev + 1);
    
    setIsAnswered(true);
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <Link to={`/decks/${id}`} className="text-blue-600 flex items-center gap-1 hover:underline">
          <ArrowLeft size={16} /> Назад к карточкам
        </Link>
        <span className="text-gray-500 font-medium">
          {currentIndex + 1} / {cards.length}
        </span>
      </div>

      {/* Карточка */}
      <div className="bg-white p-8 rounded-xl shadow-lg border border-gray-100 min-h-[300px] flex flex-col justify-center">
        <h2 className="text-xl font-semibold mb-6 text-center">{currentCard.question}</h2>

        {/* Тип: Обычный текст (Вопрос-Ответ) */}
        {currentCard.type === 'text' && (
          <div className="text-center">
            {!isFlipped ? (
              <button onClick={() => setIsFlipped(true)} className="bg-blue-100 text-blue-700 px-6 py-2 rounded-lg font-medium hover:bg-blue-200">
                Показать ответ
              </button>
            ) : (
              <div>
                <p className="text-lg text-green-700 bg-green-50 p-4 rounded-lg mb-6">{currentCard.answer}</p>
                <div className="flex gap-4 justify-center">
                  <button 
                    onClick={() => handleTextAnswer(false)} 
                    className="bg-red-100 text-red-700 px-6 py-2 rounded-lg font-medium hover:bg-red-200"
                  >
                    Не знаю
                  </button>
                  <button 
                    onClick={() => handleTextAnswer(true)} 
                    className="bg-green-100 text-green-700 px-6 py-2 rounded-lg font-medium hover:bg-green-200"
                  >
                    Знаю
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Тип: Варианты ответа */}
        {currentCard.type === 'multiple_choice' && (
          <div className="space-y-3">
            {currentCard.options?.map(option => {
              // Логика подсветки вариантов после ответа
              let itemClass = "flex items-center p-3 border rounded-lg cursor-pointer hover:bg-gray-50";
              
              if (isAnswered) {
                if (option.isCorrect) {
                  // Правильный ответ всегда зеленый
                  itemClass = "flex items-center p-3 border-2 border-green-500 bg-green-50 rounded-lg";
                } else if (selectedOptions.includes(option.id)) {
                  // Неправильно выбранный ответ красный
                  itemClass = "flex items-center p-3 border-2 border-red-500 bg-red-50 rounded-lg";
                } else {
                  // Остальные (невыбранные и неправильные) просто бледные
                  itemClass = "flex items-center p-3 border rounded-lg opacity-50 cursor-not-allowed";
                }
              }

              return (
                <label key={option.id} className={itemClass}>
                  <input
                    type="checkbox"
                    disabled={isAnswered}
                    checked={selectedOptions.includes(option.id)}
                    onChange={(e) => {
                      if (e.target.checked) setSelectedOptions([...selectedOptions, option.id]);
                      else setSelectedOptions(selectedOptions.filter(id => id !== option.id));
                    }}
                    className="w-5 h-5 text-blue-600 rounded mr-3"
                  />
                  <span>{option.text}</span>
                </label>
              );
            })}
            
            {!isAnswered ? (
              <button onClick={checkMultipleChoice} className="w-full mt-4 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700">Ответить</button>
            ) : (
              <div className="text-center mt-4">
                <p className={`font-bold mb-4 ${isCorrect ? 'text-green-600' : 'text-red-600'}`}>
                  {isCorrect ? 'Правильно!' : 'Ошибка!'}
                </p>
                <button onClick={handleNext} className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">Дальше</button>
              </div>
            )}
          </div>
        )}

        {/* Тип: Ввод ответа */}
        {currentCard.type === 'input' && (
          <div className="space-y-3">
            <input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              disabled={isAnswered}
              placeholder="Введите ваш ответ..."
              className="w-full border-2 border-gray-300 rounded-lg p-3 text-lg focus:border-blue-500 focus:outline-none"
            />
            {!isAnswered ? (
              <button onClick={checkInput} className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700">Проверить</button>
            ) : (
              <div className="text-center mt-4">
                <p className={`font-bold mb-2 ${isCorrect ? 'text-green-600' : 'text-red-600'}`}>
                  {isCorrect ? 'Правильно!' : 'Ошибка!'}
                </p>
                <p className="text-gray-600 mb-4">Правильный ответ: <span className="font-medium text-black">{currentCard.answer}</span></p>
                <button onClick={handleNext} className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">Дальше</button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}