/**
 * LibraByte - Member Directory Module
 * Heritage Library Edition
 */

let allMembers = [];

document.addEventListener('DOMContentLoaded', () => {
    loadMembers();

    const searchInput = document.getElementById('member-search-input');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                searchMembers(e.target.value);
            }, 250);
        });
    }
});

async function loadMembers() {
    try {
        allMembers = await apiRequest('/members');
        renderMembers(allMembers);
    } catch (err) {
        showToast('Error loading patron directory: ' + err.message, 'error');
    }
}

async function searchMembers(query) {
    try {
        const url = query ? `/members?search=${encodeURIComponent(query)}` : '/members';
        const members = await apiRequest(url);
        renderMembers(members);
    } catch (err) {
        showToast('Search failed: ' + err.message, 'error');
    }
}

function renderMembers(members) {
    const tbody = document.getElementById('members-table-body');
    if (!tbody) return;

    if (!members || members.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state"><h3>No patrons found</h3><p>Try searching another keyword or register a new library member.</p></td></tr>`;
        return;
    }

    tbody.innerHTML = members.map(m => {
        let statusBadge = '<span class="badge badge-success">ACTIVE</span>';
        if (m.status === 'SUSPENDED') statusBadge = '<span class="badge badge-danger">SUSPENDED</span>';
        if (m.status === 'INACTIVE') statusBadge = '<span class="badge badge-warning">INACTIVE</span>';

        return `
            <tr>
                <td><strong>#${m.id}</strong></td>
                <td>
                    <div style="font-weight: 600; color: var(--text-main); font-size: 0.95rem;">${escapeHtml(m.fullName)}</div>
                </td>
                <td>${escapeHtml(m.email)}</td>
                <td>${escapeHtml(m.phone)}</td>
                <td>${formatDate(m.registrationDate)}</td>
                <td>${statusBadge}</td>
                <td style="text-align: right; white-space: nowrap;">
                    <button class="btn btn-sm btn-outline" onclick="viewMemberProfile(${m.id})" title="View Profile">👤 Profile</button>
                    <button class="btn btn-sm btn-outline" onclick="openEditMemberModal(${m.id})" title="Edit Member">✏️</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteMember(${m.id}, '${escapeHtml(m.fullName.replace(/'/g, "\\'"))}')" title="Delete Member">🗑️</button>
                </td>
            </tr>
        `;
    }).join('');
}

function openAddMemberModal() {
    document.getElementById('member-form').reset();
    document.getElementById('member-id').value = '';
    document.getElementById('member-modal-title').textContent = 'Register New Patron';
    openModal('member-modal');
}

async function openEditMemberModal(id) {
    try {
        const member = await apiRequest(`/members/${id}`);
        document.getElementById('member-id').value = member.id;
        document.getElementById('member-name').value = member.fullName;
        document.getElementById('member-email').value = member.email;
        document.getElementById('member-phone').value = member.phone;
        document.getElementById('member-status').value = member.status || 'ACTIVE';

        document.getElementById('member-modal-title').textContent = `Edit Patron #${member.id}`;
        openModal('member-modal');
    } catch (err) {
        showToast('Failed to fetch patron details: ' + err.message, 'error');
    }
}

