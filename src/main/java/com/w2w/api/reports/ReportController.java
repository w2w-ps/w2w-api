package com.w2w.api.reports;

import com.w2w.api.reports.dto.ReportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/csv")
    public ResponseEntity<byte[]> generateCsv(@RequestBody ReportRequest request) throws IOException {
        byte[] csv = reportService.generateCsv(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("report.csv").build());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    @PostMapping("/pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody ReportRequest request) {
        byte[] pdf = reportService.generatePdf(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("report.pdf").build());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
