/**
 * LibraByte - Book Management Module
 * Heritage Library Edition
 */

let allBooks = [];

document.addEventListener('DOMContentLoaded', () => {
    loadBooks();

    const searchInput = document.getElementById('book-search-input');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                searchBooks(e.target.value);
            }, 250);
        });
    }
});

async function loadBooks() {
    try {
        allBooks = await apiRequest('/books');
        populateCategories(allBooks);
        renderBooks(allBooks);
    } catch (err) {
        showToast('Error loading books: ' + err.message, 'error');
    }
}

async function searchBooks(query) {
    try {
        const url = query ? `/books?search=${encodeURIComponent(query)}` : '/books';
        const books = await apiRequest(url);
        renderBooks(books);
    } catch (err) {
        showToast('Search failed: ' + err.message, 'error');
    }
}

const CSE_CATEGORIES = [
    "C Programming",
    "Computer Networks",
    "Operating Systems",
    "Java Programming",
    "Database Management Systems (DBMS)",
    "Frontend Development",
    "Discrete Mathematics"
];

function populateCategories(books) {
    const select = document.getElementById('category-filter');
    if (!select) return;

    const bookCategories = (books || []).map(b => b.category).filter(Boolean);
    const set = new Set([...CSE_CATEGORIES, ...bookCategories]);
    const categories = Array.from(set);

    const currentVal = select.value;
    select.innerHTML = '<option value="">All Categories</option>' +
        categories.map(c => `<option value="${escapeHtml(c)}"${c === currentVal ? ' selected' : ''}>${escapeHtml(c)}</option>`).join('');
}

function filterByCategory() {
    const selected = document.getElementById('category-filter').value;
    if (!selected) {
        renderBooks(allBooks);
    } else {
        const filtered = allBooks.filter(b => b.category === selected);
        renderBooks(filtered);
    }
}

function renderBooks(books) {
    const tbody = document.getElementById('books-table-body');
    if (!tbody) return;

    if (!books || books.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="empty-state"><h3>No catalog titles found</h3><p>Try a different keyword or add a new volume to the library.</p></td></tr>`;
        return;
    }

    tbody.innerHTML = books.map(book => {
        const issued = Math.max(0, book.totalCopies - book.availableCopies);
        let statusBadge = '<span class="badge badge-success">Available</span>';
        if (book.availableCopies === 0) {
            statusBadge = '<span class="badge badge-danger">All Issued (Waitlist Open)</span>';
        } else if (book.availableCopies < book.totalCopies) {
            statusBadge = '<span class="badge badge-warning">Partially Issued</span>';
        }

        return `
            <tr>
                <td><strong>#${book.id}</strong></td>
                <td>
                    <div style="font-weight: 600; color: var(--text-main); font-size: 0.95rem;">${escapeHtml(book.title)}</div>
                    <div style="color: var(--text-muted); font-size: 0.8rem;">By ${escapeHtml(book.author)} &bull; ${escapeHtml(book.publisher)}</div>
                </td>
                <td><code>${escapeHtml(book.isbn)}</code></td>
                <td><span class="badge badge-info">${escapeHtml(book.category)}</span></td>
                <td><strong>${book.totalCopies}</strong></td>
                <td><strong style="color: ${book.availableCopies > 0 ? 'var(--sidebar-bg)' : 'var(--danger-text)'};">${book.availableCopies}</strong></td>
                <td>${issued}</td>
                <td>${statusBadge}</td>
                <td style="text-align: right; white-space: nowrap;">
                    ${book.availableCopies > 0 
                        ? `<a href="issue-return.html?bookId=${book.id}" class="btn btn-sm btn-primary" style="margin-right: 0.35rem;">Issue</a>` 
                        : `<a href="reservations.html?bookId=${book.id}" class="btn btn-sm btn-outline" style="margin-right: 0.35rem; color: var(--bronze); border-color: var(--bronze-border);">Waitlist</a>`
                    }
                    <button class="btn btn-sm btn-outline" onclick="openEditBookModal(${book.id})" title="Edit Volume">✏️</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteBook(${book.id}, '${escapeHtml(book.title.replace(/'/g, "\\'"))}')" title="Delete Volume">🗑️</button>
                </td>
            </tr>
        `;
    }).join('');
}

function openAddBookModal() {
    document.getElementById('book-form').reset();
    document.getElementById('book-id').value = '';
    document.getElementById('book-modal-title').textContent = 'Add New Book Volume';
    document.getElementById('available-copies-group').style.display = 'none';
    openModal('book-modal');
}

async function openEditBookModal(id) {
    try {
        const book = await apiRequest(`/books/${id}`);
        document.getElementById('book-id').value = book.id;
        document.getElementById('book-title').value = book.title;
        document.getElementById('book-author').value = book.author;
        document.getElementById('book-isbn').value = book.isbn;
        document.getElementById('book-category').value = book.category;
        document.getElementById('book-publisher').value = book.publisher;
        document.getElementById('book-total-copies').value = book.totalCopies;
        document.getElementById('book-available-copies').value = book.availableCopies;
        document.getElementById('available-copies-group').style.display = 'block';

        document.getElementById('book-modal-title').textContent = `Edit Book Volume #${book.id}`;
        openModal('book-modal');
    } catch (err) {
        showToast('Failed to fetch book details: ' + err.message, 'error');
    }
}

async function saveBook(event) {
    event.preventDefault();
    const id = document.getElementById('book-id').value;
    const isEdit = Boolean(id);

    const payload = {
        title: document.getElementById('book-title').value.trim(),
        author: document.getElementById('book-author').value.trim(),
        isbn: document.getElementById('book-isbn').value.trim(),
        category: document.getElementById('book-category').value.trim(),
        publisher: document.getElementById('book-publisher').value.trim(),
        totalCopies: parseInt(document.getElementById('book-total-copies').value, 10)
    };

    try {
        if (isEdit) {
            await apiRequest(`/books/${id}`, {
                method: 'PUT',
                body: payload
            });
            showToast('Book volume updated successfully!', 'success');
        } else {
            await apiRequest('/books', {
                method: 'POST',
                body: payload
            });
            showToast('New title cataloged in library collection!', 'success');
        }

        closeModal('book-modal');
        loadBooks();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteBook(id, title) {
    if (!confirm(`Are you sure you want to remove "${title}" from the catalog?\n\nNote: Books can only be removed if they have no active loans or queued patrons.`)) {
        return;
    }

    try {
        const result = await apiRequest(`/books/${id}`, { method: 'DELETE' });
        showToast(result.message || 'Book removed from catalog', 'success');
        loadBooks();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
