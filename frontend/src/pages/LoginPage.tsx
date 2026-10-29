import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuthStore } from '../store/authStore';

const loginSchema = z.object({
  email: z.string().email('Неверный формат email'),
  password: z.string().min(1, 'Введите пароль'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const loginToStore = useAuthStore((state) => state.login);
  const [serverError, setServerError] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginForm) => {
    try {
      setServerError('');
      // ИМИТАЦИЯ ЗАПРОСА
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Мок: если пароль не "password", выдаем ошибку для проверки
      if (data.password === 'error') {
        throw new Error('Неверный пароль');
      }

      const fakeToken = 'mock-jwt-token-123';
      const fakeUser = { id: '1', username: 'testuser', email: data.email };
      
      loginToStore(fakeToken, fakeUser);
      navigate('/');
    } catch (err: any) {
      setServerError('Неверный email или пароль');
    }
  };

  return (
    <div className="max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow">
      <h1 className="text-2xl font-bold mb-6 text-center">Вход</h1>
      
      {serverError && <p className="text-red-500 text-center mb-4">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Email</label>
          <input type="email" {...register('email')} className="w-full border rounded-lg p-2" />
          {errors.email && <p className="text-red-500 text-sm mt-1">{errors.email.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Пароль</label>
          <input type="password" {...register('password')} className="w-full border rounded-lg p-2" />
          {errors.password && <p className="text-red-500 text-sm mt-1">{errors.password.message}</p>}
        </div>

        <button type="submit" disabled={isSubmitting} className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50">
          {isSubmitting ? 'Загрузка...' : 'Войти'}
        </button>
      </form>
      
      <p className="text-center mt-4 text-sm text-gray-600">
        Нет аккаунта? <Link to="/register" className="text-blue-600 hover:underline">Зарегистрироваться</Link>
      </p>
    </div>
  );
}