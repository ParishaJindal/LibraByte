/**
 * LibraByte - Personal Profile & Account Security Controller
 * Manages profile viewing, updating, avatar upload, and PBKDF2 password change.
 * Compatible with both ADMIN and MEMBER accounts.
 */

let currentProfile = null;
let currentAvatarBase64 = null;

document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Authenticate with server - both ADMIN and MEMBER are permitted
        const user = await checkAuth(null);
        if (!user) return;

        // Adapt navigation based on role
        setupRoleNavigation(user);

        // Setup mobile menu
        setupMobileMenu();

        // Load profile data from API
        await loadProfile();

        // Setup avatar file input listener
        setupAvatarUploader();

    } catch (err) {
        console.error('Initialization error in profile:', err);
        showToast('Error loading profile: ' + err.message, 'error');
    }
});

function setupMobileMenu() {
    const toggleBtn = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }
}

function setupRoleNavigation(user) {
    const backBtn = document.getElementById('headerBackBtn');
    const brandLink = document.getElementById('brandHomeLink');
    const navDashboard = document.getElementById('dashboardNavLink');
    const navMembers = document.getElementById('navMembersItem');
    const navIssueReturn = document.getElementById('navIssueReturnItem');
    const navReports = document.getElementById('navReportsItem');

    if (user.role === 'MEMBER') {
        const dest = 'member-dashboard.html';
        if (backBtn) backBtn.href = dest;
        if (brandLink) brandLink.href = dest;
        if (navDashboard) {
            navDashboard.href = dest;
            navDashboard.innerHTML = `
                <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <rect x="3" y="3" width="7" height="7"></rect>
                    <rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect>
                    <rect x="3" y="14" width="7" height="7"></rect>
                </svg>
                Patron Dashboard
            `;
        }
        // Hide Admin-exclusive links in sidebar
        if (navMembers) navMembers.style.display = 'none';
        if (navReports) navReports.style.display = 'none';
        if (navIssueReturn) navIssueReturn.style.display = 'none';
    } else {
        const dest = 'index.html';
        if (backBtn) backBtn.href = dest;
        if (brandLink) brandLink.href = dest;
        if (navDashboard) navDashboard.href = dest;
    }
}

/**
 * Loads full profile details for the authenticated user from GET /api/profile
 */
async function loadProfile() {
    try {
        const profile = await apiRequest('/profile');
        currentProfile = profile;
        currentAvatarBase64 = profile.profileImage || '';

        // 1. Populate Hero Profile Card
        const heroName = document.getElementById('heroFullName');
        if (heroName) heroName.textContent = profile.fullName || 'User Profile';

        const roleBadge = document.getElementById('heroRoleBadge');
        if (roleBadge) {
            roleBadge.textContent = profile.role || 'USER';
            roleBadge.className = profile.role === 'ADMIN' ? 'badge badge-gold' : 'badge badge-info';
        }

        const statusBadge = document.getElementById('heroStatusBadge');
        if (statusBadge) statusBadge.textContent = profile.status || 'ACTIVE';

        const heroUserId = document.getElementById('heroUserId');
        if (heroUserId) heroUserId.textContent = '#' + profile.id;

        const memberWrap = document.getElementById('heroMemberIdWrap');
        const heroMemberId = document.getElementById('heroMemberId');
        if (profile.memberId) {
            if (memberWrap) memberWrap.style.display = 'inline-flex';
            if (heroMemberId) heroMemberId.textContent = '#' + profile.memberId;
        } else {
            if (memberWrap) memberWrap.style.display = 'none';
        }

        const heroCreatedAt = document.getElementById('heroCreatedAt');
        if (heroCreatedAt) heroCreatedAt.textContent = formatDate(profile.createdAt);

        // 2. Avatar Display
        renderAvatar(profile.fullName, profile.profileImage);

        // 3. View Mode Fields
        const viewName = document.getElementById('viewFullName');
        if (viewName) viewName.textContent = profile.fullName || '—';

        const viewEmail = document.getElementById('viewEmail');
        if (viewEmail) viewEmail.textContent = profile.email || '—';

        const viewPhone = document.getElementById('viewPhone');
        if (viewPhone) viewPhone.textContent = profile.phone || '—';

        const viewRole = document.getElementById('viewRole');
        if (viewRole) {
            viewRole.textContent = profile.role;
            viewRole.className = profile.role === 'ADMIN' ? 'badge badge-gold' : 'badge badge-info';
        }

        const viewMemberGroup = document.getElementById('viewMemberIdGroup');
        const viewMemberId = document.getElementById('viewMemberId');
        if (profile.memberId) {
            if (viewMemberGroup) viewMemberGroup.style.display = 'flex';
            if (viewMemberId) viewMemberId.textContent = 'PATRON #' + profile.memberId;
        } else {
            if (viewMemberGroup) viewMemberGroup.style.display = 'none';
        }

        // 4. Pre-fill Edit Form Inputs
        const editName = document.getElementById('editFullName');
        if (editName) editName.value = profile.fullName || '';

        const editEmail = document.getElementById('editEmail');
        if (editEmail) editEmail.value = profile.email || '';

        const editPhone = document.getElementById('editPhone');
        if (editPhone) editPhone.value = profile.phone || '';

        const editRole = document.getElementById('editRoleDisplay');
        if (editRole) editRole.value = profile.role || '';

        // 5. Sidebar Footer Sync
        const sName = document.getElementById('sidebarFooterName');
        if (sName) sName.textContent = profile.fullName;
        const sEmail = document.getElementById('sidebarFooterEmail');
        if (sEmail) sEmail.textContent = profile.email;
        const sRole = document.getElementById('sidebarFooterRole');
        if (sRole) {
            sRole.textContent = profile.role;
            sRole.className = profile.role === 'ADMIN' ? 'badge badge-gold' : 'badge badge-info';
        }

        // 6. Member-Exclusive Activity Section (Loans, Queue, History)
        const memberSec = document.getElementById('memberActivitySection');
        if (profile.role === 'MEMBER') {
            if (memberSec) {
                memberSec.style.display = 'flex';
                renderMemberLoans(profile.activeLoans || []);
                renderMemberReservations(profile.activeReservations || []);
                renderMemberHistory(profile.borrowingHistory || []);
            }
        } else {
            if (memberSec) memberSec.style.display = 'none';
        }

    } catch (err) {
        console.error('Error in loadProfile:', err);
        showToast('Failed to load profile details: ' + err.message, 'error');
    }
}

