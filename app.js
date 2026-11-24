/**
 * LibraByte - Global Application Framework
 * Core JavaScript utility module for API communications, UI components, and state management.
 * Heritage Library Palette Edition: Warm Ivory, Forest Green, Pure White, Muted Bronze, and Charcoal.
 */

const API_BASE = '/api';

// -------------------------------------------------------------
// Unified API Client with Error Handling
// -------------------------------------------------------------
async function apiRequest(endpoint, options = {}) {
    const config = {
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        },
        ...options
    };

    if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
        config.body = JSON.stringify(config.body);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, config);
        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMsg = (data && data.error) ? data.error : `HTTP error ${response.status}: ${response.statusText}`;
            throw new Error(errorMsg);
        }

        return data;
    } catch (err) {
        console.error(`API Error [${endpoint}]:`, err);
        throw err;
    }
}

// -------------------------------------------------------------
// Toast Notification Engine (Dignified Library Styling)
// -------------------------------------------------------------
function showToast(message, type = 'info', duration = 4000) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = '📖';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '✕';
    if (type === 'gold' || type === 'bronze') icon = '✦';

    toast.innerHTML = `
        <span style="font-weight: 700; color: var(--bronze); font-size: 1.1rem; line-height: 1;">${icon}</span>
        <div style="flex: 1;">
            <p style="margin: 0; line-height: 1.4; font-weight: 500; font-size: 0.88rem;">${escapeHtml(message)}</p>
        </div>
        <button onclick="this.parentElement.remove()" style="background:none; border:none; color:var(--text-light); cursor:pointer; font-size:1.1rem;">&times;</button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.25s ease, transform 0.25s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-10px)';
        setTimeout(() => toast.remove(), 250);
    }, duration);
}

// -------------------------------------------------------------
// Modal Dialog Controller
// -------------------------------------------------------------
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
}

// Close modal when clicking outside backdrop
window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
        document.body.style.overflow = '';
    }
});

// Close modal on Escape
window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.active').forEach(modal => {
            modal.classList.remove('active');
        });
        document.body.style.overflow = '';
    }
});

// -------------------------------------------------------------
// Formatting & Sanitization Helpers
// -------------------------------------------------------------
function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
        const parts = dateStr.substring(0, 10).split('-');
        if (parts.length === 3) {
            return `${parts[1]}/${parts[2]}/${parts[0]}`;
        }
    } catch (e) {}
    return dateStr;
}

// -------------------------------------------------------------
// Authentication & Role-Based Session Guard
// -------------------------------------------------------------
let currentAuthUser = null;

async function checkAuth(requiredRole = 'ADMIN') {
    // Skip auth guard on public auth pages
    const path = window.location.pathname;
    if (path.endsWith('login.html') || path.endsWith('signup.html')) {
        return null;
    }

    try {
        const response = await fetch(`${API_BASE}/auth/me`);
        if (!response.ok) {
            // Not authenticated -> redirect to login
            window.location.href = 'login.html?unauthorized=true';
            return null;
        }

        currentAuthUser = await response.json();

        // Enforce role restrictions
        if (requiredRole === 'ADMIN' && currentAuthUser.role !== 'ADMIN') {
            window.location.href = 'member-dashboard.html';
            return null;
        }

        renderAuthNavigation(currentAuthUser);
        return currentAuthUser;
    } catch (err) {
        console.error('Session guard error:', err);
        window.location.href = 'login.html';
        return null;
    }
}

async function logoutUser() {
    try {
        await fetch(`${API_BASE}/auth/logout`, { method: 'POST' });
    } catch (err) {
        console.error('Logout error:', err);
    }
    localStorage.removeItem('librabyte_user');
    window.location.href = 'login.html?logged_out=true';
}

function renderAuthNavigation(user) {
    if (!user) return;

    // 1. Render structured, non-overlapping Active Account section in sidebar footer
    const footer = document.querySelector('.sidebar-footer');
    if (footer) {
        footer.innerHTML = `
            <div class="sidebar-account-card" id="sidebar-account-card">
                <div class="sidebar-account-heading">ACTIVE ACCOUNT</div>
                <div class="sidebar-account-name">${escapeHtml(user.fullName)}</div>
                <div class="sidebar-account-email">${escapeHtml(user.email)}</div>
                <div class="sidebar-account-actions">
                    <span class="badge ${user.role === 'ADMIN' ? 'badge-gold' : 'badge-info'}">
                        ${escapeHtml(user.role)}
                    </span>
                    <button id="sidebar-logout-btn" onclick="logoutUser()" class="sidebar-logout-btn" title="Sign out of your session">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                            <polyline points="16 17 21 12 16 7"></polyline>
                            <line x1="21" y1="12" x2="9" y2="12"></line>
                        </svg>
                        Sign Out
                    </button>
                </div>
            </div>
            <hr class="sidebar-divider">
            <div class="sidebar-status-section">
                <span class="sidebar-status-text">Core Java &bull; SQLite</span>
                <div class="server-pill">
                    <span class="status-dot"></span> Online
                </div>
            </div>
        `;
    }

    // 2. Add Login & Sign Up links to sidebar navigation for quick switching/testing
    const navMenu = document.querySelector('.nav-menu');
    if (navMenu && !document.getElementById('nav-auth-section')) {
        const authLi = document.createElement('li');
        authLi.id = 'nav-auth-section';
        authLi.style.cssText = 'margin-top: 0.75rem; padding-top: 0.75rem; border-top: 1px solid rgba(245, 241, 232, 0.12);';
        authLi.innerHTML = `
            <div style="padding: 0 1rem 0.4rem; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.08em; color: rgba(211, 221, 214, 0.6);">Account & Portals</div>
            <div class="nav-item">
                <a href="profile.html">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                    My Profile
                </a>
            </div>
            <div class="nav-item">
                <a href="member-dashboard.html">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                    Member Portal
                </a>
            </div>
            <div class="nav-item">
                <a href="login.html">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                        <polyline points="10 17 15 12 10 7"></polyline>
                        <line x1="15" y1="12" x2="3" y2="12"></line>
                    </svg>
                    Switch User / Login
                </a>
            </div>
            <div class="nav-item">
                <a href="signup.html">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <line x1="20" y1="8" x2="20" y2="14"></line>
                        <line x1="23" y1="11" x2="17" y2="11"></line>
                    </svg>
                    Register Member
                </a>
            </div>
        `;
        navMenu.appendChild(authLi);
    }

    // 3. Add visible Logout button & User badge in header actions
    const headerActions = document.querySelector('.top-header .header-actions');
    if (headerActions && !document.getElementById('header-logout-btn')) {
        const userChip = document.createElement('div');
        userChip.id = 'header-user-chip';
        userChip.style.cssText = 'display: inline-flex; align-items: center; gap: 0.5rem; margin-right: 0.5rem;';
        userChip.innerHTML = `
            <a href="profile.html" style="text-decoration: none; display: inline-flex; align-items: center; gap: 0.5rem;" title="View and Edit Profile">
                <span style="font-size: 0.84rem; font-weight: 600; color: var(--text-main);">
                    ${escapeHtml(user.fullName)}
                </span>
                <span class="badge ${user.role === 'ADMIN' ? 'badge-gold' : 'badge-info'}" style="font-size: 0.7rem;">${escapeHtml(user.role)}</span>
            </a>
        `;
        headerActions.insertBefore(userChip, headerActions.firstChild);

        const logoutBtn = document.createElement('button');
        logoutBtn.id = 'header-logout-btn';
        logoutBtn.className = 'btn btn-outline btn-sm';
        logoutBtn.innerHTML = `
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
            Sign Out
        `;
        logoutBtn.onclick = logoutUser;
        headerActions.appendChild(logoutBtn);
    }
}

// -------------------------------------------------------------
// Active Sidebar Link Initializer & Auth Trigger
// -------------------------------------------------------------
document.addEventListener('DOMContentLoaded', async () => {
    // Verify session unless on public auth pages or profile page (which does role-agnostic checkAuth)
    const path = window.location.pathname;
    if (!path.endsWith('login.html') && !path.endsWith('signup.html') && !path.endsWith('member-dashboard.html') && !path.endsWith('profile.html')) {
        await checkAuth('ADMIN');
    }

    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-menu .nav-item a');
    
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (currentPath === href || 
            (currentPath.endsWith('/') && href === 'index.html') ||
            (currentPath.endsWith(href) && href !== 'index.html')) {
            link.parentElement.classList.add('active');
        } else {
            link.parentElement.classList.remove('active');
        }
    });

    // Mobile sidebar toggle
    const toggleBtn = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }

    // Initialize Dashboard if on index.html or root
    if (document.getElementById('dashboard-metrics')) {
        loadDashboardData();
    }
});

// -------------------------------------------------------------
// Dashboard Specific Loader
// -------------------------------------------------------------
async function loadDashboardData() {
    try {
        const stats = await apiRequest('/dashboard');
        
        // Update metric values
        document.getElementById('metric-members').textContent = stats.totalMembers;
        document.getElementById('metric-books').textContent = stats.totalBooks;
        document.getElementById('metric-copies').textContent = `${stats.availableCopies} / ${stats.totalCopies}`;
        document.getElementById('metric-issued').textContent = stats.issuedBooks;
        document.getElementById('metric-reservations').textContent = stats.activeReservations;
        document.getElementById('metric-overdue').textContent = stats.overdueBooks;

        // Render Recent Issues
        renderRecentIssues(stats.recentIssues || []);

        // Render Recent Returns
        renderRecentReturns(stats.recentReturns || []);

        // Render Popular Books
        renderPopularBooks(stats.popularBooks || []);

    } catch (err) {
        showToast('Failed to load dashboard statistics: ' + err.message, 'error');
    }
}

function renderRecentIssues(issues) {
    const tbody = document.getElementById('recent-issues-table');
    if (!tbody) return;

    if (issues.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No recent book loans recorded.</td></tr>`;
        return;
    }

    tbody.innerHTML = issues.map(iss => `
        <tr>
            <td><strong>#${iss.id}</strong></td>
            <td>
                <div style="font-weight: 600; color: var(--text-main);">${escapeHtml(iss.bookTitle)}</div>
                <small style="color: var(--text-muted);">ISBN: ${escapeHtml(iss.bookIsbn || '—')}</small>
            </td>
            <td>
                <div>${escapeHtml(iss.memberName)}</div>
                <small style="color: var(--text-muted);">${escapeHtml(iss.memberEmail)}</small>
            </td>
            <td>${formatDate(iss.issueDate)}</td>
            <td>
                <span class="badge ${iss.status === 'OVERDUE' ? 'badge-danger' : 'badge-info'}">
                    ${iss.status}
                </span>
            </td>
        </tr>
    `).join('');
}

