/* ═══════════════════════════════════════════════════════════════
   doc-query — app.js
   ═══════════════════════════════════════════════════════════════ */

// === CONFIGURATION ===

const BASE_URL = 'http://localhost:8080';
const POLLING_INTERVAL_MS = 3000;

// === STATE ===

const state = {
  token: localStorage.getItem('dq_token'),
  userEmail: localStorage.getItem('dq_email'),
  documents: [],
  selectedDocumentId: null,
  pollingIntervals: new Map(), // documentId → intervalId
  isStreaming: false,
  sidebarOpen: false,
};

// === INITIALIZATION ===

document.addEventListener('DOMContentLoaded', () => {
  lucide.createIcons();

  if (state.token) {
    showApp();
  } else {
    showAuthOverlay();
  }

  bindAuthEvents();
  bindAppEvents();
});

// ═══════════════════════════════════════════════════════════════
// === AUTH ===
// ═══════════════════════════════════════════════════════════════

function bindAuthEvents() {
  document.querySelectorAll('.auth-tab').forEach((tab) => {
    tab.addEventListener('click', () => switchAuthTab(tab.dataset.tab));
  });

  document.getElementById('login-form').addEventListener('submit', handleLogin);
  document.getElementById('register-form').addEventListener('submit', handleRegister);
  document.getElementById('logout-btn').addEventListener('click', handleLogout);
}

function switchAuthTab(target) {
  document.querySelectorAll('.auth-tab').forEach((t) => {
    t.classList.toggle('active', t.dataset.tab === target);
    t.setAttribute('aria-selected', t.dataset.tab === target);
  });
  document.getElementById('login-form').classList.toggle('hidden', target !== 'login');
  document.getElementById('register-form').classList.toggle('hidden', target !== 'register');
}

async function handleLogin(event) {
  event.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  const btn = document.getElementById('login-btn');

  setButtonLoading(btn, true, 'Signing in…');
  try {
    const data = await apiPost('/auth/login', { email, password }, false);
    state.token = data.token;
    state.userEmail = email;
    localStorage.setItem('dq_token', data.token);
    localStorage.setItem('dq_email', email);
    showApp();
  } catch (_err) {
    showToast('Invalid credentials', 'error');
  } finally {
    setButtonLoading(btn, false, 'Sign in');
  }
}

async function handleRegister(event) {
  event.preventDefault();
  const email = document.getElementById('register-email').value.trim();
  const password = document.getElementById('register-password').value;
  const btn = document.getElementById('register-btn');

  setButtonLoading(btn, true, 'Creating account…');
  try {
    await apiPost('/auth/register', { email, password }, false);
    showToast('Account created! Please sign in.', 'success');
    switchAuthTab('login');
    document.getElementById('login-email').value = email;
  } catch (_err) {
    showToast('Registration failed. Email may already be in use.', 'error');
  } finally {
    setButtonLoading(btn, false, 'Create account');
  }
}

function handleLogout() {
  state.pollingIntervals.forEach((id) => clearInterval(id));
  state.pollingIntervals.clear();
  state.token = null;
  state.userEmail = null;
  state.selectedDocumentId = null;
  state.documents = [];
  localStorage.removeItem('dq_token');
  localStorage.removeItem('dq_email');
  showAuthOverlay();
}

// ═══════════════════════════════════════════════════════════════
// === API ===
// ═══════════════════════════════════════════════════════════════

