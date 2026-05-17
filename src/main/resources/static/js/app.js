'use strict';

// ── State ──────────────────────────────────────────────
let lastAiSuggestion   = null;
let editingTaskId      = null;
let originalEditDate   = null; // "YYYY-MM-DDTHH:MM" — skip future-check if date unchanged
let pendingDeleteId    = null;
let pendingDeleteTitle = null;
let successDismissTimer  = null;
let successDismissTarget = null;

let currentTasks          = [];
let currentLoadController = null;
let currentFilters = { status: '', priority: '', sortBy: 'createdAt', sortOrder: 'desc', search: '' };

// ── Init ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    loadTasks();

    bindInputClear('title', 'description', 'dueDate');
    bindInputClear('edit-title', 'edit-description', 'edit-dueDate');
    document.getElementById('ai-desc').addEventListener('input', () => clearFieldError('ai-desc'));

    // Blur: validate immediately when leaving a field
    document.getElementById('title')?.addEventListener('blur', () => validateField('', 'title', null));
    document.getElementById('description')?.addEventListener('blur', () => validateField('', 'description', null));
    document.getElementById('dueDate')?.addEventListener('blur', () => validateField('', 'dueDate', null));
    document.getElementById('edit-title')?.addEventListener('blur', () => validateField('edit', 'title', null));
    document.getElementById('edit-description')?.addEventListener('blur', () => validateField('edit', 'description', null));
    document.getElementById('edit-dueDate')?.addEventListener('blur', () => validateField('edit', 'dueDate', originalEditDate));

    // Enter key in search input triggers search
    document.getElementById('filter-search').addEventListener('keydown', e => {
        if (e.key === 'Enter') applySearch();
    });

    // Close modals when clicking the backdrop
    document.getElementById('edit-modal').addEventListener('click', e => {
        if (e.target === e.currentTarget) closeModal();
    });
    document.getElementById('confirm-modal').addEventListener('click', e => {
        if (e.target === e.currentTarget) closeConfirmModal();
    });
    document.getElementById('view-modal').addEventListener('click', e => {
        if (e.target === e.currentTarget) closeViewModal();
    });

    // Edit / Delete via event delegation on the table body
    document.getElementById('task-body').addEventListener('click', e => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const id     = parseInt(btn.dataset.id, 10);
        const action = btn.dataset.action;
        if (action === 'view')      viewTask(id);
        if (action === 'edit')      openEditModal(id);
        if (action === 'delete')    deleteTask(id, btn.dataset.title);
        if (action === 'breakdown') breakdownTask(id);
    });
});

function bindInputClear(...ids) {
    ids.forEach(id =>
        document.getElementById(id)?.addEventListener('input', () => clearFieldError(id)));
}

// ── Field-error helpers ────────────────────────────────
function showFieldError(id, message) {
    const el = document.getElementById('err-' + id);
    if (el) el.textContent = message;
}

function clearFieldError(id) {
    const el = document.getElementById('err-' + id);
    if (el) el.textContent = '';
}

function clearFormErrors(prefix) {
    const p = prefix ? prefix + '-' : '';
    ['title', 'description', 'dueDate'].forEach(f => clearFieldError(p + f));
}

// ── Validation ─────────────────────────────────────────
function validateField(prefix, field, originalDueDate) {
    const p = prefix ? prefix + '-' : '';
    let valid = true;

    if (field === 'title') {
        const v = document.getElementById(p + 'title').value.trim();
        if (!v) {
            showFieldError(p + 'title', 'Title is required.');
            valid = false;
        } else if (v.length > 200) {
            showFieldError(p + 'title', `Title cannot exceed 200 characters (currently: ${v.length}).`);
            valid = false;
        } else {
            clearFieldError(p + 'title');
        }
    }

    if (field === 'description') {
        const el = document.getElementById(p + 'description');
        if (el) {
            const v = el.value.trim();
            if (v.length > 1000) {
                showFieldError(p + 'description', `Description cannot exceed 1000 characters (currently: ${v.length}).`);
                valid = false;
            } else {
                clearFieldError(p + 'description');
            }
        }
    }

    if (field === 'dueDate') {
        const v = document.getElementById(p + 'dueDate').value;
        if (!v) {
            showFieldError(p + 'dueDate', 'Due date is required.');
            valid = false;
        } else {
            const changed = originalDueDate === null || v !== originalDueDate;
            if (changed && new Date(v) <= new Date()) {
                showFieldError(p + 'dueDate', 'Due date must be in the future.');
                valid = false;
            } else {
                clearFieldError(p + 'dueDate');
            }
        }
    }

    return valid;
}

