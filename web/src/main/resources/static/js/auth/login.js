document.addEventListener('DOMContentLoaded', () => {
    const preloader = document.getElementById('preloader');
    const loginCard = document.getElementById('loginCard');
    const alertError = document.getElementById('alertError');
    const togglePassword = document.getElementById('togglePassword');
    const passwordInput = document.querySelector('input[name="password"]');
    const loginForm = document.getElementById('loginForm');
    const btnLogin = document.getElementById('btnLogin');
    window.addEventListener('load', () => {
        setTimeout(() => {
            preloader.classList.add('preloader--hidden');
            loginCard.classList.add('animate-in');
            if (alertError) {
                setTimeout(() => alertError.classList.add('shake'), 500);
            }
        }, 500);
    });
    togglePassword.addEventListener('click', () => {
        const esPassword = passwordInput.type === 'password';
        passwordInput.type = esPassword ? 'text' : 'password';
        togglePassword.classList.toggle('bi-eye');
        togglePassword.classList.toggle('bi-eye-slash');
    });
    loginForm.addEventListener('submit', () => {
        btnLogin.classList.add('btn-login--loading');
        btnLogin.disabled = true;
    });
});
