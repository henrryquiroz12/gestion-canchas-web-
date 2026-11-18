package com.canchas.web.service;

import com.canchas.web.dto.ReportFilterDTO;

public interface ReportService {

    byte[] generateExcel(ReportFilterDTO filter);
    byte[] generatePdf(ReportFilterDTO filter);
}