// originalDueDate: null = always require future; string = skip check if date unchanged (edit)
function validateForm(prefix, originalDueDate = null) {
    let valid = true;
    if (!validateField(prefix, 'title',       originalDueDate)) valid = false;
    if (!validateField(prefix, 'description', originalDueDate)) valid = false;
    if (!validateField(prefix, 'dueDate',     originalDueDate)) valid = false;
    return valid;
}

// ── Task List ──────────────────────────────────────────
async function loadTasks() {
    if (currentLoadController) currentLoadController.abort();
    currentLoadController = new AbortController();
    const signal = currentLoadController.signal;
    try {
        const params = new URLSearchParams();
        if (currentFilters.status)    params.set('status',    currentFilters.status);
        if (currentFilters.priority)  params.set('priority',  currentFilters.priority);
        if (currentFilters.sortBy)    params.set('sortBy',    currentFilters.sortBy);
        if (currentFilters.sortOrder) params.set('sortOrder', currentFilters.sortOrder);
        if (currentFilters.search)    params.set('search',    currentFilters.search);

        const query = params.toString() ? '?' + params.toString() : '';
        const res   = await fetch('/tasks' + query, { signal });
        const tasks = await res.json();
        currentTasks = tasks;
        renderTasks(tasks);
    } catch (err) {
        if (err.name === 'AbortError') return;
        showAlert('task-msg', 'err', 'Failed to load tasks: ' + err.message);
    }
}

function applySearch() {
    currentFilters.search = document.getElementById('filter-search').value.trim();
    loadTasks();
}

function clearSearch() {
    currentFilters.search = '';
    document.getElementById('filter-search').value = '';
    loadTasks();
}

function applyFilters() {
    currentFilters.status    = document.getElementById('filter-status').value;
    currentFilters.priority  = document.getElementById('filter-priority').value;
    currentFilters.sortBy    = document.getElementById('filter-sortBy').value;
    currentFilters.sortOrder = document.getElementById('filter-sortOrder').value;
    loadTasks();
}

function clearFilters() {
    currentFilters.status    = '';
    currentFilters.priority  = '';
    currentFilters.sortBy    = 'createdAt';
    currentFilters.sortOrder = 'desc';
    document.getElementById('filter-status').value    = '';
    document.getElementById('filter-priority').value  = '';
    document.getElementById('filter-sortBy').value    = 'createdAt';
    document.getElementById('filter-sortOrder').value = 'desc';
    loadTasks();
}

