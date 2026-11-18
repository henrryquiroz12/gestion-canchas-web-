document.addEventListener('DOMContentLoaded', () => {
    const monthlyCtx = document.getElementById('chartVentasMensuales');
    if (monthlyCtx) {
        new Chart(monthlyCtx, {
            type: 'bar',
            data: {
                labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
                datasets: [{
                    label: 'Monto de reservas (S/.)',
                    data: ventasPorMes,
                    backgroundColor: '#10B981',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }
    const courtTypeCtx = document.getElementById('chartPorTipoCancha');
    const courtTypeEmpty = document.getElementById('chartTipoCanchaEmpty');
    const courtTypeSum = Object.values(ventasPorTipoCancha).reduce((a, b) => a + parseFloat(b), 0);
    if (courtTypeCtx) {
        const container = courtTypeCtx.closest('.chart-container');
        if (courtTypeSum > 0) {
            new Chart(courtTypeCtx, {
                type: 'doughnut',
                data: {
                    labels: Object.keys(ventasPorTipoCancha),
                    datasets: [{
                        data: Object.values(ventasPorTipoCancha),
                        backgroundColor: ['#10B981', '#064E3B', '#0F172A', '#0284C7']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });
        } else {
            if (container) container.classList.add('chart-container--hidden');
            if (courtTypeEmpty) courtTypeEmpty.style.display = 'block';
        }
    }
});
