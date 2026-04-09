CREATE DATABASE IF NOT EXISTS maxcopias
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;

USE maxcopias;

-- Las tablas se crean automaticamente al arrancar la aplicacion con Hibernate.
-- Si quieres convertir un usuario ya registrado en administrador:
-- UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin@maxcopias.es';
