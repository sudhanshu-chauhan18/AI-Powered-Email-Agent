// AI-Powered Email Agent - Frontend Logic

// Global Application State
const state = {
    activeTab: 'dashboard',
    drafts: [],
    sentEmails: [],
    recentActivity: [],
    activeDraft: null, // Currently selected draft in editor
    activeThreadId: null, // Currently selected thread ID
    activeThreadEmail: null, // Recipient email for the active thread
    threadMessages: [], // Messages in the active thread
    searchEmailResult: [] // Search result of conversations
};

// DOM Elements Cache
const DOM = {
    // Navigation
    menuItems: document.querySelectorAll('.menu-item'),
    panels: document.querySelectorAll('.tab-panel'),
    pageTitle: document.getElementById('page-title'),
    pageSubtitle: document.getElementById('page-subtitle'),
    btnRefreshData: document.getElementById('btn-refresh-data'),
    
    // Loader & Toasts
    globalLoader: document.getElementById('global-loader'),
    loaderText: document.getElementById('loader-text'),
    toastBox: document.getElementById('toast-box'),
    
    // Dashboard
    metricSentCount: document.getElementById('metric-sent-count'),
    metricDraftsCount: document.getElementById('metric-drafts-count'),
    recentActivityTbody: document.getElementById('recent-activity-tbody'),
    qaWriteEmail: document.getElementById('qa-write-email'),
    qaCheckDrafts: document.getElementById('qa-check-drafts'),
    btnViewAllActivity: document.getElementById('btn-view-all-activity'),
    
    // AI Generator
    genPrompt: document.getElementById('gen-prompt'),
    genEmail: document.getElementById('gen-email'),
    btnGenerateSave: document.getElementById('btn-generate-save'),
    previewBadge: document.getElementById('preview-badge'),
    previewPlaceholder: document.getElementById('preview-placeholder'),
    previewActual: document.getElementById('preview-actual'),
    previewSubject: document.getElementById('preview-subject'),
    previewBody: document.getElementById('preview-body'),
    suggestionChips: document.querySelectorAll('.chip-btn'),
    
    // Drafts Manager
    draftsCountLabel: document.getElementById('drafts-count-label'),
    draftsSearchInput: document.getElementById('drafts-search-input'),
    draftsListContainer: document.getElementById('drafts-list-container'),
    draftEditorPlaceholder: document.getElementById('draft-editor-placeholder'),
    draftEditorWorkspace: document.getElementById('draft-editor-workspace'),
    editorRecipient: document.getElementById('editor-recipient'),
    editorThreadId: document.getElementById('editor-thread-id'),
    editorSubject: document.getElementById('editor-subject'),
    editorBody: document.getElementById('editor-body'),
    refineInstructions: document.getElementById('refine-instructions'),
    btnRefineDraft: document.getElementById('btn-refine-draft'),
    btnDeleteDraft: document.getElementById('btn-delete-draft'),
    btnSaveDraftChanges: document.getElementById('btn-save-draft-changes'),
    btnSendDraft: document.getElementById('btn-send-draft'),
    draftsCountBadge: document.getElementById('drafts-count-badge'),
    

    
    // Sent History
    sentCountLabel: document.getElementById('sent-count-label'),
    sentSearchInput: document.getElementById('sent-search-input'),
    sentEmailsTbody: document.getElementById('sent-emails-tbody'),
    

};

// ==========================================
// API Helper Functions
// ==========================================

// Show/Hide Global Loading Overlay
function showLoader(show, text = 'AI Agent is thinking...') {
    if (DOM.globalLoader) {
        DOM.loaderText.textContent = text;
        DOM.globalLoader.style.display = show ? 'flex' : 'none';
    }
}

// Show Toast Notification
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let iconClass = 'fa-circle-check';
    if (type === 'error') iconClass = 'fa-circle-exclamation';
    if (type === 'warning') iconClass = 'fa-triangle-exclamation';
    if (type === 'info') iconClass = 'fa-circle-info';
    
    toast.innerHTML = `
        <i class="fa-solid ${iconClass} toast-icon"></i>
        <span class="toast-message">${message}</span>
    `;
    
    DOM.toastBox.appendChild(toast);
    
    // Slide out and remove after 4 seconds
    setTimeout(() => {
        toast.classList.add('leaving');
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, 4000);
}