/**
 * Renders avatar initial or image
 */
function renderAvatar(fullName, imageUrl) {
    const avatarCircle = document.getElementById('avatarCircle');
    const avatarInitial = document.getElementById('avatarInitial');
    const avatarImage = document.getElementById('avatarImage');
    const removeBtn = document.getElementById('removePhotoBtn');

    if (!avatarCircle || !avatarInitial || !avatarImage) return;

    if (imageUrl && imageUrl.trim().length > 0) {
        avatarImage.src = imageUrl;
        avatarImage.style.display = 'block';
        avatarInitial.style.display = 'none';
        if (removeBtn) removeBtn.style.display = 'inline-block';
    } else {
        avatarImage.style.display = 'none';
        avatarInitial.style.display = 'block';
        const initial = (fullName && fullName.trim().length > 0) ? fullName.trim().charAt(0).toUpperCase() : 'U';
        avatarInitial.textContent = initial;
        if (removeBtn) removeBtn.style.display = 'none';
    }
}

/**
 * Handles avatar file upload and preview conversion to Base64
 */
function setupAvatarUploader() {
    const fileInput = document.getElementById('avatarFileInput');
    if (!fileInput) return;

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // Size check (max 2.5 MB)
        if (file.size > 2.5 * 1024 * 1024) {
            showToast('Selected image exceeds 2.5 MB. Please choose a smaller image.', 'error');
            fileInput.value = '';
            return;
        }

        // Format check
        if (!file.type.startsWith('image/')) {
            showToast('Please upload a valid image file (JPEG, PNG, WebP).', 'error');
            fileInput.value = '';
            return;
        }

        const reader = new FileReader();
        reader.onload = (event) => {
            currentAvatarBase64 = event.target.result;
            renderAvatar(currentProfile ? currentProfile.fullName : '', currentAvatarBase64);
            const fileNameLabel = document.getElementById('photoFileName');
            if (fileNameLabel) fileNameLabel.textContent = `Selected: ${file.name} (Ready to save)`;
            const removeBtn = document.getElementById('removePhotoBtn');
            if (removeBtn) removeBtn.style.display = 'inline-block';
            showToast('Photo selected! Click "Save Changes" to apply.', 'info');
        };
        reader.readAsDataURL(file);
    });
}

function removeProfilePhoto() {
    currentAvatarBase64 = '';
    const fileInput = document.getElementById('avatarFileInput');
    if (fileInput) fileInput.value = '';
    renderAvatar(currentProfile ? currentProfile.fullName : '', '');
    const fileNameLabel = document.getElementById('photoFileName');
    if (fileNameLabel) fileNameLabel.textContent = 'Photo removed (Click "Save Changes" to apply)';
    const removeBtn = document.getElementById('removePhotoBtn');
    if (removeBtn) removeBtn.style.display = 'none';
    showToast('Photo cleared. Click "Save Changes" to persist.', 'info');
}

