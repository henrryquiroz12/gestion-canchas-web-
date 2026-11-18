const canvas = document.getElementById('particlesCanvas');
const ctx = canvas.getContext('2d');
let particles = [];

function resizeCanvas() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
}

function crearParticulas() {
    particles = [];
    const cantidad = window.innerWidth < 768 ? 14 : 26;
    for (let i = 0; i < cantidad; i++) {
        particles.push({ x: Math.random() * canvas.width, y: Math.random() * canvas.height, radio: Math.random() * 2 + 1, velocidadX: (Math.random() - 0.5) * 0.3, velocidadY: (Math.random() - 0.5) * 0.3, opacidad: Math.random() * 0.4 + 0.1 });
    }
}

function dibujarParticulas() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    particles.forEach(p => {
        p.x += p.velocidadX;
        p.y += p.velocidadY;
        if (p.x < 0 || p.x > canvas.width) p.velocidadX *= -1;
        if (p.y < 0 || p.y > canvas.height) p.velocidadY *= -1;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radio, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(198, 255, 52, ${p.opacidad})`;
        ctx.fill();
    });
    particles.forEach((p, i) => {
        particles.slice(i + 1).forEach(q => {
            const dx = p.x - q.x;
            const dy = p.y - q.y;
            const distancia = Math.sqrt(dx * dx + dy * dy);
            if (distancia < 120) {
                ctx.beginPath();
                ctx.moveTo(p.x, p.y);
                ctx.lineTo(q.x, q.y);
                ctx.strokeStyle = `rgba(178, 213, 229, ${0.08 * (1 - distancia / 120)})`;
                ctx.stroke();
            }
        });
    });
    requestAnimationFrame(dibujarParticulas);
}

window.addEventListener('resize', () => {
    resizeCanvas();
    crearParticulas();
});

resizeCanvas();
crearParticulas();
dibujarParticulas();
