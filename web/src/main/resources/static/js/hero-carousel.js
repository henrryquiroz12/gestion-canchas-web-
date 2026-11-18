/**
 * hero-carousel.js - Carrusel de Fachada Fredy Queen Soccer (Accesible WCAG 2.2 AA)
 * - Control de Pausar / Reanudar explícito accesible.
 * - Deslizamiento horizontal con transform: translateX(...).
 * - Autoplay de 3 segundos con dirección consistente.
 * - Soporte para prefers-reduced-motion y estado manual de pausa respetado.
 * - Navegación accesible por teclado.
 */
document.addEventListener('DOMContentLoaded', function () {
    const carouselEl = document.getElementById('fqsHeroCarousel');
    const track = document.getElementById('fqsCarouselTrack');
    const prevBtn = document.getElementById('fqsCarouselPrev');
    const nextBtn = document.getElementById('fqsCarouselNext');
    const dotsContainer = document.getElementById('fqsCarouselDots');
    const togglePlayBtn = document.getElementById('fqsCarouselTogglePlay');
    const toggleIcon = document.getElementById('fqsCarouselToggleIcon');
    const toggleText = document.getElementById('fqsCarouselToggleText');

    if (!carouselEl || !track || !prevBtn || !nextBtn || !dotsContainer) {
        return;
    }

    const dots = Array.from(dotsContainer.querySelectorAll('.hero-carousel-dot'));
    const totalRealSlides = 3;
    let currentIndex = 0;
    let autoplayTimer = null;
    let isTransitioning = false;
    let isManuallyPaused = false;

    // Accesibilidad de movimiento reducido
    const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    let prefersReducedMotion = motionQuery.matches;

    function updatePlayButtonUI() {
        if (!togglePlayBtn) return;
        const isPaused = isManuallyPaused || prefersReducedMotion || !autoplayTimer;
        if (isPaused) {
            if (toggleIcon) {
                toggleIcon.className = 'bi bi-play-fill';
            }
            if (toggleText) {
                toggleText.textContent = 'Reanudar carrusel';
            }
            togglePlayBtn.setAttribute('aria-label', 'Reanudar carrusel');
            togglePlayBtn.setAttribute('title', 'Reanudar carrusel');
        } else {
            if (toggleIcon) {
                toggleIcon.className = 'bi bi-pause-fill';
            }
            if (toggleText) {
                toggleText.textContent = 'Pausar carrusel';
            }
            togglePlayBtn.setAttribute('aria-label', 'Pausar carrusel');
            togglePlayBtn.setAttribute('title', 'Pausar carrusel');
        }
    }

    if (prefersReducedMotion) {
        isManuallyPaused = true;
    }

    motionQuery.addEventListener('change', function () {
        prefersReducedMotion = motionQuery.matches;
        if (prefersReducedMotion) {
            isManuallyPaused = true;
            stopAutoplay();
            track.style.transition = 'none';
        } else {
            isManuallyPaused = false;
            startAutoplay();
        }
        updatePlayButtonUI();
    });

    function getTransitionStyle() {
        return prefersReducedMotion ? 'none' : 'transform 700ms cubic-bezier(0.25, 1, 0.5, 1)';
    }

    function updateDots(realIdx) {
        dots.forEach(function (dot, i) {
            if (i === realIdx) {
                dot.classList.add('active');
                dot.setAttribute('aria-current', 'true');
            } else {
                dot.classList.remove('active');
                dot.removeAttribute('aria-current');
            }
        });
    }

    function slideNext() {
        if (isTransitioning) return;
        isTransitioning = true;
        currentIndex++;
        track.style.transition = getTransitionStyle();
        track.style.transform = 'translateX(-' + (currentIndex * 100) + '%)';
        updateDots(currentIndex % totalRealSlides);
    }

    function slidePrev() {
        if (isTransitioning) return;
        isTransitioning = true;

        if (currentIndex === 0) {
            track.style.transition = 'none';
            currentIndex = totalRealSlides;
            track.style.transform = 'translateX(-' + (currentIndex * 100) + '%)';
            void track.offsetHeight;

            currentIndex = totalRealSlides - 1;
            track.style.transition = getTransitionStyle();
            track.style.transform = 'translateX(-' + (currentIndex * 100) + '%)';
        } else {
            currentIndex--;
            track.style.transition = getTransitionStyle();
            track.style.transform = 'translateX(-' + (currentIndex * 100) + '%)';
        }
        updateDots(currentIndex % totalRealSlides);
    }

    function slideTo(targetIndex) {
        if (isTransitioning || targetIndex === (currentIndex % totalRealSlides)) return;
        isTransitioning = true;
        currentIndex = targetIndex;
        track.style.transition = getTransitionStyle();
        track.style.transform = 'translateX(-' + (currentIndex * 100) + '%)';
        updateDots(currentIndex);
    }

    track.addEventListener('transitionend', function () {
        isTransitioning = false;
        if (currentIndex === totalRealSlides) {
            track.style.transition = 'none';
            currentIndex = 0;
            track.style.transform = 'translateX(0%)';
        }
    });

    function startAutoplay() {
        if (prefersReducedMotion || isManuallyPaused) {
            updatePlayButtonUI();
            return;
        }
        stopAutoplay();
        autoplayTimer = setInterval(function () {
            slideNext();
        }, 3000);
        updatePlayButtonUI();
    }

    function stopAutoplay() {
        if (autoplayTimer) {
            clearInterval(autoplayTimer);
            autoplayTimer = null;
        }
        updatePlayButtonUI();
    }

    function resetAutoplay() {
        if (isManuallyPaused || prefersReducedMotion) return;
        stopAutoplay();
        startAutoplay();
    }

    // Botón de Pausa / Reanudación
    if (togglePlayBtn) {
        togglePlayBtn.addEventListener('click', function () {
            if (isManuallyPaused || !autoplayTimer) {
                isManuallyPaused = false;
                startAutoplay();
            } else {
                isManuallyPaused = true;
                stopAutoplay();
            }
        });
    }

    // Eventos de botones
    nextBtn.addEventListener('click', function () {
        slideNext();
        resetAutoplay();
    });

    prevBtn.addEventListener('click', function () {
        slidePrev();
        resetAutoplay();
    });

    // Eventos de indicadores
    dots.forEach(function (dot) {
        dot.addEventListener('click', function () {
            const idx = parseInt(this.getAttribute('data-index'), 10);
            if (!isNaN(idx)) {
                slideTo(idx);
                resetAutoplay();
            }
        });
    });

    // Pausa en desktop al hacer hover solo si no fue pausado manualmente
    carouselEl.addEventListener('mouseenter', function () {
        if (!isManuallyPaused && window.matchMedia('(hover: hover)').matches) {
            stopAutoplay();
        }
    });

    carouselEl.addEventListener('mouseleave', function () {
        if (!isManuallyPaused && window.matchMedia('(hover: hover)').matches) {
            startAutoplay();
        }
    });

    // Teclado: flechas izquierda/derecha cuando el carrusel tiene foco
    carouselEl.addEventListener('keydown', function(e) {
        if (e.key === 'ArrowLeft') {
            slidePrev();
            resetAutoplay();
        } else if (e.key === 'ArrowRight') {
            slideNext();
            resetAutoplay();
        }
    });

    // Inicializar carrusel
    updateDots(0);
    if (!prefersReducedMotion) {
        startAutoplay();
    } else {
        updatePlayButtonUI();
    }
});