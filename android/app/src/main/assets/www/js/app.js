document.addEventListener('DOMContentLoaded', () => {

    const screens = {
        auth: document.getElementById('auth-screen'),
        main: document.getElementById('main-screen'),
        call: document.getElementById('call-screen')
    };

    function showScreen(screen) {
        Object.values(screens).forEach(s => { if(s) s.classList.remove('active'); });
        if(screen) screen.classList.add('active');
    }

    // --- AUTHENTICATION TABS ---
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');
    const authBtn = document.getElementById('auth-btn');
    const authSubtitle = document.getElementById('auth-subtitle');
    let isLoginMode = true;

    if (tabLogin && tabRegister) {
        tabLogin.addEventListener('click', () => {
            isLoginMode = true;
            tabLogin.classList.add('active');
            tabRegister.classList.remove('active');
            authBtn.textContent = 'Login';
            authSubtitle.textContent = 'Welcome back to Offline Calling';
        });

        tabRegister.addEventListener('click', () => {
            isLoginMode = false;
            tabRegister.classList.add('active');
            tabLogin.classList.remove('active');
            authBtn.textContent = 'Create Account';
            authSubtitle.textContent = 'Join the Offline Network';
        });
    }

    // --- AUTHENTICATION LOGIC ---
    const authForm = document.getElementById('auth-form');
    const rememberChk = document.getElementById('auth-remember');
    
    if (authForm) {
        // Load saved auth
        if (localStorage.getItem('savedUsername')) {
            document.getElementById('auth-username').value = localStorage.getItem('savedUsername');
            document.getElementById('auth-password').value = localStorage.getItem('savedPassword');
            if(rememberChk) rememberChk.checked = true;
        }

        authForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const user = document.getElementById('auth-username').value;
            const pass = document.getElementById('auth-password').value;

            // In offline mode, register/login are functionally identical for local storage
            // If it was a real backend, we would separate the API calls here based on isLoginMode.

            if (rememberChk && rememberChk.checked) {
                localStorage.setItem('savedUsername', user);
                localStorage.setItem('savedPassword', pass);
            } else {
                localStorage.removeItem('savedUsername');
                localStorage.removeItem('savedPassword');
            }

            document.getElementById('my-username').textContent = user;
            showScreen(screens.main);

            // Tell native app to start standard standard peer discovery
            if (window.AndroidBridge && window.AndroidBridge.discoverPeers) {
                window.AndroidBridge.discoverPeers();
            }
        });
    }

    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', () => {
            showScreen(screens.auth);
        });
    }

    // --- MAC TO ID HASHING (GENIUS OFFLINE ROUTING) ---
    function macToId(mac) {
        if (!mac) return "00000";
        let hash = 0;
        for (let i = 0; i < mac.length; i++) {
            hash = ((hash << 5) - hash) + mac.charCodeAt(i);
            hash = hash & hash;
        }
        let str = Math.abs(hash).toString();
        while (str.length < 5) str = "0" + str;
        return str.substring(0, 5);
    }

    // --- NEARBY LIST LOGIC ---
    window.offlinePhonebook = {}; // ID -> MAC Address

    window.onPeersDiscovered = function(jsonPeersString) {
        const peers = JSON.parse(jsonPeersString);
        const listContainer = document.getElementById('nearby-list');
        listContainer.innerHTML = ''; 
        window.offlinePhonebook = {};

        if (peers.length === 0) {
            listContainer.innerHTML = '<p class="empty-state">No devices found yet.</p>';
            return;
        }

        peers.forEach(peer => {
            const numericId = macToId(peer.address);
            window.offlinePhonebook[numericId] = peer.address;

            const div = document.createElement('div');
            div.className = 'nearby-item';
            div.innerHTML = `
                <div>
                    <div class="nearby-id">${numericId}</div>
                    <div class="nearby-mac">${peer.name}</div>
                </div>
                <div style="color: #22c55e; font-size: 1.5rem; font-weight: bold;">+</div>
            `;
            // Auto-fill the input when tapped
            div.addEventListener('click', () => {
                document.getElementById('target-input').value = numericId;
            });
            listContainer.appendChild(div);
        });
    };

    // --- CALLING LOGIC ---
    let callTimerInterval = null;
    let callSeconds = 0;

    const btnCallMain = document.getElementById('btn-call-main');
    if (btnCallMain) {
        btnCallMain.addEventListener('click', () => {
            const targetId = document.getElementById('target-input').value.trim();
            if (targetId.length === 0) {
                alert("Please enter or select an ID Number.");
                return;
            }

            document.getElementById('call-target-name').textContent = "ID: " + targetId;
            document.getElementById('call-status').textContent = 'Negotiating connection...';
            document.getElementById('call-timer').classList.remove('visible');
            callSeconds = 0;
            showScreen(screens.call);

            if (window.AndroidBridge) {
                const macAddress = window.offlinePhonebook[targetId];
                if (macAddress) {
                    window.AndroidBridge.connectToPeer(macAddress);
                } else {
                    document.getElementById('call-status').textContent = 'Error: ID not found in nearby list.';
                }
            } else {
                // Simulator Mode
                setTimeout(() => { window.onConnectionChanged(true); }, 2000);
            }
        });
    }

    window.onConnectionChanged = function(isConnected) {
        const statusEl = document.getElementById('call-status');
        const timerEl = document.getElementById('call-timer');
        
        if (isConnected) {
            statusEl.textContent = 'Connected (Secure Socket)';
            timerEl.classList.add('visible');
            callTimerInterval = setInterval(() => {
                callSeconds++;
                const m = String(Math.floor(callSeconds / 60)).padStart(2, '0');
                const s = String(callSeconds % 60).padStart(2, '0');
                timerEl.textContent = `${m}:${s}`;
            }, 1000);
        } else {
            statusEl.textContent = 'Disconnected';
            clearInterval(callTimerInterval);
        }
    };

    const btnEndCall = document.getElementById('btn-end-call');
    if (btnEndCall) {
        btnEndCall.addEventListener('click', () => {
            clearInterval(callTimerInterval);
            document.getElementById('call-status').textContent = 'Ending Call...';
            if (window.AndroidBridge) window.AndroidBridge.disconnect();
            setTimeout(() => showScreen(screens.main), 1500);
        });
    }

    // SIMULATOR MOCK (For PC testing)
    if (typeof window.AndroidBridge === 'undefined') {
        setTimeout(() => {
            if (document.getElementById('main-screen').classList.contains('active')) {
                const fakePeers = [
                    { name: "Android_Galaxy", address: "00:11:22:33:44:55" },
                    { name: "Infinix_Note", address: "AA:BB:CC:DD:EE:FF" }
                ];
                if (window.onPeersDiscovered) window.onPeersDiscovered(JSON.stringify(fakePeers));
            }
        }, 3000);
    }
});