// Unified Async Fetch Wrapper
async function apiRequest(url, options = {}) {
    const defaultHeaders = {
        'Content-Type': 'application/json'
    };
    
    options.headers = {
        ...defaultHeaders,
        ...options.headers
    };
    
    try {
        const response = await fetch(url, options);
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `API error ${response.status}`);
        }
        
        // Return JSON if present, otherwise text
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        return await response.text();
    } catch (error) {
        console.error(`Fetch error for ${url}:`, error);
        throw error;
    }
}

// ==========================================
// Core Data Syncing
// ==========================================

// Fetch drafts from backend
async function syncDrafts() {
    try {
        state.drafts = await apiRequest('/api/email/drafts');
        
        // Update badge count in sidebar
        const count = state.drafts.length;
        if (count > 0) {
            DOM.draftsCountBadge.textContent = count;
            DOM.draftsCountBadge.style.display = 'block';
        } else {
            DOM.draftsCountBadge.style.display = 'none';
        }
        
        DOM.metricDraftsCount.textContent = count;
        DOM.draftsCountLabel.textContent = `${count} Draft${count !== 1 ? 's' : ''}`;
    } catch (e) {
        showToast('Failed to load drafts', 'error');
    }
}

// Fetch sent emails from backend
async function syncSentEmails() {
    try {
        state.sentEmails = await apiRequest('/api/email/sent');
        const count = state.sentEmails.length;
        DOM.metricSentCount.textContent = count;
        DOM.sentCountLabel.textContent = `${count} Email${count !== 1 ? 's' : ''}`;
    } catch (e) {
        showToast('Failed to load sent history', 'error');
    }
}

// Refresh all dashboard metrics & tables
async function refreshAllData(silent = false) {
    if (!silent) showLoader(true, 'Syncing with database...');
    try {
        await Promise.all([syncDrafts(), syncSentEmails()]);
        
        // Populate recent activity
        // Combine drafts and sent, sort by timestamp
        const combined = [];
        state.drafts.forEach(d => combined.push({ ...d, type: 'Draft' }));
        state.sentEmails.forEach(s => combined.push({ ...s, type: 'Sent' }));
        
        combined.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        state.recentActivity = combined.slice(0, 5);
        
        renderRecentActivityTable();
        
        // If we are currently viewing lists, re-render them
        if (state.activeTab === 'drafts') renderDraftsList();
        if (state.activeTab === 'sent') renderSentEmailsTable();
        
        if (!silent) showToast('Data synchronized', 'success');
    } catch (e) {
        if (!silent) showToast('Sync failed', 'error');
    } finally {
        if (!silent) showLoader(false);
    }
}

// ==========================================
// Rendering Modules
// ==========================================

// Helper: Format Dates nicely
function formatDateTime(isoString) {
    if (!isoString) return 'N/A';
    const date = new Date(isoString);
    return date.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 1. Render Dashboard Recent Activity
function renderRecentActivityTable() {
    const tbody = DOM.recentActivityTbody;
    tbody.innerHTML = '';
    
    if (state.recentActivity.length === 0) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="5">No recent email activity recorded.</td>
            </tr>
        `;
        return;
    }
    
    state.recentActivity.forEach(activity => {
        const row = document.createElement('tr');
        
        // Status Pill Class
        let statusClass = 'draft';
        if (activity.status === 'sent') statusClass = 'sent';
        if (activity.status === 'failed') statusClass = 'failed';
        
        // Role Badge Class
        const roleClass = activity.role === 'agent' ? 'agent' : 'customer';
        
        row.innerHTML = `
            <td class="text-bold">${escapeHTML(activity.email)}</td>
            <td>${escapeHTML(activity.subject || '(No Subject)')}</td>
            <td><span class="role-pill ${roleClass}">${escapeHTML(activity.role)}</span></td>
            <td><span class="status-pill ${statusClass}">${escapeHTML(activity.status)}</span></td>
            <td>${formatDateTime(activity.timestamp)}</td>
        `;
        tbody.appendChild(row);
    });
}

// 2. Render Drafts List (Drafts Manager)
function renderDraftsList(filterText = '') {
    const container = DOM.draftsListContainer;
    container.innerHTML = '';
    
    const filtered = state.drafts.filter(draft => {
        const text = filterText.toLowerCase();
        return (
            (draft.email && draft.email.toLowerCase().includes(text)) ||
            (draft.subject && draft.subject.toLowerCase().includes(text)) ||
            (draft.message && draft.message.toLowerCase().includes(text))
        );
    });
    
    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-list-state">
                <i class="fa-solid fa-box-open"></i>
                <p>${filterText ? 'No matching drafts found.' : 'No drafts available. Use the AI Generator to write one!'}</p>
            </div>
        `;
        return;
    }
    
    filtered.forEach(draft => {
        const card = document.createElement('div');
        card.className = `list-item-card ${state.activeDraft && state.activeDraft.id === draft.id ? 'active' : ''}`;
        
        card.innerHTML = `
            <div class="item-card-header">
                <span class="item-card-title">${escapeHTML(draft.email || 'No Recipient')}</span>
                <span class="item-card-date">${formatDateTime(draft.timestamp)}</span>
            </div>
            <div class="item-card-subject">${escapeHTML(draft.subject || '(No Subject)')}</div>
            <div class="item-card-snippet">${escapeHTML(draft.message || '(Empty message)')}</div>
        `;
        
        card.addEventListener('click', () => {
            // Remove active class from previous
            document.querySelectorAll('#drafts-list-container .list-item-card').forEach(c => c.classList.remove('active'));
            card.classList.add('active');
            selectDraft(draft);
        });
        
        container.appendChild(card);
    });
}

