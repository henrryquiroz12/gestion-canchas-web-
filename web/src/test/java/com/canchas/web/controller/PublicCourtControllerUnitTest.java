package com.canchas.web.controller;

import com.canchas.web.model.Court;
import com.canchas.web.service.CourtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class PublicCourtControllerUnitTest {

    @Mock
    private CourtService courtService;

    @InjectMocks
    private PublicCourtController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void listCourts_ShouldReturnCatalogWithFilters() {
        Court c1 = new Court(1L, "Cancha 1", Court.CourtType.NORMAL, new BigDecimal("40.00"), "Desc", null, true);
        Court c2 = new Court(2L, "Cancha 2", Court.CourtType.GRANDE, new BigDecimal("60.00"), "Desc", null, true);

        when(courtService.findAvailable()).thenReturn(Arrays.asList(c1, c2));

        Model model = new ConcurrentModel();

        // Test normal list
        String view = controller.listCourts(null, "numero", model);
        assertEquals("canchas/listado", view);
        List<Court> canchas = (List<Court>) model.getAttribute("canchas");
        assertEquals(2, canchas.size());

        // Test filter NORMAL
        model = new ConcurrentModel();
        controller.listCourts("NORMAL", "numero", model);
        canchas = (List<Court>) model.getAttribute("canchas");
        assertEquals(1, canchas.size());
        assertEquals(Court.CourtType.NORMAL, canchas.get(0).getType());

        // Test invalid filter (should be ignored and return 2)
        model = new ConcurrentModel();
        controller.listCourts("INVALIDO", "numero", model);
        canchas = (List<Court>) model.getAttribute("canchas");
        assertEquals(2, canchas.size());

        // Test sort precioDesc
        model = new ConcurrentModel();
        controller.listCourts(null, "precioDesc", model);
        canchas = (List<Court>) model.getAttribute("canchas");
        assertEquals(2, canchas.size());
        assertEquals(new BigDecimal("60.00"), canchas.get(0).getHourlyRate());
    }

    @Test
    public void courtDetail_ShouldReturnViewWhenCourtExists() {
        Court c1 = new Court(1L, "Cancha 1", Court.CourtType.NORMAL, new BigDecimal("40.00"), "Desc", null, true);
        when(courtService.findById(1L)).thenReturn(c1);

        Model model = new ConcurrentModel();
        String view = controller.courtDetail(1L, model);

        assertEquals("canchas/detalle", view);
        assertEquals(c1, model.getAttribute("cancha"));
    }

    @Test
    public void courtDetail_ShouldThrow404WhenCourtNotFound() {
        when(courtService.findById(99L)).thenReturn(null);

        Model model = new ConcurrentModel();

        assertThrows(ResponseStatusException.class, () -> {
            controller.courtDetail(99L, model);
        });
    }
}