/**
 * Toggles between View Mode and Edit Mode
 */
function toggleEditMode(showEdit) {
    const viewGroup = document.getElementById('profileViewMode');
    const editForm = document.getElementById('profileEditForm');
    const toggleBtn = document.getElementById('toggleEditBtn');

    if (showEdit) {
        if (viewGroup) viewGroup.style.display = 'none';
        if (editForm) editForm.style.display = 'flex';
        if (toggleBtn) toggleBtn.style.display = 'none';
    } else {
        if (viewGroup) viewGroup.style.display = 'flex';
        if (editForm) editForm.style.display = 'none';
        if (toggleBtn) toggleBtn.style.display = 'inline-flex';
        // Reset inputs to original loaded values
        if (currentProfile) {
            document.getElementById('editFullName').value = currentProfile.fullName || '';
            document.getElementById('editEmail').value = currentProfile.email || '';
            document.getElementById('editPhone').value = currentProfile.phone || '';
            currentAvatarBase64 = currentProfile.profileImage || '';
            renderAvatar(currentProfile.fullName, currentAvatarBase64);
        }
    }
}

/**
 * Validates Indian Phone Numbers:
 * Supports +91-98765-43210, +91 9876543210, 09876543210, 9876543210.
 * Must resolve to a 10-digit number starting with 6, 7, 8, or 9.
 */
function isValidIndianPhone(phone) {
    if (!phone) return false;
    const clean = phone.replace(/[^0-9]/g, '');
    if (clean.length === 10 && /^[6-9]\d{9}$/.test(clean)) return true;
    if (clean.length === 11 && clean.startsWith('0') && /^[6-9]\d{9}$/.test(clean.substring(1))) return true;
    if (clean.length === 12 && clean.startsWith('91') && /^[6-9]\d{9}$/.test(clean.substring(2))) return true;
    return false;
}

/**
 * Saves Personal Profile Information via PUT /api/profile
 */
async function handleProfileUpdate(event) {
    event.preventDefault();

    const fullNameInput = document.getElementById('editFullName');
    const emailInput = document.getElementById('editEmail');
    const phoneInput = document.getElementById('editPhone');
    const submitBtn = document.getElementById('saveProfileBtn');

    const fullName = fullNameInput ? fullNameInput.value.trim() : '';
    const email = emailInput ? emailInput.value.trim() : '';
    const phone = phoneInput ? phoneInput.value.trim() : '';

    // Validations
    if (fullName.length < 2) {
        showToast('Please enter a valid full name (at least 2 characters).', 'error');
        fullNameInput.focus();
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('Please enter a valid email address.', 'error');
        emailInput.focus();
        return;
    }

    if (!isValidIndianPhone(phone)) {
        showToast('Please enter a valid 10-digit Indian mobile number (e.g., +91-98765-43210 or 9876543210).', 'error');
        phoneInput.focus();
        return;
    }

    try {
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving...';
        }

        const payload = {
            fullName: fullName,
            email: email,
            phone: phone,
            profileImage: currentAvatarBase64 || ''
        };

        const updatedProfile = await apiRequest('/profile', {
            method: 'PUT',
            body: payload
        });

        currentProfile = updatedProfile;
        currentAvatarBase64 = updatedProfile.profileImage || '';

        showToast('Personal information updated successfully!', 'success');

        // Refresh UI
        await loadProfile();
        toggleEditMode(false);

    } catch (err) {
        console.error('Update profile error:', err);
        showToast(err.message || 'Failed to update profile.', 'error');
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Save Changes';
        }
    }
}

/**
 * Handles PBKDF2 Password Change via PUT /api/profile/password
 */
async function handleChangePassword(event) {
    event.preventDefault();

    const currentPwdInput = document.getElementById('currentPassword');
    const newPwdInput = document.getElementById('newPassword');
    const confirmPwdInput = document.getElementById('confirmNewPassword');
    const submitBtn = document.getElementById('changePasswordBtn');

    const currentPassword = currentPwdInput ? currentPwdInput.value : '';
    const newPassword = newPwdInput ? newPwdInput.value : '';
    const confirmNewPassword = confirmPwdInput ? confirmPwdInput.value : '';

    if (!currentPassword) {
        showToast('Please enter your current password.', 'error');
        currentPwdInput.focus();
        return;
    }

    if (newPassword.length < 6) {
        showToast('New password must be at least 6 characters long.', 'error');
        newPwdInput.focus();
        return;
    }

    if (newPassword === currentPassword) {
        showToast('New password must be different from your current password.', 'error');
        newPwdInput.focus();
        return;
    }

    if (newPassword !== confirmNewPassword) {
        showToast('New passwords do not match. Please re-check.', 'error');
        confirmPwdInput.focus();
        return;
    }

    try {
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Updating Password...';
        }

        const res = await apiRequest('/profile/password', {
            method: 'PUT',
            body: {
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmNewPassword
            }
        });

        showToast(res.message || 'Password changed successfully! Keep it secure.', 'success');

        // Clear password form
        if (currentPwdInput) currentPwdInput.value = '';
        if (newPwdInput) newPwdInput.value = '';
        if (confirmPwdInput) confirmPwdInput.value = '';

    } catch (err) {
        console.error('Password change error:', err);
        showToast(err.message || 'Failed to change password.', 'error');
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Update Password';
        }
    }
}