// Load draft details into the editor on the right
function selectDraft(draft) {
    state.activeDraft = { ...draft }; // Clone to track edits
    
    DOM.draftEditorPlaceholder.style.display = 'none';
    DOM.draftEditorWorkspace.style.display = 'flex';
    
    DOM.editorRecipient.textContent = draft.email || 'Not specified';
    DOM.editorThreadId.textContent = draft.threadId || 'None';
    DOM.editorSubject.value = draft.subject || '';
    DOM.editorBody.value = draft.message || '';
    
    // Clear refine inputs
    DOM.refineInstructions.value = '';
}

// 3. Render Sent Emails List
function renderSentEmailsTable(filterText = '') {
    const tbody = DOM.sentEmailsTbody;
    tbody.innerHTML = '';
    
    const filtered = state.sentEmails.filter(email => {
        const text = filterText.toLowerCase();
        return (
            (email.email && email.email.toLowerCase().includes(text)) ||
            (email.subject && email.subject.toLowerCase().includes(text)) ||
            (email.message && email.message.toLowerCase().includes(text))
        );
    });
    
    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="5">No sent emails found.</td>
            </tr>
        `;
        return;
    }
    
    filtered.forEach(email => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="text-bold">${escapeHTML(email.email)}</td>
            <td class="text-bold">${escapeHTML(email.subject || '(No Subject)')}</td>
            <td><span class="item-card-snippet">${escapeHTML(email.message || '')}</span></td>
            <td><span class="status-pill sent">sent</span></td>
            <td>${formatDateTime(email.timestamp)}</td>
        `;
        tbody.appendChild(row);
    });
}




// ==========================================
// User Actions handlers (Event bindings)
// ==========================================