async function apiRequest(method, path, body, authenticated) {
  const headers = { 'Content-Type': 'application/json' };
  if (authenticated && state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  const options = { method, headers };
  if (body !== null && body !== undefined) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(BASE_URL + path, options);

  if (response.status === 401 && authenticated) {
    handleLogout();
    throw new Error('Session expired');
  }

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const contentType = response.headers.get('Content-Type') || '';
  if (contentType.includes('application/json') || contentType.includes('text/plain')) {
    return response.json();
  }
  return response;
}

async function apiGet(path) {
  return apiRequest('GET', path, null, true);
}

async function apiPost(path, body, authenticated = true) {
  return apiRequest('POST', path, body, authenticated);
}

async function apiDelete(path) {
  return apiRequest('DELETE', path, null, true);
}

async function apiPostMultipart(path, formData) {
  const headers = {};
  if (state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  const response = await fetch(BASE_URL + path, { method: 'POST', headers, body: formData });

  if (response.status === 401) {
    handleLogout();
    throw new Error('Session expired');
  }
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

// ═══════════════════════════════════════════════════════════════
// === DOCUMENTS ===
// ═══════════════════════════════════════════════════════════════

async function loadDocuments() {
  try {
    const docs = await apiGet('/documents');
    state.documents = Array.isArray(docs) ? docs : [];
    renderDocumentList();
  } catch (_err) {
    showToast('Failed to load documents', 'error');
  }
}

function renderDocumentList() {
  const container = document.getElementById('documents-list');
  const countEl = document.getElementById('doc-count');
  countEl.textContent = state.documents.length;

  if (state.documents.length === 0) {
    container.innerHTML = `
      <div class="empty-list">
        <i data-lucide="files"></i>
        <p>No documents yet</p>
      </div>`;
    lucide.createIcons({ nodes: [container] });
    return;
  }

  container.innerHTML = state.documents.map((doc) => renderDocumentItem(doc)).join('');
  lucide.createIcons({ nodes: [container] });

  container.querySelectorAll('.doc-item').forEach((item) => {
    const id = item.dataset.id;
    const status = item.dataset.status;

    item.addEventListener('click', (e) => {
      if (e.target.closest('.doc-delete-btn')) return;
      if (status === 'INDEXED') {
        selectDocument(id);
        if (state.sidebarOpen) closeSidebar();
      }
    });

    item.addEventListener('keydown', (e) => {
      if ((e.key === 'Enter' || e.key === ' ') && status === 'INDEXED') {
        e.preventDefault();
        selectDocument(id);
        if (state.sidebarOpen) closeSidebar();
      }
    });
  });

  container.querySelectorAll('.doc-delete-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      deleteDocument(btn.dataset.id);
    });
  });
}

function renderDocumentItem(doc) {
  const isSelected = doc.id === state.selectedDocumentId;
  const isClickable = doc.status === 'INDEXED';
  const date = doc.createdAt
    ? new Date(doc.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    : '';

  return `
    <div class="doc-item${isSelected ? ' selected' : ''}${isClickable ? ' clickable' : ''}"
         data-id="${doc.id}"
         data-status="${doc.status}"
         role="button"
         tabindex="${isClickable ? '0' : '-1'}"
         aria-label="${escapeHtml(doc.fileName)}, status: ${doc.status}">
      <div class="doc-item-header">
        <i data-lucide="file-text" class="doc-icon"></i>
        <div class="doc-info">
          <span class="doc-name" title="${escapeHtml(doc.fileName)}">${escapeHtml(doc.fileName)}</span>
          <span class="doc-date">${date}</span>
        </div>
        <button class="doc-delete-btn" data-id="${doc.id}" aria-label="Delete ${escapeHtml(doc.fileName)}">
          <i data-lucide="trash-2"></i>
        </button>
      </div>
      <div class="doc-status">
        <span class="status-badge badge-${doc.status.toLowerCase()}">${formatStatus(doc.status)}</span>
      </div>
    </div>`;
}

async function deleteDocument(documentId) {
  try {
    await apiDelete(`/documents/${documentId}`);

    if (state.pollingIntervals.has(documentId)) {
      clearInterval(state.pollingIntervals.get(documentId));
      state.pollingIntervals.delete(documentId);
    }

    if (state.selectedDocumentId === documentId) {
      state.selectedDocumentId = null;
      showEmptyState();
    }

    state.documents = state.documents.filter((d) => d.id !== documentId);
    renderDocumentList();
    showToast('Document deleted', 'success');
  } catch (_err) {
    showToast('Failed to delete document', 'error');
  }
}

async function selectDocument(documentId) {
  state.selectedDocumentId = documentId;
  renderDocumentList();

  const doc = state.documents.find((d) => d.id === documentId);
  document.getElementById('chat-document-name').textContent = doc ? doc.fileName : '';

  showChatArea();
  clearMessages();

  try {
    const history = await apiGet(`/documents/${documentId}/history`);
    if (Array.isArray(history) && history.length > 0) {
      renderHistory(history);
    }
  } catch (_err) {
    showToast('Failed to load conversation history', 'error');
  }
}

// ── UPLOAD ──────────────────────────────────────────────────────

function bindUploadEvents() {
  const uploadArea = document.getElementById('upload-area');
  const fileInput = document.getElementById('file-input');

  uploadArea.addEventListener('click', (e) => {
    if (e.target === fileInput) return;
    fileInput.click();
  });

  uploadArea.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      fileInput.click();
    }
  });

  uploadArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadArea.classList.add('drag-over');
  });

  uploadArea.addEventListener('dragleave', (e) => {
    if (!uploadArea.contains(e.relatedTarget)) {
      uploadArea.classList.remove('drag-over');
    }
  });

  uploadArea.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadArea.classList.remove('drag-over');
    const file = e.dataTransfer.files[0];
    if (file) uploadFile(file);
  });

  fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (file) uploadFile(file);
    fileInput.value = '';
  });
}

