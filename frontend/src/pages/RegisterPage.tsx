import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuthStore } from '../store/authStore';

const registerSchema = z.object({
  username: z.string().min(3, 'Минимум 3 символа').max(20, 'Максимум 20 символов'),
  email: z.string().email('Неверный формат email'),
  password: z.string().min(6, 'Минимум 6 символов'),
});

type RegisterForm = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const navigate = useNavigate();
  const loginToStore = useAuthStore((state) => state.login);
  const [serverError, setServerError] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterForm) => {
    try {
      setServerError('');
      // ИМИТАЦИЯ ЗАПРОСА К БЭКЕНДУ (потом заменить!!!!)
      // const res = await axios.post('/auth/register', data)
      
      // Временная заглушка (мок):
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Представим, что сервер вернул токен и юзера
      const fakeToken = 'mock-jwt-token-123';
      const fakeUser = { id: '1', username: data.username, email: data.email };
      
      loginToStore(fakeToken, fakeUser);
      navigate('/'); // Перекидываем на главную
    } catch (err: any) {
      setServerError(err.response?.data?.message || 'Ошибка при регистрации');
    }
  };

  return (
    <div className="max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow">
      <h1 className="text-2xl font-bold mb-6 text-center">Регистрация</h1>
      
      {serverError && <p className="text-red-500 text-center mb-4">{serverError}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Username</label>
          <input {...register('username')} className="w-full border rounded-lg p-2" />
          {errors.username && <p className="text-red-500 text-sm mt-1">{errors.username.message}</p>}
        </div>

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
          {isSubmitting ? 'Загрузка...' : 'Зарегистрироваться'}
        </button>
      </form>
      
      <p className="text-center mt-4 text-sm text-gray-600">
        Уже есть аккаунт? <Link to="/login" className="text-blue-600 hover:underline">Войти</Link>
      </p>
    </div>
  );
}