async function viewMemberProfile(id) {
    try {
        const member = await apiRequest(`/members/${id}`);
        const issues = await apiRequest('/issues');
        const memberIssues = issues.filter(i => i.memberId === id);
        const activeIssues = memberIssues.filter(i => i.status === 'ISSUED' || i.status === 'OVERDUE');

        const reservations = await apiRequest('/reservations');
        const memberReservations = reservations.filter(r => r.memberId === id && r.status === 'WAITING');

        const body = document.getElementById('profile-modal-body');
        body.innerHTML = `
            <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px solid var(--border-light);">
                <div style="width: 52px; height: 52px; border-radius: var(--radius-sm); background: var(--sidebar-bg); color: #FFFFFF; display: flex; align-items: center; justify-content: center; font-size: 1.35rem; font-family: var(--font-serif); font-weight: 700;">
                    ${escapeHtml(member.fullName.charAt(0).toUpperCase())}
                </div>
                <div>
                    <h4 style="font-size: 1.25rem; color: var(--text-main); font-family: var(--font-serif);">${escapeHtml(member.fullName)}</h4>
                    <span class="badge ${member.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}">${member.status}</span>
                    <small style="color: var(--text-muted); margin-left: 0.5rem;">Registered ${formatDate(member.registrationDate)}</small>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem;">
                <div>
                    <span style="font-size: 0.74rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600; letter-spacing: 0.05em;">Email</span>
                    <p style="font-weight: 500; font-size: 0.9rem;">${escapeHtml(member.email)}</p>
                </div>
                <div>
                    <span style="font-size: 0.74rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600; letter-spacing: 0.05em;">Phone</span>
                    <p style="font-weight: 500; font-size: 0.9rem;">${escapeHtml(member.phone)}</p>
                </div>
            </div>

            <h5 style="margin-bottom: 0.75rem; color: var(--text-main); font-family: var(--font-serif); font-size: 1rem;">Active Circulating Loans (${activeIssues.length})</h5>
            ${activeIssues.length === 0 ? '<p style="color: var(--text-muted); font-size: 0.85rem; margin-bottom: 1.25rem;">No active loans currently checked out.</p>' : `
                <div style="margin-bottom: 1.25rem;">
                    ${activeIssues.map(iss => `
                        <div style="padding: 0.6rem 0.85rem; background: var(--bg-surface-warm); border-radius: var(--radius-sm); border: 1px solid var(--border-light); margin-bottom: 0.45rem; display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <strong style="color: var(--text-main); font-size: 0.9rem;">${escapeHtml(iss.bookTitle)}</strong>
                                <div style="font-size: 0.76rem; color: var(--text-muted);">Due: ${formatDate(iss.dueDate)}</div>
                            </div>
                            <span class="badge ${iss.status === 'OVERDUE' ? 'badge-danger' : 'badge-info'}">${iss.status}</span>
                        </div>
                    `).join('')}
                </div>
            `}

            <h5 style="margin-bottom: 0.75rem; color: var(--text-main); font-family: var(--font-serif); font-size: 1rem;">Active Waitlist Positions (${memberReservations.length})</h5>
            ${memberReservations.length === 0 ? '<p style="color: var(--text-muted); font-size: 0.85rem;">No active reservations in queue.</p>' : `
                <div>
                    ${memberReservations.map(res => `
                        <div style="padding: 0.6rem 0.85rem; background: var(--bronze-light); border-radius: var(--radius-sm); margin-bottom: 0.45rem; display: flex; justify-content: space-between; align-items: center; border: 1px solid var(--bronze-border);">
                            <div>
                                <strong style="color: var(--text-main); font-size: 0.9rem;">${escapeHtml(res.bookTitle)}</strong>
                                <div style="font-size: 0.76rem; color: var(--text-muted);">FIFO Position: #${res.queuePosition} in waitlist</div>
                            </div>
                            <span class="badge badge-gold">WAITING</span>
                        </div>
                    `).join('')}
                </div>
            `}
        `;

        openModal('profile-modal');
    } catch (err) {
        showToast('Failed to load patron profile: ' + err.message, 'error');
    }
}

async function saveMember(event) {
    event.preventDefault();
    const id = document.getElementById('member-id').value;
    const isEdit = Boolean(id);

    const payload = {
        fullName: document.getElementById('member-name').value.trim(),
        email: document.getElementById('member-email').value.trim(),
        phone: document.getElementById('member-phone').value.trim(),
        status: document.getElementById('member-status').value
    };

    try {
        if (isEdit) {
            await apiRequest(`/members/${id}`, {
                method: 'PUT',
                body: payload
            });
            showToast('Patron profile updated successfully!', 'success');
        } else {
            await apiRequest('/members', {
                method: 'POST',
                body: payload
            });
            showToast('New patron registered successfully!', 'success');
        }

        closeModal('member-modal');
        loadMembers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteMember(id, name) {
    if (!confirm(`Are you sure you want to remove patron "${name}"?\n\nNote: Members can only be deleted if they have no active loans or reservations.`)) {
        return;
    }

    try {
        const result = await apiRequest(`/members/${id}`, { method: 'DELETE' });
        showToast(result.message || 'Patron removed successfully', 'success');
        loadMembers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