async function uploadFile(file) {
  if (file.type !== 'application/pdf') {
    showToast('Only PDF files are supported', 'error');
    return;
  }

  showUploadProgress(file.name, 'Uploading…');

  const formData = new FormData();
  formData.append('file', file);

  try {
    const documentId = await apiPostMultipart('/documents', formData);
    startPolling(documentId, file.name);
  } catch (_err) {
    hideUploadProgress();
    showToast('Upload failed', 'error');
  }
}

function startPolling(documentId, fileName) {
  // Add placeholder entry immediately so it shows in the list
  const placeholder = {
    id: documentId,
    fileName,
    fileSizeBytes: 0,
    status: 'UPLOADED',
    createdAt: new Date().toISOString(),
    indexedAt: null,
  };
  state.documents = [placeholder, ...state.documents.filter((d) => d.id !== documentId)];
  renderDocumentList();

  const intervalId = setInterval(async () => {
    try {
      const doc = await apiGet(`/documents/${documentId}`);

      // Update the entry in the local list
      const idx = state.documents.findIndex((d) => d.id === documentId);
      if (idx >= 0) {
        state.documents[idx] = { ...state.documents[idx], ...doc };
      }
      renderDocumentList();
      setProgressStatus(doc.status);

      if (doc.status === 'INDEXED') {
        clearInterval(state.pollingIntervals.get(documentId));
        state.pollingIntervals.delete(documentId);
        hideUploadProgress();
        showToast(`"${doc.fileName}" is ready to chat!`, 'success');
      } else if (doc.status === 'FAILED') {
        clearInterval(state.pollingIntervals.get(documentId));
        state.pollingIntervals.delete(documentId);
        hideUploadProgress();
        showToast(`Processing failed for "${doc.fileName}"`, 'error');
      }
    } catch (_err) {
      // Polling errors are ignored silently
    }
  }, POLLING_INTERVAL_MS);

  state.pollingIntervals.set(documentId, intervalId);
}

// ═══════════════════════════════════════════════════════════════
// === CHAT ===
// ═══════════════════════════════════════════════════════════════

async function sendMessage() {
  if (state.isStreaming || !state.selectedDocumentId) return;

  const input = document.getElementById('chat-input');
  const question = input.value.trim();
  if (!question) return;

  input.value = '';
  resizeTextarea(input);

  appendUserMessage(question);

  const thinkingId = appendThinkingMessage();
  state.isStreaming = true;
  setSendButtonState(true);

  try {
    const response = await fetch(`${BASE_URL}/documents/${state.selectedDocumentId}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`,
      },
      body: JSON.stringify({ question }),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    removeThinkingMessage(thinkingId);
    const assistantMsgId = appendAssistantMessage('');

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // Process complete SSE lines
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      for (const rawLine of lines) {
        const line = rawLine.replace(/\r$/, ''); // strip trailing \r (CRLF safety)
        if (line.startsWith('data:')) {
          const token = line.slice(5); // preserve spaces — they are part of LLM tokens
          if (token.trim() !== '[DONE]' && token.length > 0) {
            appendTokenToMessage(assistantMsgId, token);
          }
        }
      }
    }

    // Flush any remaining data in buffer
    if (buffer.startsWith('data:')) {
      const token = buffer.replace(/\r$/, '').slice(5);
      if (token.trim() !== '[DONE]' && token.length > 0) {
        appendTokenToMessage(assistantMsgId, token);
      }
    }

  } catch (_err) {
    removeThinkingMessage(thinkingId);
    showToast('Failed to send message', 'error');
  } finally {
    state.isStreaming = false;
    setSendButtonState(false);
  }
}

