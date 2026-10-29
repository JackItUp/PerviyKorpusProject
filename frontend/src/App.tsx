import { BrowserRouter, Routes, Route, Link, Navigate, useNavigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from './store/authStore';
import DecksPage from './pages/DecksPage';
import DeckDetailsPage from './pages/DeckDetailsPage';
import RegisterPage from './pages/RegisterPage';
import LoginPage from './pages/LoginPage';
import StudyPage from './pages/StudyPage';

const queryClient = new QueryClient();

// Компонент для защиты маршрутов
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function Layout({ children }: { children: React.ReactNode }) {
  const { token, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm p-4 flex justify-between items-center">
        <Link to="/" className="text-xl font-bold text-blue-600">
          Вразуми
        </Link>
        <nav className="flex items-center gap-4">
          {token ? (
            <>
              <Link to="/" className="text-gray-600 hover:text-blue-600">
                Мои колоды
              </Link>
              <button onClick={handleLogout} className="text-sm text-red-500 hover:underline">
                Выйти
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-gray-600 hover:text-blue-600">Вход</Link>
              <Link to="/register" className="text-blue-600 font-medium hover:underline">Регистрация</Link>
            </>
          )}
        </nav>
      </header>
      <main className="max-w-4xl mx-auto p-4">{children}</main>
    </div>
  );
}


export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Layout>
          <Routes>
            {/* Публичные маршруты */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            
            {/* Защищенные маршруты */}
            <Route path="/" element={
              <ProtectedRoute>
                <DecksPage />
              </ProtectedRoute>
            } />
            
            <Route path="/decks/:id" element={
              <ProtectedRoute>
                <DeckDetailsPage />
              </ProtectedRoute>
            } />

            {/* Новый маршрут для учебной сессии */}
            <Route path="/decks/:id/study" element={
              <ProtectedRoute>
                <StudyPage />
              </ProtectedRoute>
            } />
          </Routes>
        </Layout>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
