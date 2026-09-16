if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('./service-worker.js');
    });
}

let deferredPrompt;
window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    
    // Check every 500ms until the DOM is loaded and the element exists
    const checkBanner = setInterval(() => {
        const installBanner = document.getElementById('install-banner');
        if (installBanner) {
            clearInterval(checkBanner);
            installBanner.style.display = 'block';
            installBanner.addEventListener('click', () => {
                installBanner.style.display = 'none';
                deferredPrompt.prompt();
                deferredPrompt.userChoice.then(() => { deferredPrompt = null; });
            });
        }
    }, 500);
});

document.addEventListener('DOMContentLoaded', () => {
    // Application Screens
    const screens = {
        login: document.getElementById('login-screen'),
        register: document.getElementById('register-screen'),
        home: document.getElementById('home-screen'),
        nearby: document.getElementById('nearby-screen'),
        call: document.getElementById('call-screen'),
        dialpad: document.getElementById('dialpad-screen'),
        contacts: document.getElementById('contacts-screen'),
        history: document.getElementById('history-screen'),
        settings: document.getElementById('settings-screen')
    };
    
    // Auth Forms & Inputs
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');

    // Global State
    let callTimerInterval = null;
    let callSeconds = 0;
    let currentUser = null;

    // --- NAVIGATION HELPERS ---
    window.showScreen = function(screenElement) {
        Object.values(screens).forEach(s => { if(s) s.classList.remove('active'); });
        if(screenElement) screenElement.classList.add('active');
    };

    // Auth Navigation
    document.getElementById('go-to-register').addEventListener('click', (e) => { e.preventDefault(); showScreen(screens.register); });
    document.getElementById('go-to-login').addEventListener('click', (e) => { e.preventDefault(); showScreen(screens.login); });
    document.getElementById('btn-logout').addEventListener('click', () => {
        sessionStorage.removeItem('currentUser');
        currentUser = null;
        showScreen(screens.login);
    });

    // Dashboard Buttons
    document.getElementById('btn-call').addEventListener('click', () => {
        document.getElementById('dial-input').value = 'DC-';
        showScreen(screens.dialpad);
    });
    document.getElementById('btn-contacts').addEventListener('click', () => showScreen(screens.contacts));
    document.getElementById('btn-nearby').addEventListener('click', () => { showScreen(screens.nearby); populateNearbyUsers(); });
    document.getElementById('btn-history').addEventListener('click', () => showScreen(screens.history));
    document.getElementById('btn-settings').addEventListener('click', () => {
        document.getElementById('settings-name').textContent = currentUser ? currentUser.name : "User";
        document.getElementById('settings-id').textContent = currentUser ? currentUser.id : "DC-00000";
        showScreen(screens.settings);
    });

    // Universal Back Buttons (all buttons with class 'back-to-home')
    document.querySelectorAll('.back-to-home').forEach(btn => {
        btn.addEventListener('click', () => showScreen(screens.home));
    });

    // --- AUTHENTICATION LOGIC ---
    function generateUserId() { return `DC-${Math.floor(10000 + Math.random() * 90000)}`; }

    registerForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const name = document.getElementById('reg-name').value;
        const username = document.getElementById('reg-username').value;
        const password = document.getElementById('reg-password').value;
        const confirm = document.getElementById('reg-confirm').value;

        if (password !== confirm) { alert("Passwords do not match!"); return; }

        const newId = generateUserId();
        const userObj = { id: newId, name: name, username: username, password: password };
        localStorage.setItem(`user_${username}`, JSON.stringify(userObj));
        alert(`Registration successful! Your ID is ${newId}`);
        showScreen(screens.login);
        registerForm.reset();
    });

    loginForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const identifier = document.getElementById('login-identifier').value;
        const password = document.getElementById('login-password').value;

        const userData = localStorage.getItem(`user_${identifier}`);
        if (userData) {
            const user = JSON.parse(userData);
            if (user.password === password) {
                sessionStorage.setItem('currentUser', JSON.stringify(user));
                updateDashboard(user);
                showScreen(screens.home);
                loginForm.reset();
            } else { alert("Incorrect password."); }
        } else {
            alert("Account not found. For this test phase, please register an account first.");
        }
    });

    function updateDashboard(user) {
        currentUser = user;
        document.getElementById('welcome-name').textContent = `Hello, ${user.name.split(' ')[0]} 👋`;
        document.getElementById('display-id').textContent = user.id;
    }

    // Check existing session on reload
    const activeUser = sessionStorage.getItem('currentUser');
    if (activeUser) {
        updateDashboard(JSON.parse(activeUser));
        showScreen(screens.home);
    }

    // --- DIALPAD LOGIC ---
    const dialInput = document.getElementById('dial-input');
    document.querySelectorAll('.dial-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const val = btn.textContent;
            if (val === 'X') {
                if (dialInput.value.length > 3) dialInput.value = dialInput.value.slice(0, -1);
            } else {
                dialInput.value += val;
            }
        });
    });
    document.getElementById('dial-call-btn').addEventListener('click', () => {
        if(dialInput.value.length > 3) {
            startCall('Unknown User', dialInput.value);
        }
    });


    // --- NEARBY USERS MOCK ---
    function populateNearbyUsers() {
        const listContainer = document.getElementById('nearby-list');
        listContainer.innerHTML = ''; 
        const mockPeers = [
            { name: "John Doe", id: "DC-72931" },
            { name: "Sarah Smith", id: "DC-55192" }
        ];

        mockPeers.forEach(peer => {
            const item = document.createElement('div');
            item.className = 'user-item';
            item.innerHTML = `
                <div class="user-info">
                    <div class="avatar-small">👤</div>
                    <div><div class="item-name">${peer.name}</div><div class="item-id"><span class="status-dot"></span> ${peer.id}</div></div>
                </div>
                <button class="btn-call-small" onclick="startCall('${peer.name}', '${peer.id}')">📞 Call</button>
            `;
            listContainer.appendChild(item);
        });
    }

    // --- CALLING LOGIC ---
    window.startCall = function(name, id) {
        document.getElementById('caller-name').textContent = name;
        document.getElementById('caller-id').textContent = id;
        document.getElementById('call-status').textContent = 'Connecting via Wi-Fi Direct...';
        
        const timerEl = document.getElementById('call-timer');
        timerEl.classList.remove('visible');
        timerEl.textContent = "00:00";
        callSeconds = 0;
        
        showScreen(screens.call);

        setTimeout(() => {
            document.getElementById('call-status').textContent = 'Connected - Local Socket';
            timerEl.classList.add('visible');
            callTimerInterval = setInterval(() => {
                callSeconds++;
                const m = String(Math.floor(callSeconds / 60)).padStart(2, '0');
                const s = String(callSeconds % 60).padStart(2, '0');
                timerEl.textContent = `${m}:${s}`;
            }, 1000);
        }, 3000);
    };

    document.getElementById('btn-end-call').addEventListener('click', () => {
        clearInterval(callTimerInterval);
        document.getElementById('call-status').textContent = 'Call Ended';
        setTimeout(() => showScreen(screens.home), 1500);
    });

    document.getElementById('btn-mute').addEventListener('click', function() {
        this.classList.toggle('active');
        this.textContent = this.classList.contains('active') ? '🎙️' : '🔇';
    });
    document.getElementById('btn-speaker').addEventListener('click', function() {
        this.classList.toggle('active');
    });
});