function renderTasks(tasks) {
    const tbody = document.getElementById('task-body');
    tbody.innerHTML = '';

    if (!tasks.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="8">No tasks yet. Create one below.</td></tr>';
        return;
    }

    tasks.forEach(t => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${esc(String(t.id))}</td>
            <td>${esc(t.title)}</td>
            <td class="col-desc" title="${attr(t.description || '')}">${esc(t.description || '')}</td>
            <td>${fmtDate(t.dueDate)}</td>
            <td><span class="badge badge-${attr(t.priority)}">${esc(t.priority)}</span></td>
            <td><span class="task-status status-${attr(t.status)}">${esc(t.status.replace('_', ' '))}</span></td>
            <td class="col-created">${fmtDate(t.createdAt)}</td>
            <td class="col-actions">
                <div class="action-btns">
                    <button class="btn btn-sm btn-secondary"
                            data-action="view" data-id="${t.id}">View</button>
                    <button class="btn btn-sm btn-edit"
                            data-action="edit" data-id="${t.id}">Edit</button>
                    <button class="btn btn-sm btn-delete"
                            data-action="delete" data-id="${t.id}"
                            data-title="${attr(t.title)}">Delete</button>
                    <button class="btn btn-sm btn-primary"
                            data-action="breakdown" data-id="${t.id}">AI Breakdown</button>
                </div>
            </td>`;
        tbody.appendChild(tr);
    });
}

// ── Create Task ────────────────────────────────────────
async function createTask(event) {
    event.preventDefault();
    clearAlert('task-msg');
    clearAlert('create-msg');
    clearFormErrors('');

    if (!validateForm('')) return;

    const payload = {
        title:       document.getElementById('title').value.trim(),
        description: document.getElementById('description').value.trim() || null,
        dueDate:     document.getElementById('dueDate').value,
        priority:    document.getElementById('priority').value,
        status:      document.getElementById('status').value
    };

    try {
        const res  = await fetch('/tasks', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (res.ok) {
            showAlert('create-msg', 'ok', 'Task created (ID: ' + data.id + ')');
            document.getElementById('create-form').reset();
            clearFormErrors('');
            loadTasks();
        } else {
            showAlert('create-msg', 'err', formatApiError(data));
        }
    } catch (err) {
        showAlert('create-msg', 'err', 'Request failed: ' + err.message);
    }
}

function resetCreateForm() {
    document.getElementById('create-form').reset();
    clearAlert('create-msg');
    clearFormErrors('');
}

// ── Edit Task ──────────────────────────────────────────
async function openEditModal(id) {
    editingTaskId    = id;
    originalEditDate = null;
    clearFormErrors('edit');
    clearAlert('edit-form-msg');

    try {
        const res = await fetch('/tasks/' + id);
        if (!res.ok) throw new Error('Task not found');
        const task = await res.json();

        originalEditDate = task.dueDate ? task.dueDate.substring(0, 16) : '';

        document.getElementById('edit-title').value       = task.title || '';
        document.getElementById('edit-description').value = task.description || '';
        document.getElementById('edit-dueDate').value     = originalEditDate;
        document.getElementById('edit-priority').value    = task.priority || 'MEDIUM';
        document.getElementById('edit-status').value      = task.status   || 'TODO';

        document.getElementById('edit-modal').showModal();
    } catch (err) {
        showAlert('task-msg', 'err', 'Could not open task for editing: ' + err.message);
    }
}

async function saveEdit(event) {
    event.preventDefault();
    clearFormErrors('edit');

    if (!validateForm('edit', originalEditDate)) return;

    const payload = {
        title:       document.getElementById('edit-title').value.trim(),
        description: document.getElementById('edit-description').value.trim() || null,
        dueDate:     document.getElementById('edit-dueDate').value,
        priority:    document.getElementById('edit-priority').value,
        status:      document.getElementById('edit-status').value
    };

    try {
        const res  = await fetch('/tasks/' + editingTaskId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (res.ok) {
            closeModal();
            showAlert('task-msg', 'ok', 'Task updated successfully.');
            loadTasks();
        } else {
            showAlert('edit-form-msg', 'err', formatApiError(data));
        }
    } catch (err) {
        showAlert('edit-form-msg', 'err', 'Request failed: ' + err.message);
    }
}

function closeModal() {
    document.getElementById('edit-modal').close();
    editingTaskId    = null;
    originalEditDate = null;
}

// ── Delete Task ────────────────────────────────────────
function deleteTask(id, title) {
    clearAllAlerts();
    pendingDeleteId    = id;
    pendingDeleteTitle = title;
    document.getElementById('confirm-msg').textContent =
        `Are you sure you want to delete "${title}"? This action cannot be undone.`;
    document.getElementById('confirm-modal').showModal();
}

async function confirmDelete() {
    if (!pendingDeleteId) return;
    const id    = pendingDeleteId;
    const title = pendingDeleteTitle;
    closeConfirmModal();

    try {
        const res = await fetch('/tasks/' + id, { method: 'DELETE' });
        if (res.ok) {
            showAlert('task-msg', 'ok', `Task "${title}" deleted.`);
            loadTasks();
        } else {
            const data = await res.json();
            showAlert('task-msg', 'err', formatApiError(data));
        }
    } catch (err) {
        showAlert('task-msg', 'err', 'Request failed: ' + err.message);
    }
}

function closeConfirmModal() {
    document.getElementById('confirm-modal').close();
    pendingDeleteId    = null;
    pendingDeleteTitle = null;
}

// ── View Task ──────────────────────────────────────────
function viewTask(id) {
    const task = currentTasks.find(t => t.id === id);
    if (!task) return;

    document.getElementById('view-modal-content').innerHTML =
        '<p><strong>Title:</strong> '       + esc(task.title)              + '</p>' +
        '<p><strong>Description:</strong> ' + esc(task.description || '—') + '</p>' +
        '<p><strong>Due Date:</strong> '    + esc(fmtDate(task.dueDate))   + '</p>' +
        '<p><strong>Priority:</strong> <span class="badge badge-' + attr(task.priority) + '">'
            + esc(task.priority) + '</span></p>' +
        '<p><strong>Status:</strong> <span class="task-status status-' + attr(task.status) + '">'
            + esc(task.status.replace('_', ' ')) + '</span></p>' +
        '<p><strong>Created:</strong> '  + esc(fmtDate(task.createdAt)) + '</p>' +
        '<p><strong>Updated:</strong> '  + esc(fmtDate(task.updatedAt)) + '</p>';

    document.getElementById('view-modal').showModal();
}

function closeViewModal() {
    document.getElementById('view-modal').close();
}

// ── AI Breakdown ───────────────────────────────────────
async function breakdownTask(id) {
    const container = document.getElementById('breakdown-result');
    container.innerHTML = '<p class="breakdown-loading">Generating AI breakdown…</p>';
    container.style.display = 'block';

    try {
        const res  = await fetch('/tasks/' + id + '/breakdown', { method: 'POST' });
        const data = await res.json();

        if (!res.ok) {
            container.innerHTML =
                '<p class="breakdown-error">Failed to generate AI breakdown: ' +
                esc(data.message || 'Unknown error') + '</p>';
            return;
        }

        const subtasks  = data.suggestedSubtasks || [];
        const reasoning = data.reasoning         || '';

        container.innerHTML =
            '<h3>AI Task Breakdown</h3>' +
            '<p><strong>Original Task:</strong> ' + esc(data.originalTask.title) + '</p>' +
            '<p><strong>AI Reasoning:</strong> '  + esc(reasoning) + '</p>' +
            '<h4>Suggested Subtasks:</h4>' +
            '<ul>' +
            subtasks.map(s =>
                '<li><strong>' + esc(s.title) + '</strong> – ' + esc(s.priority) + '<br>' +
                esc(s.description || '') + '</li>'
            ).join('') +
            '</ul>';
    } catch (err) {
        container.innerHTML =
            '<p class="breakdown-error">Failed to generate AI breakdown: ' + esc(err.message) + '</p>';
    }
}

// ── AI Suggestion ──────────────────────────────────────
async function suggestTask() {
    const desc     = document.getElementById('ai-desc').value.trim();
    const resultEl = document.getElementById('ai-result');
    clearAlert('ai-msg');
    clearFieldError('ai-desc');
    resultEl.style.display = 'none';
    lastAiSuggestion = null;

    if (!desc) {
        showFieldError('ai-desc', 'Description is required.');
        return;
    }

    showAlert('ai-msg', 'info', 'Asking AI…');

    try {
        const res  = await fetch('/tasks/suggest', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ description: desc })
        });
        const data = await res.json();
        if (res.ok) {
            clearAlert('ai-msg');
            lastAiSuggestion = data.suggestedTask;
            document.getElementById('ai-result-text').textContent = JSON.stringify(data, null, 2);
            resultEl.style.display = 'block';
        } else {
            showAlert('ai-msg', 'err', formatApiError(data));
        }
    } catch (err) {
        showAlert('ai-msg', 'err', 'Request failed: ' + err.message);
    }
}

function useAiSuggestion() {
    if (!lastAiSuggestion) return;
    const s = lastAiSuggestion;
    document.getElementById('title').value       = s.title       || '';
    document.getElementById('description').value = s.description || '';
    // datetime-local needs "YYYY-MM-DDTHH:MM"; API returns "YYYY-MM-DDTHH:MM:SS"
    document.getElementById('dueDate').value     = s.dueDate ? s.dueDate.substring(0, 16) : '';
    document.getElementById('priority').value    = s.priority || 'MEDIUM';
    document.getElementById('status').value      = s.status   || 'TODO';
    clearAlert('create-msg');
    clearFormErrors('');
    document.getElementById('ai-result').style.display = 'none';
    document.getElementById('create-section').scrollIntoView({ behavior: 'smooth' });
}

// ── Alert helpers ──────────────────────────────────────
function showAlert(id, type, text) {
    // Cancel existing timer if it targets the same element
    if (successDismissTimer && successDismissTarget === id) {
        clearTimeout(successDismissTimer);
        successDismissTimer  = null;
        successDismissTarget = null;
    }
    const el = document.getElementById(id);
    if (!el) return;
    el.className = 'alert' +
        (type === 'ok'   ? ' alert-ok'   :
         type === 'err'  ? ' alert-err'  :
         type === 'info' ? ' alert-info' : '');
    el.textContent = text;
    // Only success messages auto-dismiss; errors and info stay until action taken
    if (type === 'ok') {
        successDismissTarget = id;
        successDismissTimer  = setTimeout(() => {
            clearAlert(id);
            successDismissTimer  = null;
            successDismissTarget = null;
        }, 5000);
    }
}

function clearAlert(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.className   = 'alert';
    el.textContent = '';
}

function clearAllAlerts() {
    if (successDismissTimer) {
        clearTimeout(successDismissTimer);
        successDismissTimer  = null;
        successDismissTarget = null;
    }
    ['task-msg', 'create-msg', 'ai-msg', 'edit-form-msg'].forEach(id => clearAlert(id));
}

// ── API error formatter ────────────────────────────────
function formatApiError(data) {
    if (data.errors) {
        return 'Validation errors — ' + Object.entries(data.errors)
            .map(([f, m]) => f + ': ' + m).join(' | ');
    }
    return data.message || 'An unexpected error occurred.';
}

// ── DOM / string utils ─────────────────────────────────
function fmtDate(iso) {
    if (!iso) return '';

    const date = new Date(iso);

    return date.toLocaleString([], {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit'
    });
}

// Safe for element text content
function esc(text) {
    const d = document.createElement('div');
    d.appendChild(document.createTextNode(String(text)));
    return d.innerHTML;
}

// Safe for double-quoted HTML attribute values
function attr(text) {
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}
