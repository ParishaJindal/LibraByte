/**
 * LibraByte - Reservation Queue Optimization Module
 * Heritage Library Edition
 */

let allBooks = [];
let allMembers = [];
let allReservations = [];

document.addEventListener('DOMContentLoaded', () => {
    loadQueues();

    // Check query params if bookId is passed
    const params = new URLSearchParams(window.location.search);
    const preselectedBookId = params.get('bookId');
    if (preselectedBookId) {
        setTimeout(() => {
            const filter = document.getElementById('queue-book-filter');
            if (filter) {
                filter.value = preselectedBookId;
                handleQueueBookFilter();
            }
        }, 400);
    }
});

async function loadQueues() {
    try {
        const [books, members, reservations] = await Promise.all([
            apiRequest('/books'),
            apiRequest('/members'),
            apiRequest('/reservations')
        ]);

        allBooks = books;
        allMembers = members;
        allReservations = reservations;

        populateBookFilter(allBooks);
        populateModalDropdowns(allBooks, allMembers);
        renderQueues(allBooks, allReservations);

    } catch (err) {
        showToast('Failed to load reservation waitlists: ' + err.message, 'error');
    }
}

function populateBookFilter(books) {
    const select = document.getElementById('queue-book-filter');
    if (!select) return;

    select.innerHTML = '<option value="">All Books with Active Queues</option>' +
        books.map(b => `<option value="${b.id}">${escapeHtml(b.title)}</option>`).join('');
}

function populateModalDropdowns(books, members) {
    const memberSelect = document.getElementById('reserve-member-select');
    const bookSelect = document.getElementById('reserve-book-select');

    if (memberSelect) {
        memberSelect.innerHTML = '<option value="">Choose a member...</option>' +
            members.map(m => `<option value="${m.id}" ${m.status !== 'ACTIVE' ? 'disabled' : ''}>${escapeHtml(m.fullName)} (${escapeHtml(m.email)})</option>`).join('');
    }

    if (bookSelect) {
        bookSelect.innerHTML = '<option value="">Choose a book to reserve...</option>' +
            books.map(b => `<option value="${b.id}" data-available="${b.availableCopies}">${escapeHtml(b.title)} (Available: ${b.availableCopies}/${b.totalCopies})</option>`).join('');
    }
}

function handleQueueBookFilter() {
    const selectedBookId = document.getElementById('queue-book-filter').value;
    if (!selectedBookId) {
        renderQueues(allBooks, allReservations);
    } else {
        const bookId = parseInt(selectedBookId, 10);
        const filteredBooks = allBooks.filter(b => b.id === bookId);
        renderQueues(filteredBooks, allReservations);
    }
}

