package com.canchas.web.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDTO {
    private long weeklyReservations;
    private BigDecimal weeklyRevenue;
    private List<BigDecimal> monthlySales;
    private Map<String, BigDecimal> salesByCourtType;

    // Backward compatibility aliases
    public long getAlquileresSemana() { return weeklyReservations; }
    public void setAlquileresSemana(long alquileresSemana) { this.weeklyReservations = alquileresSemana; }
    public BigDecimal getGananciasSemana() { return weeklyRevenue; }
    public void setGananciasSemana(BigDecimal gananciasSemana) { this.weeklyRevenue = gananciasSemana; }
    public List<BigDecimal> getVentasPorMes() { return monthlySales; }
    public void setVentasPorMes(List<BigDecimal> ventasPorMes) { this.monthlySales = ventasPorMes; }
    public Map<String, BigDecimal> getVentasPorTipoCancha() { return salesByCourtType; }
    public void setVentasPorTipoCancha(Map<String, BigDecimal> ventasPorTipoCancha) { this.salesByCourtType = ventasPorTipoCancha; }
}
