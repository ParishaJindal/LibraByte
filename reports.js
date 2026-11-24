/**
 * LibraByte - Reports & Analytics Module
 * Heritage Library Edition
 */

let currentTab = 'overdue';
let overdueData = [];
let popularData = [];
let reservationData = [];

// Sort state trackers
let sortState = {
    field: '',
    asc: true
};

document.addEventListener('DOMContentLoaded', () => {
    loadAllReports();
});

async function loadAllReports() {
    try {
        const [overdue, popular, reservations] = await Promise.all([
            apiRequest('/reports/overdue'),
            apiRequest('/reports/popular-books'),
            apiRequest('/reports/reservations')
        ]);

        overdueData = overdue;
        popularData = popular;
        reservationData = reservations;

        renderCurrentReport();

    } catch (err) {
        showToast('Error loading report datasets: ' + err.message, 'error');
    }
}

function switchReportTab(tab) {
    currentTab = tab;

    document.querySelectorAll('.report-tab-btn').forEach(b => b.classList.remove('active'));
    document.getElementById(`tab-btn-${tab}`).classList.add('active');

    document.getElementById('report-section-overdue').style.display = tab === 'overdue' ? 'block' : 'none';
    document.getElementById('report-section-popular').style.display = tab === 'popular' ? 'block' : 'none';
    document.getElementById('report-section-reservations').style.display = tab === 'reservations' ? 'block' : 'none';

    document.getElementById('report-search-input').value = '';
    renderCurrentReport();
}

function renderCurrentReport() {
    if (currentTab === 'overdue') {
        renderOverdueTable(overdueData);
        updateSummaryBadge(`${overdueData.length} Overdue Loans`);
    } else if (currentTab === 'popular') {
        renderPopularTable(popularData);
        updateSummaryBadge(`${popularData.length} Catalog Titles Tracked`);
    } else if (currentTab === 'reservations') {
        const activeOnly = reservationData.filter(r => r.status === 'WAITING');
        renderReservationsTable(activeOnly);
        updateSummaryBadge(`${activeOnly.length} Active Waitlist Entries`);
    }
}

function updateSummaryBadge(text) {
    const badge = document.getElementById('report-summary-badge');
    if (badge) badge.textContent = text;
}

// -------------------------------------------------------------
// Render Tables
// -------------------------------------------------------------
function renderOverdueTable(items) {
    const tbody = document.getElementById('overdue-table-body');
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No overdue books! All active loans are within their 14-day borrowing window.</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(item => `
        <tr>
            <td><strong>#${item.issueId}</strong></td>
            <td>
                <div style="font-weight: 600; color: var(--text-main); font-size: 0.92rem;">${escapeHtml(item.memberName)}</div>
                <small style="color: var(--text-muted);">${escapeHtml(item.memberEmail)}</small>
            </td>
            <td><strong style="font-size: 0.92rem;">${escapeHtml(item.bookTitle)}</strong></td>
            <td>${formatDate(item.dueDate)}</td>
            <td>
                <span class="badge badge-danger" style="font-size: 0.78rem; font-weight: 700;">
                    ${item.overdueDays} Days Overdue
                </span>
            </td>
            <td><span class="badge badge-danger">OVERDUE</span></td>
            <td style="text-align: right;">
                <a href="issue-return.html" class="btn btn-sm btn-primary">Process Return</a>
            </td>
        </tr>
    `).join('');
}

function renderPopularTable(items) {
    const tbody = document.getElementById('popular-table-body');
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No circulation history recorded yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(item => `
        <tr>
            <td>
                <span class="badge ${item.popularityRank === 1 ? 'badge-gold' : 'badge-navy'}" style="font-weight: 700; width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; font-family: var(--font-serif); font-size: 0.95rem;">
                    #${item.popularityRank}
                </span>
            </td>
            <td>
                <div style="font-weight: 600; color: var(--text-main); font-size: 0.92rem;">${escapeHtml(item.title)}</div>
                <small style="color: var(--text-muted);">ISBN: ${escapeHtml(item.isbn)}</small>
            </td>
            <td>${escapeHtml(item.author)}</td>
            <td><span class="badge badge-info">${escapeHtml(item.category)}</span></td>
            <td>
                <strong style="font-size: 1rem; color: var(--text-main);">${item.borrowCount}</strong> loans
            </td>
            <td>${item.availableCopies} available / ${item.totalCopies} total</td>
        </tr>
    `).join('');
}

