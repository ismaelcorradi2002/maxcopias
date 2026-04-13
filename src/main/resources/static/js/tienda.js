// Carrusel y categorías dinámicas - FIXED SYNTAX
document.addEventListener('DOMContentLoaded', function() {
  // CARRUSEL
  const items = document.querySelectorAll('.carousel-item');
  const indicators = document.querySelectorAll('.carousel-indicator');
  let currentIndex = 0;
  let carouselInterval;

  function showSlide(index) {
    items.forEach((item, i) => {
      item.style.opacity = (i === index) ? '1' : '0';
    });
    indicators.forEach((ind, i) => {
      ind.classList.toggle('active', i === index);
      ind.style.background = (i === index) ? 'rgba(255,255,255,1)' : 'rgba(255,255,255,0.4)';
    });
    currentIndex = index;
  }

  function nextSlide() {
    let nextIndex = currentIndex + 1;
    if (nextIndex >= 5) {
      nextIndex = 0;
    }
    showSlide(nextIndex);
  }

  carouselInterval = setInterval(nextSlide, 2000);

  indicators.forEach((indicator, index) => {
    indicator.addEventListener('click', () => {
      clearInterval(carouselInterval);
      showSlide(index);
      carouselInterval = setInterval(nextSlide, 2000);
    });
  });

  const carousel = document.querySelector('.carousel-inner');
  carousel.addEventListener('mouseenter', () => clearInterval(carouselInterval));
  carousel.addEventListener('mouseleave', () => carouselInterval = setInterval(nextSlide, 2000));

  // TODOS LOS PRODUCTOS
  const todosProductos = [
    { category: 'escolar', img: 'https://images.unsplash.com/photo-1581605405669-fcdf81165afa?q=80&w=774&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', alt: '[Escolar1] Mochila escolar reforzada', nombre: 'Mochila escolar', desc: 'Capacidad 25L, múltiples compartimentos.', precio: '€29.99' },
    { category: 'escolar', img: 'https://images.unsplash.com/photo-1567634088512-20ec1da1e1a5?q=80&w=1548&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', alt: '[Escolar2] Estuche doble', nombre: 'Estuche doble', desc: 'Plástico resistente, 2 cremalleras.', precio: '€8.50' },
    { category: 'oficina', img: 'https://multimedia.dideco.es/img/papeleria/EAN_8422951051238-5.jpg', alt: '[Oficina1] Separadores A4', nombre: 'Separadores A4', desc: '12 pestañas, colores variados.', precio: '€5.99' },
    { category: 'oficina', img: 'https://images.unsplash.com/photo-1559743341-7fef133c7c6a?q=80&w=1570&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', alt: '[Oficina2] Grapadora pesada', nombre: 'Grapadora pesada', desc: 'Capacidad 100 hojas, metálica.', precio: '€19.95' },
    { category: 'arte', img: 'https://m.media-amazon.com/images/I/61xNnFt-2QL.jpg', alt: '[Arte1] Lápices acuarela 24u', nombre: 'Lápices acuarela', desc: 'Set profesional 24 colores.', precio: '€22.50' },
    { category: 'arte', img: 'https://plus.unsplash.com/premium_photo-1683309559481-f5b07f07774e?q=80&w=774&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', nombre: 'Bloc dibujo', desc: 'Formato A4, 50h 200gr.', precio: '€12.99' },
    { category: 'organizacion', img: 'https://lasuperpapeleria.com//imagenes_grandes/3130631/313063189560.JPG', alt: '[Organizacion1] Caja archivador', nombre: 'Caja archivador', desc: 'Transfer resistente, 50 documentos.', precio: '€6.75' },
    { category: 'organizacion', img: 'https://moldiber.com/2658-thickbox_default/perfil-auxiliar-de-aluminio-a-medida-modelo-20.webp', alt: '[Organizacion2] Tablero corcho', nombre: 'Tablero corcho', desc: '60x40cm con marcos aluminio.', precio: '€18.90' },
    { category: 'tecnologia', img: 'https://m.media-amazon.com/images/I/717phNvKCVS._AC_UF1000,1000_QL80_.jpg', alt: '[Tecnologia1] Protector teclado', nombre: 'Protector teclado', desc: 'Transparente, funda silicona.', precio: '€9.99' },
    { category: 'tecnologia', img: 'https://m.media-amazon.com/images/I/71YTA1pw9CL.jpg', alt: '[Tecnologia2] Cable organizador', nombre: 'Cable organizador', desc: 'Kit 10 brazaletes velcro.', precio: '€4.25' }
  ];

  const tabs = document.querySelectorAll('.section-tab');
  const container = document.getElementById('productos-container');
  const title = document.getElementById('productos-title');
  let currentCategory = 'todos';

  function showProducts(category) {
    let productsToShow;
    if (category === 'todos') {
      productsToShow = todosProductos;
      title.innerHTML = '<span class="section-kicker">Todos</span><h2>Catálogo completo</h2>';
    } else {
      productsToShow = todosProductos.filter(p => p.category === category).slice(0, 2);
      const catName = category.charAt(0).toUpperCase() + category.slice(1);
      title.innerHTML = `<span class="section-kicker">${catName}</span><h2>Productos ${catName}</h2>`;
    }

    container.innerHTML = productsToShow.map(product => `
      <article class="mini-service-card" style="border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
        <img src="${product.img}" alt="${product.alt}" style="width: 100%; height: 250px; object-fit: cover;">
        <div style="padding: 1.5rem;">
          <h3>${product.nombre}</h3>
          <p>${product.desc}</p>
          <span style="color: #27ae60; font-weight: 700; font-size: 1.2rem;">${product.precio}</span>
        </div>
      </article>
    `).join('');
  }

  tabs.forEach(tab => {
    tab.addEventListener('click', function() {
      const newCategory = this.dataset.category;
      
      // Toggle: si misma categoría activa → todos
      if (currentCategory === newCategory) {
        currentCategory = 'todos';
        // Resetear todos los botones a colores originales
        tabs.forEach(t => {
          const cat = t.dataset.category;
          t.classList.remove('active');
          t.style.background = cat === 'escolar' ? '#3498db' : 
                              cat === 'oficina' ? '#e74c3c' : 
                              cat === 'arte' ? '#9b59b6' : 
                              cat === 'organizacion' ? '#f39c12' : '#2ecc71';
          t.style.color = 'white';
          t.style.transform = 'scale(1)';
        });
      } else {
        currentCategory = newCategory;
        // Reset todos, activa el clickeado oscuro
        tabs.forEach(t => {
          const cat = t.dataset.category;
          t.classList.remove('active');
          t.style.background = cat === 'escolar' ? '#3498db' : 
                              cat === 'oficina' ? '#e74c3c' : 
                              cat === 'arte' ? '#9b59b6' : 
                              cat === 'organizacion' ? '#f39c12' : '#2ecc71';
          t.style.color = 'white';
          t.style.transform = 'scale(1)';
        });
        this.classList.add('active');
        this.style.background = '#2c3e50'; // Oscuro
        this.style.transform = 'scale(1.05)';
      }
      
      showProducts(currentCategory);
    });
  });

  // Default: mostrar todos
  showProducts('todos');
});