async function clearHistory() {
  if (!state.selectedDocumentId) return;
  try {
    await apiDelete(`/documents/${state.selectedDocumentId}/history`);
    clearMessages();
    showToast('History cleared', 'success');
  } catch (_err) {
    showToast('Failed to clear history', 'error');
  }
}

function renderHistory(history) {
  history.forEach((entry) => {
    appendUserMessage(entry.question);
    appendAssistantMessage(entry.answer);
  });
}

// ═══════════════════════════════════════════════════════════════
// === UI — MESSAGES ===
// ═══════════════════════════════════════════════════════════════

let messageCounter = 0;

function appendUserMessage(text) {
  const id = `msg-${++messageCounter}`;
  const div = document.createElement('div');
  div.className = 'message message-user';
  div.id = id;
  div.innerHTML = `<div class="message-bubble">${escapeHtml(text)}</div>`;
  document.getElementById('messages').appendChild(div);
  scrollToBottom();
  return id;
}

function appendAssistantMessage(text) {
  const id = `msg-${++messageCounter}`;
  const div = document.createElement('div');
  div.className = 'message message-assistant';
  div.id = id;
  div.innerHTML = `<div class="message-bubble"><span class="message-text">${escapeHtml(text)}</span></div>`;
  document.getElementById('messages').appendChild(div);
  scrollToBottom();
  return id;
}

function appendThinkingMessage() {
  const id = `msg-${++messageCounter}`;
  const div = document.createElement('div');
  div.className = 'message message-assistant';
  div.id = id;
  div.innerHTML = `
    <div class="message-bubble">
      <div class="thinking" aria-label="Thinking">
        <span class="thinking-dot"></span>
        <span class="thinking-dot"></span>
        <span class="thinking-dot"></span>
      </div>
    </div>`;
  document.getElementById('messages').appendChild(div);
  scrollToBottom();
  return id;
}

function removeThinkingMessage(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
}

function appendTokenToMessage(messageId, token) {
  const el = document.getElementById(messageId);
  if (!el) return;
  const textEl = el.querySelector('.message-text');
  if (textEl) {
    textEl.textContent += token;
    scrollToBottom();
  }
}

function clearMessages() {
  document.getElementById('messages').innerHTML = '';
  messageCounter = 0;
}

function scrollToBottom() {
  const messages = document.getElementById('messages');
  messages.scrollTop = messages.scrollHeight;
}

// ═══════════════════════════════════════════════════════════════
// === UI — LAYOUT ===
// ═══════════════════════════════════════════════════════════════

function showAuthOverlay() {
  document.getElementById('auth-overlay').classList.remove('hidden');
  document.getElementById('app').classList.add('hidden');
}

function showApp() {
  document.getElementById('auth-overlay').classList.add('hidden');
  document.getElementById('app').classList.remove('hidden');
  document.getElementById('user-email').textContent = state.userEmail || '';
  lucide.createIcons();
  bindUploadEvents();
  loadDocuments();
}

function showEmptyState() {
  document.getElementById('empty-state').classList.remove('hidden');
  document.getElementById('chat-area').classList.add('hidden');
}

function showChatArea() {
  document.getElementById('empty-state').classList.add('hidden');
  document.getElementById('chat-area').classList.remove('hidden');
}

function showUploadProgress(fileName, statusText) {
  document.getElementById('upload-area').classList.add('hidden');
  document.getElementById('upload-progress').classList.remove('hidden');
  document.getElementById('upload-filename').textContent = fileName;
  document.getElementById('upload-status-text').textContent = statusText;
  setProgressFill(20);
}

function hideUploadProgress() {
  document.getElementById('upload-area').classList.remove('hidden');
  document.getElementById('upload-progress').classList.add('hidden');
  setProgressFill(0);
}

function setProgressFill(percent) {
  document.getElementById('progress-fill').style.width = `${percent}%`;
  document.getElementById('progress-fill').closest('[role="progressbar"]')
    ?.setAttribute('aria-valuenow', percent);
}

