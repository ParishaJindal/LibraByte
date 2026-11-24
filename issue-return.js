/**
 * LibraByte - Circulation Desk Module
 * Heritage Library Edition
 */

let membersList = [];
let booksList = [];
let activeIssuesList = [];

document.addEventListener('DOMContentLoaded', () => {
    loadAllData();

    // Check query params if directed from Books page
    const params = new URLSearchParams(window.location.search);
    const preselectedBookId = params.get('bookId');
    if (preselectedBookId) {
        setTimeout(() => {
            const select = document.getElementById('issue-book-select');
            if (select) {
                select.value = preselectedBookId;
                handleBookSelectionChange();
            }
        }, 300);
    }

    const searchInput = document.getElementById('loan-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            filterActiveLoans(e.target.value);
        });
    }
});

async function loadAllData() {
    try {
        const [members, books, issues] = await Promise.all([
            apiRequest('/members'),
            apiRequest('/books'),
            apiRequest('/issues')
        ]);

        membersList = members;
        booksList = books;
        activeIssuesList = issues.filter(i => i.status === 'ISSUED' || i.status === 'OVERDUE');

        populateMemberDropdown(membersList);
        populateBookDropdown(booksList);
        populateReturnDropdown(activeIssuesList);
        renderActiveLoans(activeIssuesList);

    } catch (err) {
        showToast('Error loading circulation data: ' + err.message, 'error');
    }
}

function populateMemberDropdown(members) {
    const select = document.getElementById('issue-member-select');
    if (!select) return;

    select.innerHTML = '<option value="">Choose a member...</option>' +
        members.map(m => `
            <option value="${m.id}" ${m.status !== 'ACTIVE' ? 'disabled' : ''}>
                ${escapeHtml(m.fullName)} (${escapeHtml(m.email)}) [${m.status}]
            </option>
        `).join('');
}

function populateBookDropdown(books) {
    const select = document.getElementById('issue-book-select');
    if (!select) return;

    select.innerHTML = '<option value="">Choose a book...</option>' +
        books.map(b => `
            <option value="${b.id}" data-available="${b.availableCopies}" data-total="${b.totalCopies}">
                ${escapeHtml(b.title)} (${b.availableCopies} of ${b.totalCopies} available)
            </option>
        `).join('');
}

function populateReturnDropdown(issues) {
    const select = document.getElementById('return-issue-select');
    if (!select) return;

    select.innerHTML = '<option value="">Select an active loan to return...</option>' +
        issues.map(i => `
            <option value="${i.id}">
                Loan #${i.id}: "${escapeHtml(i.bookTitle)}" borrowed by ${escapeHtml(i.memberName)} (Due: ${formatDate(i.dueDate)})
            </option>
        `).join('');
}

function handleBookSelectionChange() {
    const select = document.getElementById('issue-book-select');
    const banner = document.getElementById('availability-banner');
    const submitBtn = document.getElementById('btn-issue-submit');
    const reserveBtn = document.getElementById('btn-reserve-fallback');

    if (!select.value) {
        banner.style.display = 'none';
        submitBtn.style.display = 'block';
        reserveBtn.style.display = 'none';
        return;
    }

    const selectedOption = select.options[select.selectedIndex];
    const available = parseInt(selectedOption.getAttribute('data-available'), 10);
    const total = parseInt(selectedOption.getAttribute('data-total'), 10);

    banner.style.display = 'block';

    if (available > 0) {
        banner.innerHTML = `
            <div style="padding: 0.75rem 1rem; background: var(--success-bg); border: 1px solid var(--success-border); border-radius: var(--radius-sm); color: var(--success-text); display: flex; align-items: center; justify-content: space-between;">
                <div>
                    <strong>Available for Immediate Loan:</strong> ${available} copy/copies remaining on shelf.
                </div>
                <span class="badge badge-success">In Stock</span>
            </div>
        `;
        submitBtn.disabled = false;
        submitBtn.style.display = 'block';
        reserveBtn.style.display = 'none';
    } else {
        banner.innerHTML = `
            <div style="padding: 0.75rem 1rem; background: var(--danger-bg); border: 1px solid var(--danger-border); border-radius: var(--radius-sm); color: var(--danger-text);">
                <strong>No Copies Available:</strong> All ${total} copies are currently in circulation. You can add this patron to the FIFO waitlist.
            </div>
        `;
        submitBtn.disabled = true;
        submitBtn.style.display = 'none';
        reserveBtn.style.display = 'block';
    }
}

