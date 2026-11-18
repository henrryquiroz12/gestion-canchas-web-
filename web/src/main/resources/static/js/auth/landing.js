document.addEventListener('DOMContentLoaded', () => {
    const preloader = document.getElementById('preloader');
    const landingCard = document.getElementById('landingCard');
    const landingFooter = document.getElementById('landingFooter');
    const btnIniciarSesion = document.getElementById('btnIniciarSesion');
    window.addEventListener('load', () => {
        setTimeout(() => {
            preloader.classList.add('preloader--hidden');
            landingCard.classList.add('animate-in');
            landingFooter.classList.add('animate-in');
        }, 600);
    });
    btnIniciarSesion.addEventListener('click', (e) => {
        e.preventDefault();
        const destino = btnIniciarSesion.getAttribute('href');
        landingCard.classList.add('animate-out');
        landingFooter.classList.add('animate-out');
        setTimeout(() => {
            window.location.href = destino;
        }, 450);
    });
});