function setProgressStatus(status) {
  const labels = {
    UPLOADED: 'Uploaded',
    PARSING: 'Parsing…',
    PARSED: 'Parsed',
    INDEXING: 'Indexing…',
    INDEXED: 'Indexed',
    FAILED: 'Failed',
  };
  const pcts = { UPLOADED: 20, PARSING: 45, PARSED: 65, INDEXING: 82, INDEXED: 100, FAILED: 100 };
  document.getElementById('upload-status-text').textContent = labels[status] || status;
  setProgressFill(pcts[status] || 50);
}

function setSendButtonState(disabled) {
  document.getElementById('send-btn').disabled = disabled;
}

// ── SIDEBAR (responsive) ─────────────────────────────────────

function bindSidebarToggle() {
  document.getElementById('hamburger-btn').addEventListener('click', toggleSidebar);
  document.getElementById('sidebar-overlay').addEventListener('click', closeSidebar);
}

function toggleSidebar() {
  state.sidebarOpen = !state.sidebarOpen;
  document.getElementById('sidebar').classList.toggle('open', state.sidebarOpen);
  document.getElementById('sidebar-overlay').classList.toggle('hidden', !state.sidebarOpen);
  document.getElementById('sidebar-overlay').setAttribute('aria-hidden', !state.sidebarOpen);
}

function closeSidebar() {
  state.sidebarOpen = false;
  document.getElementById('sidebar').classList.remove('open');
  document.getElementById('sidebar-overlay').classList.add('hidden');
  document.getElementById('sidebar-overlay').setAttribute('aria-hidden', true);
}

// ═══════════════════════════════════════════════════════════════
// === UI — TOAST ===
// ═══════════════════════════════════════════════════════════════

function showToast(message, type = 'info') {
  const iconName = type === 'success' ? 'check-circle' : type === 'error' ? 'alert-circle' : 'info';
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<i data-lucide="${iconName}"></i><span>${escapeHtml(message)}</span>`;
  document.getElementById('toast-container').appendChild(toast);
  lucide.createIcons({ nodes: [toast] });

  requestAnimationFrame(() => {
    requestAnimationFrame(() => toast.classList.add('visible'));
  });

  setTimeout(() => {
    toast.classList.remove('visible');
    setTimeout(() => toast.remove(), 350);
  }, 3500);
}

// ═══════════════════════════════════════════════════════════════
// === APP EVENTS ===
// ═══════════════════════════════════════════════════════════════

function bindAppEvents() {
  // Send message on button click
  document.getElementById('send-btn').addEventListener('click', sendMessage);

  // Send on Enter (new line on Shift+Enter)
  document.getElementById('chat-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  // Auto-resize textarea
  document.getElementById('chat-input').addEventListener('input', (e) => {
    resizeTextarea(e.target);
  });

  // Clear history
  document.getElementById('clear-history-btn').addEventListener('click', clearHistory);

  // Sidebar toggle (responsive)
  bindSidebarToggle();

  // Show/hide hamburger based on screen width
  const mediaQuery = window.matchMedia('(max-width: 1024px)');
  const applyMediaQuery = (mq) => {
    document.getElementById('hamburger-btn').classList.toggle('hidden', !mq.matches);
    if (!mq.matches) closeSidebar();
  };
  applyMediaQuery(mediaQuery);
  mediaQuery.addEventListener('change', applyMediaQuery);
}

// ═══════════════════════════════════════════════════════════════
// === HELPERS ===
// ═══════════════════════════════════════════════════════════════

function escapeHtml(str) {
  const el = document.createElement('span');
  el.appendChild(document.createTextNode(String(str)));
  return el.innerHTML;
}

function formatStatus(status) {
  const labels = {
    UPLOADED: 'Uploaded',
    PARSING:  'Parsing',
    PARSED:   'Parsed',
    INDEXING: 'Indexing',
    INDEXED:  'Indexed',
    FAILED:   'Failed',
  };
  return labels[status] || status;
}

function resizeTextarea(el) {
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
}

function setButtonLoading(btn, loading, label) {
  btn.disabled = loading;
  const span = btn.querySelector('.btn-label');
  if (span) span.textContent = label;
}