function renderQueues(books, reservations) {
    const container = document.getElementById('queues-container');
    const statActiveQueues = document.getElementById('stat-active-queues');
    if (!container) return;

    // Filter active WAITING reservations
    const activeReservations = reservations.filter(r => r.status === 'WAITING');

    // Group reservations by bookId
    const queueMap = new Map();
    activeReservations.forEach(r => {
        if (!queueMap.has(r.bookId)) {
            queueMap.set(r.bookId, []);
        }
        queueMap.get(r.bookId).push(r);
    });

    // Sort reservations in each queue by position ASC (FIFO)
    queueMap.forEach(qList => {
        qList.sort((a, b) => a.queuePosition - b.queuePosition);
    });

    if (statActiveQueues) {
        statActiveQueues.textContent = `${queueMap.size} Title(s) / ${activeReservations.length} Patron(s)`;
    }

    // Determine which books have active queues
    const booksWithQueues = books.filter(b => queueMap.has(b.id) && queueMap.get(b.id).length > 0);

    if (booksWithQueues.length === 0) {
        container.innerHTML = `
            <div class="card">
                <div class="card-body empty-state">
                    <div class="empty-state-icon">📋</div>
                    <h3>No Active Waitlists</h3>
                    <p>There are currently no patrons waiting in queue for the selected book(s). When titles run out of shelf copies, patron reservations build automatically here.</p>
                    <div style="margin-top: 1.25rem;">
                        <button class="btn btn-gold btn-sm" onclick="openReserveModal()">+ Reserve a Book</button>
                    </div>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = booksWithQueues.map(book => {
        const queue = queueMap.get(book.id) || [];
        return `
            <div class="card" style="margin-bottom: 2rem;">
                <div class="card-header" style="background: var(--bg-surface-warm);">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <h3 class="card-title" style="font-size: 1.15rem; color: var(--text-main);">
                                📖 ${escapeHtml(book.title)}
                            </h3>
                            <span class="badge badge-gold" style="font-size: 0.8rem;">
                                ${queue.length} Waiting in FIFO Queue
                            </span>
                        </div>
                        <div class="card-subtitle" style="margin-top: 0.25rem;">
                            By ${escapeHtml(book.author)} &bull; Copies on shelf: 
                            <strong style="color: ${book.availableCopies > 0 ? 'var(--sidebar-bg)' : 'var(--danger-text)'};">${book.availableCopies} of ${book.totalCopies}</strong>
                        </div>
                    </div>
                    <div>
                        <button class="btn btn-outline btn-sm" onclick="openReserveForBook(${book.id})">+ Add Patron to Queue</button>
                    </div>
                </div>
                <div class="card-body">
                    <div class="queue-list">
                        ${queue.map(res => `
                            <div class="queue-card">
                                <div style="display: flex; align-items: center; gap: 1.25rem;">
                                    <div class="queue-badge-pos">
                                        #${res.queuePosition}
                                        <span>Pos</span>
                                    </div>
                                    <div>
                                        <div style="display: flex; align-items: center; gap: 0.6rem;">
                                            <span style="font-weight: 700; color: var(--text-main); font-size: 1rem;">
                                                ${escapeHtml(res.memberName)}
                                            </span>
                                            ${res.queuePosition === 1 
                                                ? '<span class="badge badge-gold">✦ 1st in Line - Next Allocated</span>' 
                                                : `<span class="badge badge-navy">Queue #${res.queuePosition}</span>`
                                            }
                                        </div>
                                        <div style="font-size: 0.82rem; color: var(--text-muted); margin-top: 0.2rem;">
                                            Patron Email: ${escapeHtml(res.memberEmail || '—')} &bull; Reserved: ${escapeHtml(res.reservationDate)}
                                        </div>
                                    </div>
                                </div>
                                <div style="display: flex; align-items: center; gap: 0.75rem;">
                                    <button class="btn btn-sm btn-danger" onclick="cancelReservation(${res.id}, '${escapeHtml(res.memberName.replace(/'/g, "\\'"))}', '${escapeHtml(book.title.replace(/'/g, "\\'"))}')">
                                        Cancel Reservation
                                    </button>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function openReserveModal() {
    document.getElementById('reserve-form').reset();
    document.getElementById('modal-queue-preview').style.display = 'none';
    openModal('reserve-modal');
}

function openReserveForBook(bookId) {
    openReserveModal();
    const bookSelect = document.getElementById('reserve-book-select');
    if (bookSelect) {
        bookSelect.value = bookId;
        updateModalQueuePreview();
    }
}

function updateModalQueuePreview() {
    const bookSelect = document.getElementById('reserve-book-select');
    const preview = document.getElementById('modal-queue-preview');
    if (!bookSelect.value) {
        preview.style.display = 'none';
        return;
    }

    const bookId = parseInt(bookSelect.value, 10);
    const existingQueue = allReservations.filter(r => r.bookId === bookId && r.status === 'WAITING');
    const nextPos = existingQueue.length + 1;

    preview.style.display = 'block';
    preview.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
                <strong>Current Waitlist:</strong> ${existingQueue.length} patron(s) waiting.
            </div>
            <span class="badge badge-gold">Assigned Position: #${nextPos}</span>
        </div>
    `;
}

async function submitReservation(event) {
    event.preventDefault();
    const memberId = parseInt(document.getElementById('reserve-member-select').value, 10);
    const bookId = parseInt(document.getElementById('reserve-book-select').value, 10);

    if (!memberId || !bookId) {
        showToast('Please select both a patron and a book volume.', 'error');
        return;
    }

    try {
        const res = await apiRequest('/reservations', {
            method: 'POST',
            body: { bookId, memberId }
        });

        showToast(`Reservation confirmed! Patron assigned FIFO waitlist position #${res.queuePosition}.`, 'gold', 5000);
        closeModal('reserve-modal');
        loadQueues();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function cancelReservation(reservationId, memberName, bookTitle) {
    if (!confirm(`Cancel reservation for ${memberName} on "${bookTitle}"?\n\nSubsequent patrons in the FIFO queue will automatically move up one position.`)) {
        return;
    }

    try {
        const result = await apiRequest(`/reservations/${reservationId}`, { method: 'DELETE' });
        showToast(result.message || 'Reservation cancelled and queue positions adjusted.', 'success');
        loadQueues();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
