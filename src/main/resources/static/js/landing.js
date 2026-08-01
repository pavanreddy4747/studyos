const API_BASE = '';

// Check if already logged in — if so, skip straight to the app
(async function checkAuth() {
    try {
        const res = await fetch(`${API_BASE}/api/auth/me`);
        if (res.ok) {
            window.location.href = '/app.html';
        }
    } catch (err) {
        // not logged in, stay on landing page
    }
})();

const modal = document.getElementById('auth-modal');

function openModal(tab) {
    modal.classList.add('active');
    showAuthTab(tab);
}
function closeModal() {
    modal.classList.remove('active');
}

function showAuthTab(tab) {
    document.getElementById('auth-tab-login').classList.toggle('active', tab === 'login');
    document.getElementById('auth-tab-signup').classList.toggle('active', tab === 'signup');
    document.getElementById('login-error').textContent = '';
    document.getElementById('signup-error').textContent = '';
}

document.getElementById('nav-login-link').addEventListener('click', (e) => { e.preventDefault(); openModal('login'); });
document.getElementById('nav-signup-link').addEventListener('click', (e) => { e.preventDefault(); openModal('signup'); });
document.getElementById('hero-get-started').addEventListener('click', () => openModal('signup'));
document.getElementById('auth-modal-close').addEventListener('click', closeModal);
document.getElementById('switch-to-signup').addEventListener('click', (e) => { e.preventDefault(); showAuthTab('signup'); });
document.getElementById('switch-to-login').addEventListener('click', (e) => { e.preventDefault(); showAuthTab('login'); });

modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
});

document.getElementById('btn-login').addEventListener('click', async () => {
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');
    errorEl.textContent = '';

    if (!email || !password) {
        errorEl.textContent = 'Please enter both email and password.';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();

        if (!res.ok) {
            errorEl.textContent = data.error || 'Login failed.';
            return;
        }

        window.location.href = '/app.html';
    } catch (err) {
        errorEl.textContent = 'Network error: ' + err.message;
    }
});

document.getElementById('btn-signup').addEventListener('click', async () => {
    const displayName = document.getElementById('signup-name').value.trim();
    const email = document.getElementById('signup-email').value.trim();
    const password = document.getElementById('signup-password').value;
    const errorEl = document.getElementById('signup-error');
    errorEl.textContent = '';

    if (!email || !password) {
        errorEl.textContent = 'Please enter both email and password.';
        return;
    }
    if (password.length < 6) {
        errorEl.textContent = 'Password must be at least 6 characters.';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/auth/signup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ displayName, email, password })
        });
        const data = await res.json();

        if (!res.ok) {
            errorEl.textContent = data.error || 'Signup failed.';
            return;
        }

        window.location.href = '/app.html';
    } catch (err) {
        errorEl.textContent = 'Network error: ' + err.message;
    }
});
