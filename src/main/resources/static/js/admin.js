function switchTab(button, tab) {
    // Update active button
    document.querySelectorAll('.admin-buttons button').forEach(btn => btn.classList.remove('active'));
    button.classList.add('active');
    
    const tableContainer = document.querySelector('.table-container');
    tableContainer.innerHTML = '<div style="text-align: center; padding: 2rem;">Cargando...</div>';
    
    const apiEndpoint = tab === 'users' ? '/admin/api/users' : '/admin/api/products';
    
    fetch(apiEndpoint)
        .then(response => response.json())
        .then(data => {
            if (data.length === 0) {
                tableContainer.innerHTML = `<p class="no-data">No hay ${tab === 'users' ? 'usuarios' : 'productos'}.</p>`;
                return;
            }
            
            if (tab === 'users') {
                tableContainer.innerHTML = `
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Email</th>
                                <th>Teléfono</th>
                                <th>Rol</th>
                                <th>Creado</th>
                                <th>Cambiar rol</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.map(user => `
                                <tr>
                                    <td>${user.id}</td>
                                    <td>${user.firstName || ''} ${user.lastName || ''}</td>
                                    <td>${user.email}</td>
                                    <td>${user.phone || ''}</td>
                                    <td>${user.rol === 'ROLE_USER' ? 'Usuario' : 'Admin'}</td>
                                    <td>${new Date(user.createdAt).toLocaleString('es-ES')}</td>
                                    <td>
                                        <form action="/admin/update-role/${user.id}" method="post" style="display: inline;">
                                            <input type="hidden" name="_csrf" value="${document.querySelector('meta[name="_csrf"]').getAttribute('content') || ''}">
                                            <select name="newRole" onchange="this.form.submit()">
                                                <option value="ROLE_USER" ${user.rol === 'ROLE_USER' ? 'selected' : ''}>Usuario</option>
                                                <option value="ROLE_ADMIN" ${user.rol === 'ROLE_ADMIN' ? 'selected' : ''}>Admin</option>
                                            </select>
                                        </form>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            } else {
                tableContainer.innerHTML = `
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Precio</th>
                                <th>Stock</th>
                                <th>Categorías</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.map(product => `
                                <tr>
                                    <td>${product.id}</td>
                                    <td>${product.nombre}</td>
                                    <td>${product.precio}</td>
                                    <td>${product.stock}</td>
                                    <td>${product.categorias ? product.categorias.map(cat => cat.nombre).join(', ') : ''}</td>
                                    <td><button class="button button-small button-outline" title="Editar producto" onclick="window.location.href='/editarstock/' + ${product.id}">✏️</button></td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            tableContainer.innerHTML = '<p class="no-data">Error cargando datos.</p>';
        });
}

document.addEventListener('DOMContentLoaded', function() {
    if (document.querySelector('.admin-buttons')) {
        switchTab(document.querySelector('.admin-buttons .active'), 'users');
    }
});