/**
 * Password Visibility Toggle
 */
function togglePasswordVisibility(inputId, eyeIconId) {
    const input = document.getElementById(inputId);
    const eyeIcon = document.getElementById(eyeIconId);
    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
        if (eyeIcon) {
            eyeIcon.innerHTML = `
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
            `;
        }
    } else {
        input.type = 'password';
        if (eyeIcon) {
            eyeIcon.innerHTML = `
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
            `;
        }
    }
}

/**
 * Render Member Active Loans Table
 */
function renderMemberLoans(loans) {
    const tbody = document.getElementById('memberActiveLoansBody');
    const badge = document.getElementById('activeLoansCountBadge');
    if (!tbody) return;

    if (badge) badge.textContent = `${loans.length} Book${loans.length === 1 ? '' : 's'}`;

    if (!loans || loans.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No active borrowed books right now.</td></tr>`;
        return;
    }

    tbody.innerHTML = loans.map(l => {
        const isOverdue = l.status === 'OVERDUE';
        const statusBadge = isOverdue 
            ? '<span class="badge badge-danger">OVERDUE</span>' 
            : '<span class="badge badge-success">ACTIVE</span>';

        return `
            <tr>
                <td><strong>#${l.id}</strong></td>
                <td>
                    <div style="font-weight: 600; color: var(--forest-green);">${escapeHtml(l.bookTitle || 'Book #' + l.bookId)}</div>
                    <small style="color: var(--text-muted);">${escapeHtml(l.isbn || '')}</small>
                </td>
                <td>${formatDate(l.issueDate)}</td>
                <td style="${isOverdue ? 'color: #c0392b; font-weight: 600;' : ''}">${formatDate(l.dueDate)}</td>
                <td>${statusBadge}</td>
            </tr>
        `;
    }).join('');
}

/**
 * Render Member Active Reservations Queue Table
 */
function renderMemberReservations(reservations) {
    const tbody = document.getElementById('memberActiveReservationsBody');
    const badge = document.getElementById('activeReservationsCountBadge');
    if (!tbody) return;

    if (badge) badge.textContent = `${reservations.length} in Queue`;

    if (!reservations || reservations.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No active reservations in queue.</td></tr>`;
        return;
    }

    tbody.innerHTML = reservations.map(r => {
        let statusBadge = '<span class="badge badge-gold">WAITING</span>';
        if (r.status === 'ALLOCATED') {
            statusBadge = '<span class="badge badge-success">ALLOCATED</span>';
        }

        return `
            <tr>
                <td><strong>#${r.id}</strong></td>
                <td>
                    <div style="font-weight: 600; color: var(--forest-green);">${escapeHtml(r.bookTitle || 'Book #' + r.bookId)}</div>
                    <small style="color: var(--text-muted);">Queue Priority: ${r.priority}</small>
                </td>
                <td>${formatDate(r.reservationDate)}</td>
                <td>
                    <span class="badge badge-secondary" style="font-size: 0.75rem;">FIFO Position #${r.priority}</span>
                </td>
                <td>${statusBadge}</td>
            </tr>
        `;
    }).join('');
}

/**
 * Render Past Borrowing History Table
 */
function renderMemberHistory(history) {
    const tbody = document.getElementById('memberHistoryBody');
    const badge = document.getElementById('historyCountBadge');
    if (!tbody) return;

    if (badge) badge.textContent = `${history.length} Returned`;

    if (!history || history.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No past returns recorded.</td></tr>`;
        return;
    }

    tbody.innerHTML = history.map(h => {
        return `
            <tr>
                <td><strong>#${h.id}</strong></td>
                <td>
                    <div style="font-weight: 600; color: var(--text-main);">${escapeHtml(h.bookTitle || 'Book #' + h.bookId)}</div>
                </td>
                <td>${formatDate(h.issueDate)}</td>
                <td>${formatDate(h.returnDate)}</td>
                <td><span class="badge badge-secondary">RETURNED</span></td>
            </tr>
        `;
    }).join('');
}
