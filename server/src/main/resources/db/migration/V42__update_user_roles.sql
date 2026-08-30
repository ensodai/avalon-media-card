-- Обеспечиваем, что в колонке role только валидные значения 'USER' и 'ADMIN'
UPDATE users SET role = 'USER' WHERE role NOT IN ('USER', 'ADMIN');
