const API_BASE = '';

// ---------- Navigation ----------
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        showView(link.dataset.view);
    });
});

document.getElementById('back-to-library').addEventListener('click', (e) => {
    e.preventDefault();
    showView('library');
});

function showView(viewName) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    const view = document.getElementById('view-' + viewName);
    if (view) view.classList.add('active');

    const navLink = document.querySelector(`.nav-link[data-view="${viewName}"]`);
    if (navLink) navLink.classList.add('active');

    if (viewName === 'dashboard') loadDashboard();
    if (viewName === 'library') loadLibrary();
}

// ---------- Dashboard ----------
async function loadDashboard() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard/today`);
        const data = await res.json();

        document.getElementById('stat-topics').textContent = data.totalTopics;
        document.getElementById('stat-total').textContent = data.totalQuestions;
        document.getElementById('stat-mastered').textContent = data.masteredCount;
        document.getElementById('stat-due').textContent = data.dueTodayCount;

        const queueEl = document.getElementById('review-queue');
        if (!data.dueToday || data.dueToday.length === 0) {
            queueEl.innerHTML = `<div class="empty-state">Nothing due today. Add a topic to get started, or come back tomorrow!</div>`;
            return;
        }

        queueEl.innerHTML = '';
        data.dueToday.forEach(q => {
            queueEl.appendChild(renderQuestionCard(q));
        });
    } catch (err) {
        console.error(err);
    }
}

// ---------- Create Topic ----------
document.getElementById('btn-generate').addEventListener('click', async () => {
    const title = document.getElementById('input-title').value.trim();
    const sourceText = document.getElementById('input-source').value.trim();
    const statusEl = document.getElementById('generate-status');
    const btn = document.getElementById('btn-generate');

    if (!sourceText) {
        statusEl.className = 'error';
        statusEl.textContent = 'Please paste some study material first.';
        return;
    }

    btn.disabled = true;
    statusEl.className = 'loading';
    statusEl.textContent = '✨ Generating your study kit with AI... this can take 10-20 seconds.';

    try {
        const res = await fetch(`${API_BASE}/api/topics`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, sourceText })
        });

        const data = await res.json();

        if (!res.ok) {
            statusEl.className = 'error';
            statusEl.textContent = '❌ ' + (data.error || 'Something went wrong.');
            btn.disabled = false;
            return;
        }

        statusEl.className = 'success';
        statusEl.textContent = '✅ Study kit generated! Opening it now...';
        document.getElementById('input-title').value = '';
        document.getElementById('input-source').value = '';

        setTimeout(() => {
            statusEl.textContent = '';
            openTopicDetail(data.id);
        }, 800);
    } catch (err) {
        statusEl.className = 'error';
        statusEl.textContent = '❌ Network error: ' + err.message;
    } finally {
        btn.disabled = false;
    }
});

// ---------- Library ----------
async function loadLibrary() {
    const listEl = document.getElementById('topic-list');
    listEl.innerHTML = '<div class="empty-state">Loading...</div>';

    try {
        const res = await fetch(`${API_BASE}/api/topics`);
        const topics = await res.json();

        if (topics.length === 0) {
            listEl.innerHTML = '<div class="empty-state">No topics yet. Click "New Topic" to create your first study kit.</div>';
            return;
        }

        listEl.innerHTML = '';
        topics.forEach(topic => {
            const card = document.createElement('div');
            card.className = 'topic-card';
            card.innerHTML = `
                <h3>${escapeHtml(topic.title)}</h3>
                <p>${escapeHtml((topic.summary || '').slice(0, 100))}${topic.summary && topic.summary.length > 100 ? '...' : ''}</p>
                <div class="meta">${topic.questions.length} questions · ${topic.flashcards.length} flashcards</div>
            `;
            card.addEventListener('click', () => openTopicDetail(topic.id));
            listEl.appendChild(card);
        });
    } catch (err) {
        listEl.innerHTML = '<div class="empty-state">Failed to load topics.</div>';
    }
}

// ---------- Topic Detail ----------
async function openTopicDetail(topicId) {
    showView('topic-detail');
    document.getElementById('detail-title').textContent = 'Loading...';
    document.getElementById('detail-summary').textContent = '';
    document.getElementById('quiz-container').innerHTML = '';
    document.getElementById('flashcard-container').innerHTML = '';

    try {
        const res = await fetch(`${API_BASE}/api/topics/${topicId}`);
        const topic = await res.json();

        document.getElementById('detail-title').textContent = topic.title;
        document.getElementById('detail-summary').textContent = topic.summary || 'No summary available.';

        const quizContainer = document.getElementById('quiz-container');
        quizContainer.innerHTML = '';
        topic.questions.forEach(q => quizContainer.appendChild(renderQuestionCard(q)));

        const flashcardContainer = document.getElementById('flashcard-container');
        flashcardContainer.innerHTML = '';
        topic.flashcards.forEach(card => {
            const el = document.createElement('div');
            el.className = 'flashcard';
            el.innerHTML = `
                <div class="card-front">${escapeHtml(card.front)}</div>
                <div class="card-back">${escapeHtml(card.back)}</div>
            `;
            el.addEventListener('click', () => el.classList.toggle('flipped'));
            flashcardContainer.appendChild(el);
        });
    } catch (err) {
        document.getElementById('detail-title').textContent = 'Failed to load topic';
    }
}

// ---------- Shared: Render a quiz question card ----------
function renderQuestionCard(q) {
    const card = document.createElement('div');
    card.className = 'question-card';

    const options = (q.optionsRaw || '').split('||').filter(o => o.trim() !== '');

    let dueBadge = '';
    if (q.nextReviewDate) {
        dueBadge = `<span class="badge due">Review: ${q.nextReviewDate}</span>`;
    }

    card.innerHTML = `<div class="q-text">${escapeHtml(q.questionText)} ${dueBadge}</div>`;

    const optionsWrap = document.createElement('div');
    options.forEach(opt => {
        const btn = document.createElement('button');
        btn.className = 'option-btn';
        btn.textContent = opt;
        btn.addEventListener('click', () => handleAnswerSubmit(q.id, opt, card, optionsWrap));
        optionsWrap.appendChild(btn);
    });
    card.appendChild(optionsWrap);

    return card;
}

async function handleAnswerSubmit(questionId, selectedOption, card, optionsWrap) {
    // Disable all options immediately to prevent double-submit
    optionsWrap.querySelectorAll('.option-btn').forEach(b => b.disabled = true);

    try {
        const res = await fetch(`${API_BASE}/api/quiz/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ questionId, submittedAnswer: selectedOption })
        });
        const result = await res.json();

        optionsWrap.querySelectorAll('.option-btn').forEach(btn => {
            if (btn.textContent === result.correctAnswer) {
                btn.classList.add('correct');
            } else if (btn.textContent === selectedOption && !result.correct) {
                btn.classList.add('incorrect');
            }
        });

        if (result.explanation) {
            const expl = document.createElement('div');
            expl.className = 'explanation';
            expl.textContent = (result.correct ? '✅ Correct! ' : '❌ Not quite. ') + result.explanation;
            card.appendChild(expl);
        }
    } catch (err) {
        console.error(err);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ---------- Init ----------
loadDashboard();