// Page Routing System
function switchTab(tabId) {
    state.activeTab = tabId;
    
    // Update sidebar navigation active state
    DOM.menuItems.forEach(item => {
        if (item.getAttribute('data-tab') === tabId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
    
    // Update active panel view
    DOM.panels.forEach(panel => {
        if (panel.id === `panel-${tabId}`) {
            panel.classList.add('active');
        } else {
            panel.classList.remove('active');
        }
    });
    
    // Update titles
    let titleText = 'Dashboard';
    let subtitleText = 'Overview of your automated email tasks';
    
    if (tabId === 'generator') {
        titleText = 'AI Email Writer';
        subtitleText = 'Ask Gemini / Groq to write custom emails';
    } else if (tabId === 'drafts') {
        titleText = 'Drafts Manager';
        subtitleText = 'Review, refine, and dispatch pending drafts';
        renderDraftsList();

    } else if (tabId === 'sent') {
        titleText = 'Sent Mail History';
        subtitleText = 'Review all dispatched emails';
        renderSentEmailsTable();
    }
    
    DOM.pageTitle.textContent = titleText;
    DOM.pageSubtitle.textContent = subtitleText;
}


// Generate and save directly to database as draft
async function handleGenerateAndSave() {
    const prompt = DOM.genPrompt.value.trim();
    const email = DOM.genEmail.value.trim();
    
    if (!prompt) {
        showToast('Please enter a prompt or instruction', 'warning');
        return;
    }
    if (!email) {
        showToast('Recipient email is required to save a draft', 'warning');
        return;
    }
    
    showLoader(true, 'AI is generating and saving draft...');
    try {
        const draft = await apiRequest('/api/email/generate-save', {
            method: 'POST',
            body: JSON.stringify({
                prompt,
                email
            })
        });
        
        showToast('Draft generated and saved! Preview is ready.', 'success');
        
        // Clear the prompt/email inputs
        DOM.genPrompt.value = '';
        DOM.genEmail.value = email; // keep email visible for context
        
        // Populate the preview panel fields
        DOM.previewSubject.textContent = draft.subject || '(No Subject)';
        DOM.previewBody.textContent = draft.body || '';

        // Force animation to replay even if preview was already visible:
        // hide → reflow → show triggers CSS @keyframes from scratch
        DOM.previewPlaceholder.style.display = 'none';
        DOM.previewActual.style.display = 'none';
        void DOM.previewActual.offsetHeight; // force browser reflow
        DOM.previewActual.style.display = 'flex';

        // Update badge with green "Draft Saved" pill
        DOM.previewBadge.className = 'preview-status'; // reset first
        void DOM.previewBadge.offsetHeight;             // reflow
        DOM.previewBadge.textContent = 'Draft Saved';
        DOM.previewBadge.className = 'preview-status ready';

        // Scroll the preview into view (helpful on smaller screens)
        DOM.previewActual.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        
        // Silently refresh data counts in background
        await refreshAllData(true);
    } catch (e) {
        showToast('Failed to save draft', 'error');
    } finally {
        showLoader(false);
    }
}


// 2. Drafts Manager Handlers

// AI Refinement of selected draft
async function handleRefineDraft() {
    if (!state.activeDraft) return;
    const instructions = DOM.refineInstructions.value.trim();
    
    if (!instructions) {
        showToast('Please enter refinement instructions', 'warning');
        return;
    }
    
    showLoader(true, 'AI is refining draft...');
    try {
        const updated = await apiRequest('/api/email/draft/refine', {
            method: 'POST',
            body: JSON.stringify({
                draftId: state.activeDraft.id,
                instructions
            })
        });
        
        // Update state and editor values
        state.activeDraft = updated;
        DOM.editorSubject.value = updated.subject;
        DOM.editorBody.value = updated.message;
        DOM.refineInstructions.value = '';
        
        showToast('Draft refined by AI!', 'success');
        
        // Sync drafts list
        await refreshAllData(true);
    } catch (e) {
        showToast('Refinement failed', 'error');
    } finally {
        showLoader(false);
    }
}

// Delete selected draft
async function handleDeleteDraft() {
    if (!state.activeDraft) return;
    
    // Safety check: ensure we have a valid database ID
    if (state.activeDraft.id === null || state.activeDraft.id === undefined) {
        showToast('Cannot delete: draft has no valid ID. Please refresh and try again.', 'error');
        console.error('Draft delete failed: activeDraft.id is', state.activeDraft.id, state.activeDraft);
        return;
    }
    
    if (!confirm('Are you sure you want to delete this draft?')) return;
    
    showLoader(true, 'Deleting draft...');
    try {
        await apiRequest(`/api/email/draft/${state.activeDraft.id}`, {
            method: 'DELETE'
        });
        
        showToast('Draft deleted', 'success');
        
        // Clear editor state
        state.activeDraft = null;
        DOM.draftEditorWorkspace.style.display = 'none';
        DOM.draftEditorPlaceholder.style.display = 'flex';
        
        await refreshAllData(true);
    } catch (e) {
        console.error('Delete draft error:', e);
        showToast('Failed to delete draft', 'error');
    } finally {
        showLoader(false);
    }
}

// Dispatch / Send Draft
async function handleSendDraft() {
    if (!state.activeDraft) return;
    
    const editedSubject = DOM.editorSubject.value.trim();
    const editedBody = DOM.editorBody.value.trim();
    
    // Check if the user made manual edits to the draft text in editor
    const isSubjectEdited = editedSubject !== state.activeDraft.subject;
    const isBodyEdited = editedBody !== state.activeDraft.message;
    
    showLoader(true, 'Sending draft...');
    try {
        if (isSubjectEdited || isBodyEdited) {
            // If the user made manual edits, send the custom email and then delete the old draft
            await apiRequest('/api/email/send', {
                method: 'POST',
                body: JSON.stringify({
                    to: state.activeDraft.email,
                    subject: editedSubject,
                    body: editedBody,
                    threadId: state.activeDraft.threadId
                })
            });
            
            // Delete the draft record
            await apiRequest(`/api/email/draft/${state.activeDraft.id}`, {
                method: 'DELETE'
            });
            
            showToast('Edited draft sent successfully!', 'success');
        } else {
            // If unmodified, use the standard sendDraft endpoint
            await apiRequest('/api/email/draft/send', {
                method: 'POST',
                body: JSON.stringify({
                    conversationId: state.activeDraft.id
                })
            });
            
            showToast('Draft sent successfully!', 'success');
        }
        
        // Clear editor workspace
        state.activeDraft = null;
        DOM.draftEditorWorkspace.style.display = 'none';
        DOM.draftEditorPlaceholder.style.display = 'flex';
        
        await refreshAllData(true);
    } catch (e) {
        showToast('Failed to send email', 'error');
    } finally {
        showLoader(false);
    }
}

// Save manual text changes to draft in DB
async function handleSaveDraftChanges() {
    if (!state.activeDraft) return;
    
    const subject = DOM.editorSubject.value.trim();
    const body = DOM.editorBody.value.trim();
    
    showLoader(true, 'Saving draft modifications...');
    try {
        // We use our custom save endpoint
        await apiRequest('/api/email/draft/save-custom', {
            method: 'POST',
            body: JSON.stringify({
                to: state.activeDraft.email,
                subject,
                body,
                threadId: state.activeDraft.threadId
            })
        });
        
        // Delete the old draft to replace it
        await apiRequest(`/api/email/draft/${state.activeDraft.id}`, {
            method: 'DELETE'
        });
        
        showToast('Draft changes saved!', 'success');
        state.activeDraft = null;
        DOM.draftEditorWorkspace.style.display = 'none';
        DOM.draftEditorPlaceholder.style.display = 'flex';
        
        await refreshAllData(true);
    } catch (e) {
        showToast('Failed to save manual changes', 'error');
    } finally {
        showLoader(false);
    }
}



// ==========================================
// Setup and Bindings
// ==========================================

// Safely escape HTML characters
function escapeHTML(str) {
    if (!str) return '';
    return str.replace(/[&<>'"]/g, 
        tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag)
    );
}

// Bind all button clicks & inputs
function bindEvents() {
    // 1. Navigation clicks
    DOM.menuItems.forEach(item => {
        item.addEventListener('click', () => {
            const tab = item.getAttribute('data-tab');
            switchTab(tab);
        });
    });
    
    // Sync Button
    DOM.btnRefreshData.addEventListener('click', () => refreshAllData(false));
    
    // 2. Dashboard Quick Actions
    DOM.qaWriteEmail.addEventListener('click', () => switchTab('generator'));
    DOM.qaCheckDrafts.addEventListener('click', () => switchTab('drafts'));
    DOM.btnViewAllActivity.addEventListener('click', () => switchTab('sent'));
    
    // 3. AI Generator Tab
    DOM.btnGenerateSave.addEventListener('click', handleGenerateAndSave);
    
    // Suggestion chips clicks
    DOM.suggestionChips.forEach(chip => {
        chip.addEventListener('click', () => {
            DOM.genPrompt.value = chip.textContent;
            showToast('Prompt loaded', 'info');
        });
    });
    
    // 4. Drafts Manager Tab
    DOM.btnRefineDraft.addEventListener('click', handleRefineDraft);
    DOM.btnDeleteDraft.addEventListener('click', handleDeleteDraft);
    DOM.btnSendDraft.addEventListener('click', handleSendDraft);
    DOM.btnSaveDraftChanges.addEventListener('click', handleSaveDraftChanges);
    
    // Local Drafts Search input
    DOM.draftsSearchInput.addEventListener('input', (e) => {
        renderDraftsList(e.target.value);
    });
    
    // 5. Sent History Tab
    DOM.sentSearchInput.addEventListener('input', (e) => {
        renderSentEmailsTable(e.target.value);
    });
}

// Initialise Application
async function init() {
    bindEvents();
    
    // Load initial counts and table records
    await refreshAllData(false);
    
    // Show Dashboard initially
    switchTab('dashboard');
}

// Start everything on load
window.addEventListener('DOMContentLoaded', init);
