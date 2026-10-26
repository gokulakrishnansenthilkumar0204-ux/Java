/**
 * Student Issue Management and Prioritization System - Frontend Logic
 * Vanilla JavaScript implementation for UUID handling, REST API integration,
 * dynamic search & filtering, voting, and 5-confirmation auto-resolution.
 */

(function () {
    'use strict';

    // State
    const STORAGE_KEY = 'student_browser_uuid';
    let currentBrowserId = '';
    let currentCategory = 'ALL';
    let currentStatus = 'ALL';
    let currentSort = 'votes';
    let searchQuery = '';
    let searchDebounceTimer = null;
    let latestIssuesRequestId = 0; // used to ignore out-of-date responses
    const DEFAULT_CONFIRMATIONS_REQUIRED = 5; // only used if the server does not send the value

    // DOM Elements
    const elements = {
        deviceIdDisplay: document.getElementById('current-device-id'),
        simulateNewDeviceBtn: document.getElementById('simulate-device-btn'),
        copyDeviceIdBtn: document.getElementById('copy-device-btn'),
        searchInput: document.getElementById('search-input'),
        searchBtn: document.getElementById('search-btn'),
        clearSearchBtn: document.getElementById('clear-search-btn'),
        categoryPills: document.querySelectorAll('.filter-pill[data-category]'),
        statusPills: document.querySelectorAll('.status-pill[data-status]'),
        sortButtons: document.querySelectorAll('.sort-btn[data-sort]'),
        issuesContainer: document.getElementById('issues-container'),
        issuesCountBadge: document.getElementById('issues-count-badge'),
        reportIssueModal: document.getElementById('report-issue-modal'),
        openReportModalBtn: document.getElementById('open-report-modal-btn'),
        closeReportModalBtn: document.getElementById('close-report-modal-btn'),
        cancelReportBtn: document.getElementById('cancel-report-btn'),
        reportIssueForm: document.getElementById('report-issue-form'),
        toastContainer: document.getElementById('toast-container'),
        // Stat counters
        statTotal: document.getElementById('stat-total'),
        statOpen: document.getElementById('stat-open'),
        statResolved: document.getElementById('stat-resolved'),
        statVotes: document.getElementById('stat-votes')
    };

    /**
     * Initialize Application
     */
    function init() {
        initBrowserIdentifier();

        // Initialize search query from input if prepopulated by Thymeleaf
        if (elements.searchInput && elements.searchInput.value) {
            searchQuery = elements.searchInput.value.trim();
            if (elements.clearSearchBtn) {
                elements.clearSearchBtn.style.display = searchQuery ? 'block' : 'none';
            }
        }

        setupEventListeners();
        // Server already rendered the cards; refresh silently so "Voted" states for this browser appear without flicker
        loadIssues({ showSpinner: false });
        loadStatistics();
    }

    /**
     * Requirement 7 & 8: Each browser receives a UUID stored in localStorage.
     */
    function initBrowserIdentifier() {
        let storedId = null;
        try {
            storedId = localStorage.getItem(STORAGE_KEY);
        } catch (e) {
            storedId = null;
        }
        if (!storedId || !isValidUUID(storedId)) {
            storedId = generateUUID();
            saveBrowserId(storedId);
        }
        currentBrowserId = storedId;
        updateDeviceIdDisplay();
    }

    function saveBrowserId(id) {
        try {
            localStorage.setItem(STORAGE_KEY, id);
        } catch (e) {
            // Storage blocked: the id still works for this page session
        }
    }

    function isValidUUID(value) {
        return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
    }

    function generateUUID() {
        if (typeof crypto !== 'undefined' && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        // Fallback RFC4122 v4 UUID generator
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    function updateDeviceIdDisplay() {
        if (elements.deviceIdDisplay) {
            const shortId = currentBrowserId.substring(0, 8) + '...' + currentBrowserId.substring(currentBrowserId.length - 4);
            elements.deviceIdDisplay.textContent = shortId;
            elements.deviceIdDisplay.title = 'Full UUID: ' + currentBrowserId;
        }
    }

    /**
     * Simulate New Device (Useful for evaluators to test duplicate prevention)
     */
    function simulateNewDevice() {
        const newId = generateUUID();
        saveBrowserId(newId);
        currentBrowserId = newId;
        updateDeviceIdDisplay();
        showToast('Generated new browser identifier! You can now test duplicate prevention.', 'info');
        loadIssues();
    }

    function copyDeviceId() {
        if (navigator.clipboard && currentBrowserId) {
            navigator.clipboard.writeText(currentBrowserId)
                .then(() => showToast('Browser UUID copied to clipboard!', 'success'))
                .catch(() => showToast(currentBrowserId, 'info'));
        } else {
            showToast(currentBrowserId, 'info');
        }
    }

    /**
     * Setup Event Listeners
     */
    function setupEventListeners() {
        // Device simulation
        if (elements.simulateNewDeviceBtn) {
            elements.simulateNewDeviceBtn.addEventListener('click', simulateNewDevice);
        }
        if (elements.copyDeviceIdBtn) {
            elements.copyDeviceIdBtn.addEventListener('click', copyDeviceId);
        }

        // Live Search with Debounce (Requirement 3)
        if (elements.searchInput) {
            elements.searchInput.addEventListener('input', function (e) {
                const val = e.target.value;
                if (elements.clearSearchBtn) {
                    elements.clearSearchBtn.style.display = val.trim() ? 'block' : 'none';
                }
                clearTimeout(searchDebounceTimer);
                searchDebounceTimer = setTimeout(() => {
                    searchQuery = elements.searchInput.value.trim();
                    loadIssues();
                }, 300);
            });

            // Enter key handler
            elements.searchInput.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    clearTimeout(searchDebounceTimer);
                    searchQuery = elements.searchInput.value.trim();
                    loadIssues();
                }
            });
        }

        // Explicit Search Button
        if (elements.searchBtn) {
            elements.searchBtn.addEventListener('click', function () {
                clearTimeout(searchDebounceTimer);
                if (elements.searchInput) {
                    searchQuery = elements.searchInput.value.trim();
                }
                loadIssues();
            });
        }

        // Clear Search Button
        if (elements.clearSearchBtn) {
            elements.clearSearchBtn.addEventListener('click', function () {
                if (elements.searchInput) {
                    elements.searchInput.value = '';
                    elements.searchInput.focus();
                }
                searchQuery = '';
                elements.clearSearchBtn.style.display = 'none';
                loadIssues();
            });
        }

        // Category Filter
        elements.categoryPills.forEach(pill => {
            pill.addEventListener('click', function () {
                elements.categoryPills.forEach(p => p.classList.remove('active'));
                this.classList.add('active');
                currentCategory = this.getAttribute('data-category');
                loadIssues();
            });
        });

        // Status Filter
        elements.statusPills.forEach(pill => {
            pill.addEventListener('click', function () {
                elements.statusPills.forEach(p => p.classList.remove('active'));
                this.classList.add('active');
                currentStatus = this.getAttribute('data-status');
                loadIssues();
            });
        });

        // Sort By Toggle (Requirement 5)
        elements.sortButtons.forEach(btn => {
            btn.addEventListener('click', function () {
                elements.sortButtons.forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                currentSort = this.getAttribute('data-sort');
                loadIssues();
            });
        });

        // Modal Controls
        if (elements.openReportModalBtn) {
            elements.openReportModalBtn.addEventListener('click', openModal);
        }
        if (elements.closeReportModalBtn) {
            elements.closeReportModalBtn.addEventListener('click', closeModal);
        }
        if (elements.cancelReportBtn) {
            elements.cancelReportBtn.addEventListener('click', closeModal);
        }
        if (elements.reportIssueModal) {
            elements.reportIssueModal.addEventListener('click', function (e) {
                if (e.target === elements.reportIssueModal) {
                    closeModal();
                }
            });
        }

        // Form Submission via AJAX REST API
        if (elements.reportIssueForm) {
            elements.reportIssueForm.addEventListener('submit', handleReportSubmit);
        }
    }

    function openModal() {
        if (elements.reportIssueModal) {
            elements.reportIssueModal.classList.add('open');
            document.body.style.overflow = 'hidden';
            const firstInput = elements.reportIssueModal.querySelector('input');
            if (firstInput) firstInput.focus();
        }
    }

    function closeModal() {
        if (elements.reportIssueModal) {
            elements.reportIssueModal.classList.remove('open');
            document.body.style.overflow = '';
            if (elements.reportIssueForm) {
                elements.reportIssueForm.reset();
            }
        }
    }

    /**
     * Requirement 1: Report new issue via REST API
     */
    function handleReportSubmit(e) {
        e.preventDefault();

        const title = document.getElementById('issue-title').value.trim();
        const category = document.getElementById('issue-category').value.trim();
        const location = document.getElementById('issue-location').value.trim();
        const description = document.getElementById('issue-description').value.trim();

        if (!title || !category || !location || !description) {
            showToast('Please fill in all required fields.', 'error');
            return;
        }

        const submitBtn = elements.reportIssueForm.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Submitting...';
        }

        const payload = { title, category, location, description };

        fetch(`/api/issues?browserId=${encodeURIComponent(currentBrowserId)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        .then(parseApiResponse)
        .then(res => {
            closeModal();
            showToast('Issue reported successfully!', 'success');
            loadIssues({ showSpinner: false });
            loadStatistics();
        })
        .catch(err => {
            showToast(err.message || 'Could not report issue.', 'error');
        })
        .finally(() => {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Submit Issue';
            }
        });
    }

    /**
     * Requirement 2, 3, 5: View all reported issues, search, and sort
     * Expected endpoints:
     * - List: GET /api/issues
     * - Search: GET /api/issues/search?keyword=KEYWORD
     */
    function loadIssues(options) {
        if (!elements.issuesContainer) return;

        const showSpinner = !(options && options.showSpinner === false);
        const requestId = ++latestIssuesRequestId;

        if (showSpinner) {
            elements.issuesContainer.innerHTML = `
                <div class="loading-state">
                    <div class="spinner"></div>
                    <p>Fetching campus issues...</p>
                </div>`;

            if (elements.issuesCountBadge) {
                elements.issuesCountBadge.textContent = 'Loading...';
            }
        }

        let url;
        const params = new URLSearchParams();

        if (searchQuery) {
            params.append('keyword', searchQuery);
            if (currentCategory && currentCategory !== 'ALL') params.append('category', currentCategory);
            if (currentStatus && currentStatus !== 'ALL') params.append('status', currentStatus);
            if (currentSort) params.append('sortBy', currentSort);
            if (currentBrowserId) params.append('browserId', currentBrowserId);
            url = `/api/issues/search?${params.toString()}`;
        } else {
            if (currentCategory && currentCategory !== 'ALL') params.append('category', currentCategory);
            if (currentStatus && currentStatus !== 'ALL') params.append('status', currentStatus);
            if (currentSort) params.append('sortBy', currentSort);
            if (currentBrowserId) params.append('browserId', currentBrowserId);
            const qs = params.toString();
            url = qs ? `/api/issues?${qs}` : '/api/issues';
        }

        fetch(url)
            .then(parseApiResponse)
            .then(res => {
                // A newer search/filter was started while this one was loading: ignore this old result
                if (requestId !== latestIssuesRequestId) return;
                if (res && res.success && Array.isArray(res.data)) {
                    renderIssues(res.data);
                } else {
                    renderErrorState(res ? res.message : 'Invalid response from server.');
                }
            })
            .catch(err => {
                if (requestId !== latestIssuesRequestId) return;
                console.error('Error fetching issues:', err);
                renderErrorState(err.message || 'Unable to connect to the backend database.');
            });
    }

    /**
     * Render Issue Cards
     */
    function renderIssues(issues) {
        if (!elements.issuesContainer) return;

        if (elements.issuesCountBadge) {
            const count = issues.length;
            elements.issuesCountBadge.textContent = count === 1 ? '1 issue displayed' : `${count} issues displayed`;
        }

        if (issues.length === 0) {
            renderEmptyState();
            return;
        }

        elements.issuesContainer.innerHTML = issues.map(issue => {
            const priorityClass = `priority-${(issue.priority || 'LOW').toLowerCase()}`;
            const badgePriorityClass = `badge-${(issue.priority || 'LOW').toLowerCase()}`;
            const isResolved = (issue.status || '').toUpperCase() === 'RESOLVED';
            const statusClass = isResolved ? 'badge-resolved' : 'badge-open';
            const statusText = isResolved ? 'Resolved ✓' : 'Open';

            const confirmations = issue.confirmationCount || 0;
            const required = issue.confirmationsRequired || DEFAULT_CONFIRMATIONS_REQUIRED;
            const progressPercent = Math.min(100, (confirmations / required) * 100);

            const hasVoted = issue.hasVoted === true;
            const hasConfirmed = issue.hasConfirmed === true;

            const timeAgo = formatTimeAgo(issue.createdAt);

            return `
                <div class="issue-card ${priorityClass}" id="issue-card-${issue.id}">
                    <div class="card-content">
                        <div class="card-header">
                            <div class="badge-group">
                                <span class="badge ${badgePriorityClass}">
                                    ${escapeHtml(issue.priority || 'LOW')}
                                </span>
                                <span class="badge ${statusClass}">
                                    ${escapeHtml(statusText)}
                                </span>
                            </div>
                            <span class="card-time">${timeAgo}</span>
                        </div>

                        <h3 class="issue-title">${escapeHtml(issue.title)}</h3>
                        <p class="issue-desc">${escapeHtml(issue.description)}</p>

                        <div class="meta-row">
                            <div class="meta-item">
                                <span>📁 <strong>${escapeHtml(issue.category)}</strong></span>
                            </div>
                            <div class="meta-item">
                                <span>📍 <strong>${escapeHtml(issue.location)}</strong></span>
                            </div>
                        </div>
                    </div>

                    <div class="card-actions">
                        <div class="action-row">
                            <button class="vote-btn ${hasVoted ? 'voted' : ''}" 
                                    id="vote-btn-${issue.id}" 
                                    onclick="window.StudentApp.vote(${issue.id})"
                                    ${hasVoted ? 'disabled' : ''}
                                    title="${hasVoted ? 'You have already voted' : 'Upvote this issue'}">
                                <span class="vote-icon">${hasVoted ? '✓' : '▲'}</span>
                                <span class="vote-label">${hasVoted ? 'Voted' : 'Upvote'}</span>
                                <span class="vote-count" id="vote-count-${issue.id}">(${issue.voteCount || 0})</span>
                            </button>

                            <span class="priority-label" style="font-size: 0.78rem; color: var(--text-muted);">
                                Priority: <strong style="color: var(--text-primary);">${escapeHtml(issue.priority || 'LOW')}</strong>
                            </span>
                        </div>

                        <div class="resolution-section">
                            <div class="resolution-header">
                                <span>Resolution Progress</span>
                                <strong id="confirmation-ratio-${issue.id}">${confirmations}/${required} Confirmations</strong>
                            </div>
                            <div class="progress-track">
                                <div class="progress-fill" id="progress-fill-${issue.id}" style="width: ${progressPercent}%;"></div>
                            </div>
                            
                            ${isResolved ? `
                                <button class="confirm-btn resolved" disabled>
                                    ✓ Issue Fully Resolved
                                </button>
                            ` : `
                                <button class="confirm-btn ${hasConfirmed ? 'confirmed' : ''}" 
                                        id="confirm-btn-${issue.id}"
                                        onclick="window.StudentApp.confirmResolution(${issue.id})"
                                        ${hasConfirmed ? 'disabled' : ''}
                                        title="${hasConfirmed ? 'Already confirmed from this browser' : 'Confirm that this issue is resolved'}">
                                    ${hasConfirmed ? '✓ Confirmed by you' : 'Confirm Resolution'}
                                </button>
                            `}
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    }

    function renderEmptyState() {
        if (!elements.issuesContainer) return;

        if (elements.issuesCountBadge) {
            elements.issuesCountBadge.textContent = '0 issues found';
        }

        const isFiltered = !!(searchQuery || (currentCategory && currentCategory !== 'ALL') || (currentStatus && currentStatus !== 'ALL'));

        elements.issuesContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">🔍</div>
                <h3>${isFiltered ? 'No matching issues found' : 'No issues reported yet'}</h3>
                <p>${isFiltered ? 'No campus issues match your current search or filter criteria. Try clearing filters or searching for different keywords.' : 'Be the first student to report a campus problem or infrastructure concern.'}</p>
                ${isFiltered ? `
                    <button class="btn btn-secondary" onclick="window.StudentApp.clearAllFilters()" type="button">
                        Reset Filters &amp; Search
                    </button>
                ` : `
                    <button class="btn btn-primary" onclick="window.StudentApp.openModal()" type="button">
                        + Report First Issue
                    </button>
                `}
            </div>`;
    }

    function renderErrorState(errorMessage) {
        if (!elements.issuesContainer) return;

        if (elements.issuesCountBadge) {
            elements.issuesCountBadge.textContent = 'Error loading issues';
        }

        elements.issuesContainer.innerHTML = `
            <div class="error-state">
                <div class="error-icon">⚠️</div>
                <h3>Unable to load issues</h3>
                <p>${escapeHtml(errorMessage || 'Please verify your database connection or try again.')}</p>
                <button class="btn btn-primary" onclick="window.StudentApp.retryLoad()" type="button">
                    🔄 Retry
                </button>
            </div>`;
    }

    /**
     * Requirement 4, 7, 8: Vote for existing issue & prevent duplicate votes
     */
    function vote(issueId) {
        if (!currentBrowserId) {
            initBrowserIdentifier();
        }

        const voteBtn = document.getElementById(`vote-btn-${issueId}`);
        if (voteBtn && voteBtn.disabled) return;
        if (voteBtn) voteBtn.disabled = true; // prevent double clicks while the request runs

        fetch(`/api/issues/${issueId}/vote`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ browserId: currentBrowserId })
        })
        .then(parseApiResponse)
        .then(response => {
            showToast('Vote recorded successfully! Priority recalculated.', 'success');
            loadIssues({ showSpinner: false });
            loadStatistics();
        })
        .catch(err => {
            showToast(err.message || 'Failed to record vote', 'error');
            if (voteBtn) voteBtn.disabled = false;
            loadIssues({ showSpinner: false });
        });
    }

    /**
     * Requirement 9 & 10: Confirm issue resolution (5 confirmations -> RESOLVED)
     */
    function confirmResolution(issueId) {
        if (!currentBrowserId) {
            initBrowserIdentifier();
        }

        const confirmBtn = document.getElementById(`confirm-btn-${issueId}`);
        if (confirmBtn && confirmBtn.disabled) return;
        if (confirmBtn) confirmBtn.disabled = true; // prevent double clicks while the request runs

        fetch(`/api/issues/${issueId}/confirm-resolution`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ browserId: currentBrowserId })
        })
        .then(parseApiResponse)
        .then(response => {
            const updated = response.data || {};
            const required = updated.confirmationsRequired || DEFAULT_CONFIRMATIONS_REQUIRED;
            if ((updated.status || '').toUpperCase() === 'RESOLVED') {
                showToast(`🎉 ${required} Confirmations reached! Issue has been marked as RESOLVED!`, 'success');
            } else {
                showToast(`Confirmation recorded (${updated.confirmationCount}/${required} required)`, 'info');
            }
            loadIssues({ showSpinner: false });
            loadStatistics();
        })
        .catch(err => {
            showToast(err.message || 'Failed to confirm resolution', 'error');
            if (confirmBtn) confirmBtn.disabled = false;
            loadIssues({ showSpinner: false });
        });
    }

    /**
     * Load Dashboard Statistics
     */
    function loadStatistics() {
        fetch('/api/issues/stats')
            .then(parseApiResponse)
            .then(res => {
                if (res && res.success && res.data) {
                    const stats = res.data;
                    if (elements.statTotal) elements.statTotal.textContent = stats.totalIssues || 0;
                    if (elements.statOpen) elements.statOpen.textContent = stats.openIssues || 0;
                    if (elements.statResolved) elements.statResolved.textContent = stats.resolvedIssues || 0;
                    if (elements.statVotes) elements.statVotes.textContent = stats.totalVotes || 0;
                }
            })
            .catch(err => console.error('Failed to load stats:', err));
    }

    /**
     * Clear all search & filter controls back to default
     */
    function clearAllFilters() {
        if (elements.searchInput) {
            elements.searchInput.value = '';
        }
        searchQuery = '';
        if (elements.clearSearchBtn) {
            elements.clearSearchBtn.style.display = 'none';
        }
        currentCategory = 'ALL';
        elements.categoryPills.forEach(p => {
            if (p.getAttribute('data-category') === 'ALL') p.classList.add('active');
            else p.classList.remove('active');
        });
        currentStatus = 'ALL';
        elements.statusPills.forEach(p => {
            if (p.getAttribute('data-status') === 'ALL') p.classList.add('active');
            else p.classList.remove('active');
        });
        loadIssues();
    }

    /**
     * Toast Notifications
     */
    function showToast(message, type = 'info') {
        if (!elements.toastContainer) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        let icon = 'ℹ️';
        if (type === 'success') icon = '✅';
        if (type === 'error') icon = '⚠️';

        toast.innerHTML = `<span>${icon}</span> <div>${escapeHtml(message)}</div>`;
        elements.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(15px)';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 3500);
    }

    /**
     * Helper Utilities
     */
    /**
     * Reads a fetch() Response as our ApiResponse JSON.
     * Throws an Error with the server's message for any non-2xx status,
     * and also copes with non-JSON error pages (e.g. proxy errors).
     */
    function parseApiResponse(res) {
        return res.text().then(text => {
            let data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch (e) {
                data = null;
            }
            if (!res.ok) {
                const message = (data && data.message) ? data.message : `Server returned error (${res.status})`;
                throw new Error(message);
            }
            if (!data) {
                throw new Error('Invalid response from server.');
            }
            return data;
        });
    }

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatTimeAgo(dateStr) {
        if (!dateStr) return 'Recently';
        try {
            const date = new Date(dateStr);
            const now = new Date();
            const diffSeconds = Math.floor((now - date) / 1000);

            if (diffSeconds < 60) return 'Just now';
            const diffMinutes = Math.floor(diffSeconds / 60);
            if (diffMinutes < 60) return `${diffMinutes}m ago`;
            const diffHours = Math.floor(diffMinutes / 60);
            if (diffHours < 24) return `${diffHours}h ago`;
            const diffDays = Math.floor(diffHours / 24);
            if (diffDays < 30) return `${diffDays}d ago`;
            return date.toLocaleDateString();
        } catch (e) {
            return 'Recently';
        }
    }

    // Expose global methods for inline HTML onclick handlers
    window.StudentApp = {
        vote,
        confirmResolution,
        simulateNewDevice,
        copyDeviceId,
        openModal,
        closeModal,
        clearAllFilters,
        retryLoad: function () { loadIssues(); }
    };

    // Run on DOM Ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