function renderRecentReturns(returns) {
    const tbody = document.getElementById('recent-returns-table');
    if (!tbody) return;

    if (returns.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" class="empty-state">No returned books recorded yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = returns.map(ret => `
        <tr>
            <td>
                <div style="font-weight: 600; color: var(--text-main);">${escapeHtml(ret.bookTitle)}</div>
            </td>
            <td>${escapeHtml(ret.memberName)}</td>
            <td>${formatDate(ret.returnDate || ret.issueDate)}</td>
            <td><span class="badge badge-success">RETURNED</span></td>
        </tr>
    `).join('');
}

function renderPopularBooks(popular) {
    const container = document.getElementById('popular-books-list');
    if (!container) return;

    if (popular.length === 0) {
        container.innerHTML = `<div class="empty-state">No circulation statistics available yet.</div>`;
        return;
    }

    container.innerHTML = popular.map(item => `
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.85rem 0; border-bottom: 1px solid var(--border-light);">
            <div style="display: flex; align-items: center; gap: 0.85rem;">
                <div class="badge ${item.popularityRank === 1 ? 'badge-gold' : 'badge-navy'}" style="width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; font-weight: 700;">
                    #${item.popularityRank}
                </div>
                <div>
                    <div style="font-weight: 600; color: var(--text-main); font-size: 0.92rem;">${escapeHtml(item.title)}</div>
                    <small style="color: var(--text-muted);">${escapeHtml(item.author)} &bull; ${escapeHtml(item.category)}</small>
                </div>
            </div>
            <div style="text-align: right;">
                <span class="badge badge-gold" style="font-size: 0.78rem;">
                    ${item.borrowCount} loans
                </span>
            </div>
        </div>
    `).join('');
}
