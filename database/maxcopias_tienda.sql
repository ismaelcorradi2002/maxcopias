CREATE DATABASE IF NOT EXISTS maxcopias
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE maxcopias;

CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(600) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categorias_nombre UNIQUE (nombre)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(140) NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    stock INT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_productos_nombre UNIQUE (nombre)
);

CREATE TABLE IF NOT EXISTS producto_categoria (
    producto_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    PRIMARY KEY (producto_id, categoria_id),
    CONSTRAINT fk_producto_categoria_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_producto_categoria_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id)
        ON DELETE CASCADE
);

INSERT IGNORE INTO categorias (nombre, descripcion) VALUES
    ('Escolar', 'Material habitual para clase, mochila y estudio.'),
    ('Oficina', 'Productos de escritorio y trabajo diario.'),
    ('Arte', 'Material creativo para dibujo, ilustracion y manualidades.'),
    ('Organizacion', 'Soluciones para archivar, ordenar y planificar.'),
    ('Tecnologia', 'Accesorios utiles para equipos y escritorio digital.');

INSERT IGNORE INTO productos (nombre, descripcion, stock, precio) VALUES
    ('Mochila escolar', 'Capacidad 25L, multiples compartimentos.', 18, 29.99),
    ('Estuche doble', 'Plastico resistente, 2 cremalleras.', 30, 8.50),
    ('Separadores A4', '12 pestanas, colores variados.', 45, 5.99),
    ('Grapadora pesada', 'Capacidad 100 hojas, metalica.', 12, 19.95),
    ('Lapices acuarela', 'Set profesional 24 colores.', 16, 22.50),
    ('Bloc dibujo', 'Formato A4, 50h 200gr.', 22, 12.99),
    ('Caja archivador', 'Transfer resistente, 50 documentos.', 20, 6.75),
    ('Tablero corcho', '60x40cm con marcos aluminio.', 9, 18.90),
    ('Protector teclado', 'Transparente, funda silicona.', 28, 9.99),
    ('Cable organizador', 'Kit 10 brazaletes velcro.', 40, 4.25);

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Escolar'
WHERE p.nombre = 'Mochila escolar';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Escolar'
WHERE p.nombre = 'Estuche doble';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Oficina'
WHERE p.nombre = 'Separadores A4';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Organizacion'
WHERE p.nombre = 'Separadores A4';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Oficina'
WHERE p.nombre = 'Grapadora pesada';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Arte'
WHERE p.nombre = 'Lapices acuarela';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Escolar'
WHERE p.nombre = 'Lapices acuarela';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Arte'
WHERE p.nombre = 'Bloc dibujo';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Escolar'
WHERE p.nombre = 'Bloc dibujo';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Organizacion'
WHERE p.nombre = 'Caja archivador';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Oficina'
WHERE p.nombre = 'Caja archivador';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Organizacion'
WHERE p.nombre = 'Tablero corcho';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Tecnologia'
WHERE p.nombre = 'Protector teclado';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Tecnologia'
WHERE p.nombre = 'Cable organizador';

INSERT IGNORE INTO producto_categoria (producto_id, categoria_id)
SELECT p.id, c.id
FROM productos p
JOIN categorias c ON c.nombre = 'Oficina'
WHERE p.nombre = 'Cable organizador';
