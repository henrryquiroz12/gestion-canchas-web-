package com.canchas.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicAboutControllerTest {

    @Test
    public void testShowAbout() {
        PublicAboutController controller = new PublicAboutController();
        Model model = new ConcurrentModel();
        String view = controller.showAbout(model);

        assertEquals("nosotros", view);
        assertEquals("nosotros", model.getAttribute("activeSection"));
    }
}