function renderReservationsTable(items) {
    const tbody = document.getElementById('reservations-table-body');
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No waitlist entries currently recorded.</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(item => `
        <tr>
            <td>
                <span class="badge ${item.queuePosition === 1 ? 'badge-gold' : 'badge-navy'}" style="font-size: 0.8rem; font-weight: 700;">
                    ${item.queuePosition === 1 ? '✦ Position #1 (Next Allocated)' : `Position #${item.queuePosition}`}
                </span>
            </td>
            <td><strong style="font-size: 0.92rem;">${escapeHtml(item.bookTitle)}</strong></td>
            <td>${escapeHtml(item.memberName)}</td>
            <td>${escapeHtml(item.memberEmail || '—')}</td>
            <td>${escapeHtml(item.reservationDate)}</td>
            <td><span class="badge badge-warning">${item.status}</span></td>
        </tr>
    `).join('');
}

// -------------------------------------------------------------
// Search & Filter in Active Report
// -------------------------------------------------------------
function handleReportSearch() {
    const query = document.getElementById('report-search-input').value.toLowerCase().trim();
    if (!query) {
        renderCurrentReport();
        return;
    }

    if (currentTab === 'overdue') {
        const filtered = overdueData.filter(i => 
            i.memberName.toLowerCase().includes(query) ||
            i.bookTitle.toLowerCase().includes(query) ||
            i.memberEmail.toLowerCase().includes(query) ||
            String(i.issueId).includes(query)
        );
        renderOverdueTable(filtered);
    } else if (currentTab === 'popular') {
        const filtered = popularData.filter(i => 
            i.title.toLowerCase().includes(query) ||
            i.author.toLowerCase().includes(query) ||
            i.category.toLowerCase().includes(query) ||
            i.isbn.toLowerCase().includes(query)
        );
        renderPopularTable(filtered);
    } else if (currentTab === 'reservations') {
        const filtered = reservationData.filter(i => 
            (i.bookTitle && i.bookTitle.toLowerCase().includes(query)) ||
            (i.memberName && i.memberName.toLowerCase().includes(query)) ||
            (i.memberEmail && i.memberEmail.toLowerCase().includes(query))
        );
        renderReservationsTable(filtered);
    }
}

// -------------------------------------------------------------
// Frontend Column Sorting
// -------------------------------------------------------------
function toggleSort(field) {
    if (sortState.field === field) {
        sortState.asc = !sortState.asc;
    } else {
        sortState.field = field;
        sortState.asc = true;
    }
    return sortState.asc ? 1 : -1;
}

function sortOverdue(field) {
    const dir = toggleSort(field);
    overdueData.sort((a, b) => {
        if (typeof a[field] === 'string') return a[field].localeCompare(b[field]) * dir;
        return (a[field] - b[field]) * dir;
    });
    renderOverdueTable(overdueData);
}

function sortPopular(field) {
    const dir = toggleSort(field);
    popularData.sort((a, b) => {
        if (typeof a[field] === 'string') return a[field].localeCompare(b[field]) * dir;
        return (a[field] - b[field]) * dir;
    });
    renderPopularTable(popularData);
}

function sortReservations(field) {
    const dir = toggleSort(field);
    reservationData.sort((a, b) => {
        if (typeof a[field] === 'string') return a[field].localeCompare(b[field]) * dir;
        return (a[field] - b[field]) * dir;
    });
    renderReservationsTable(reservationData);
}

// -------------------------------------------------------------
// CSV Export Generator
// -------------------------------------------------------------
function exportCurrentReportToCSV() {
    let csvContent = "data:text/csv;charset=utf-8,";
    let filename = `librabyte_${currentTab}_report_${new Date().toISOString().slice(0, 10)}.csv`;

    if (currentTab === 'overdue') {
        csvContent += "Loan ID,Patron Name,Patron Email,Book Title,Due Date,Overdue Days,Status\n";
        overdueData.forEach(row => {
            csvContent += `"${row.issueId}","${row.memberName}","${row.memberEmail}","${row.bookTitle}","${row.dueDate}","${row.overdueDays}","${row.status}"\n`;
        });
    } else if (currentTab === 'popular') {
        csvContent += "Circulation Rank,Title,Author,Category,ISBN,Borrow Count,Available Copies,Total Copies\n";
        popularData.forEach(row => {
            csvContent += `"${row.popularityRank}","${row.title}","${row.author}","${row.category}","${row.isbn}","${row.borrowCount}","${row.availableCopies}","${row.totalCopies}"\n`;
        });
    } else if (currentTab === 'reservations') {
        csvContent += "Queue Position,Book Title,Patron Name,Patron Email,Reservation Date,Status\n";
        reservationData.forEach(row => {
            csvContent += `"${row.queuePosition}","${row.bookTitle}","${row.memberName}","${row.memberEmail}","${row.reservationDate}","${row.status}"\n`;
        });
    }

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast(`Report exported successfully as ${filename}`, 'success');
}