function handleReturnSelectionChange() {
    const select = document.getElementById('return-issue-select');
    const preview = document.getElementById('return-preview');

    if (!select.value) {
        preview.style.display = 'none';
        return;
    }

    const issueId = parseInt(select.value, 10);
    const loan = activeIssuesList.find(i => i.id === issueId);
    if (!loan) return;

    preview.style.display = 'block';
    const isOverdue = loan.status === 'OVERDUE';

    preview.innerHTML = `
        <div style="font-weight: 600; color: var(--text-main); font-size: 0.95rem; margin-bottom: 0.35rem;">
            ${escapeHtml(loan.bookTitle)}
        </div>
        <div style="color: var(--text-muted); margin-bottom: 0.5rem; font-size: 0.85rem;">
            Borrower: <strong>${escapeHtml(loan.memberName)}</strong> (${escapeHtml(loan.memberEmail)})
        </div>
        <div style="display: flex; gap: 1rem; align-items: center; font-size: 0.82rem;">
            <span>Issued: ${formatDate(loan.issueDate)}</span>
            <span>Due: ${formatDate(loan.dueDate)}</span>
            <span class="badge ${isOverdue ? 'badge-danger' : 'badge-success'}">
                ${isOverdue ? 'OVERDUE' : 'ON TIME'}
            </span>
        </div>
    `;
}

async function processIssue(event) {
    event.preventDefault();
    const memberId = parseInt(document.getElementById('issue-member-select').value, 10);
    const bookId = parseInt(document.getElementById('issue-book-select').value, 10);

    if (!memberId || !bookId) {
        showToast('Please select both a patron and a book volume.', 'error');
        return;
    }

    try {
        const issue = await apiRequest('/issues', {
            method: 'POST',
            body: { bookId, memberId }
        });

        showToast(`Book volume successfully issued to ${issue.memberName}! Due Date: ${formatDate(issue.dueDate)}`, 'success', 5000);
        document.getElementById('issue-form').reset();
        document.getElementById('availability-banner').style.display = 'none';
        loadAllData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function reserveFromIssueForm() {
    const memberId = parseInt(document.getElementById('issue-member-select').value, 10);
    const bookId = parseInt(document.getElementById('issue-book-select').value, 10);

    if (!memberId || !bookId) {
        showToast('Please select a patron first to place them on the waitlist.', 'error');
        return;
    }

    try {
        const res = await apiRequest('/reservations', {
            method: 'POST',
            body: { bookId, memberId }
        });

        showToast(`Patron added to FIFO waitlist at position #${res.queuePosition}!`, 'gold', 5000);
        document.getElementById('issue-form').reset();
        document.getElementById('availability-banner').style.display = 'none';
        loadAllData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function processReturn(event) {
    event.preventDefault();
    const issueId = parseInt(document.getElementById('return-issue-select').value, 10);

    if (!issueId) {
        showToast('Please select a loan to process return.', 'error');
        return;
    }

    executeReturn(issueId);
}

async function executeReturn(issueId) {
    try {
        const result = await apiRequest('/issues/return', {
            method: 'POST',
            body: { issueId }
        });

        if (result.allocatedToQueue) {
            showToast(
                `✦ Volume returned! AUTOMATIC FIFO ALLOCATION: Allocated to reserved patron ${result.allocatedMemberName} (${result.allocatedMemberEmail})! Kept in reserve off public shelf.`,
                'gold',
                7000
            );
        } else {
            showToast(`Book returned successfully and restored to shelf availability!`, 'success', 4000);
        }

        document.getElementById('return-form').reset();
        document.getElementById('return-preview').style.display = 'none';
        loadAllData();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderActiveLoans(loans) {
    const tbody = document.getElementById('active-issues-table');
    if (!tbody) return;

    if (!loans || loans.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No active loans currently circulating.</td></tr>`;
        return;
    }

    tbody.innerHTML = loans.map(loan => {
        const isOverdue = loan.status === 'OVERDUE';
        return `
            <tr>
                <td><strong>#${loan.id}</strong></td>
                <td>
                    <div style="font-weight: 600; color: var(--text-main); font-size: 0.95rem;">${escapeHtml(loan.bookTitle)}</div>
                    <small style="color: var(--text-muted);">ISBN: ${escapeHtml(loan.bookIsbn || '—')}</small>
                </td>
                <td>
                    <div>${escapeHtml(loan.memberName)}</div>
                    <small style="color: var(--text-muted);">${escapeHtml(loan.memberEmail)}</small>
                </td>
                <td>${formatDate(loan.issueDate)}</td>
                <td><strong>${formatDate(loan.dueDate)}</strong></td>
                <td>
                    <span class="badge ${isOverdue ? 'badge-danger' : 'badge-info'}">
                        ${isOverdue ? '⚠️ OVERDUE' : 'CIRCULATING'}
                    </span>
                </td>
                <td style="text-align: right;">
                    <button class="btn btn-sm btn-primary" onclick="executeReturn(${loan.id})">
                        Return Book
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterActiveLoans(query) {
    if (!query) {
        renderActiveLoans(activeIssuesList);
        return;
    }
    const q = query.toLowerCase();
    const filtered = activeIssuesList.filter(l => 
        (l.bookTitle && l.bookTitle.toLowerCase().includes(q)) ||
        (l.memberName && l.memberName.toLowerCase().includes(q)) ||
        (l.memberEmail && l.memberEmail.toLowerCase().includes(q)) ||
        String(l.id).includes(q)
    );
    renderActiveLoans(filtered);
}
