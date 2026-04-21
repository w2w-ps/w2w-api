package com.w2w.api.reports;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.w2w.api.config.TenantContext;
import com.w2w.api.reports.dto.ReportRequest;
import com.w2w.api.tenant.Company;
import com.w2w.api.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReportService {

    @Autowired
    private TenantService tenantService;

    public byte[] generateCsv(ReportRequest request) throws IOException {
        if (request.data() == null || request.data().isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {
            List<Map<String, Object>> data = request.data();
            String[] headers = data.get(0).keySet().toArray(new String[0]);
            writer.writeNext(headers);

            Map<String, BigDecimal> totals = new HashMap<>();
            Set<String> numericColumns = new java.util.HashSet<>();

            for (Map<String, Object> item : data) {
                String[] row = new String[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    Object val = item.get(headers[i]);
                    row[i] = val != null ? val.toString() : "";
                    
                    if (val instanceof Number num) {
                        numericColumns.add(headers[i]);
                        BigDecimal current = totals.getOrDefault(headers[i], BigDecimal.ZERO);
                        totals.put(headers[i], current.add(new BigDecimal(num.toString())));
                    }
                }
                writer.writeNext(row);
            }

            if (!totals.isEmpty()) {
                String[] totalsRow = new String[headers.length];
                int firstNumericIdx = -1;
                for (int i = 0; i < headers.length; i++) {
                    if (numericColumns.contains(headers[i])) {
                        if (firstNumericIdx == -1) firstNumericIdx = i;
                        totalsRow[i] = String.format("%.2f", totals.get(headers[i]));
                    } else {
                        totalsRow[i] = "";
                    }
                }
                if (firstNumericIdx > 0) {
                    totalsRow[firstNumericIdx - 1] = "Totals";
                }
                writer.writeNext(totalsRow);
            }
        }
        return out.toByteArray();
    }

    public byte[] generatePdf(ReportRequest request) {
        Company company = tenantService.getCompanyById(TenantContext.getCurrentTenant())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        // Fonts
        Font smallFont = new Font(Font.HELVETICA, 8);
        Font normalFont = new Font(Font.HELVETICA, 10);
        Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font companyFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font rangeFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font filterFont = new Font(Font.HELVETICA, 11);

        // Header Top Row
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        try {
            headerTable.setWidths(new float[]{1, 2, 1});
        } catch (Exception e) {}

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("M/d/yy, h:mm a", Locale.ENGLISH));
        PdfPCell leftCell = new PdfPCell(new Phrase(now, smallFont));
        leftCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(leftCell);

        PdfPCell centerHeaderCell = new PdfPCell(new Phrase("WhenToWork.com - Published Schedule", smallFont));
        centerHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        centerHeaderCell.setBorder(PdfPCell.NO_BORDER);
        headerTable.addCell(centerHeaderCell);

        PdfPCell rightCell = new PdfPCell(new Phrase("", smallFont));
        rightCell.setBorder(PdfPCell.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Title Section
        PdfPTable titleTable = new PdfPTable(3);
        titleTable.setWidthPercentage(100);
        try { titleTable.setWidths(new float[]{1, 3, 1}); } catch (Exception e) {}

        // Logo
        try {
            Image logo = Image.getInstance(new URL("https://whentowork.com/images_sales/w2w_logo_circle_und.png"));
            logo.scaleToFit(80, 80);
            PdfPCell logoCell = new PdfPCell(logo);
            logoCell.setBorder(PdfPCell.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleTable.addCell(logoCell);
        } catch (Exception e) {
            titleTable.addCell(createNoBorderCell("", companyFont)); // Fallback
        }

        PdfPCell centerTextCell = new PdfPCell();
        centerTextCell.setBorder(PdfPCell.NO_BORDER);
        centerTextCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        centerTextCell.addElement(createCenteredPara(company.getCompanyName(), companyFont, 0));
        centerTextCell.addElement(createCenteredPara(request.title(), titleFont, 5));
        
        DateTimeFormatter rangeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
        String rangeText = request.startDate().format(rangeFormatter) + " - " + request.endDate().format(rangeFormatter);
        centerTextCell.addElement(createCenteredPara(rangeText, rangeFont, 5));
        titleTable.addCell(centerTextCell);

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
        PdfPCell rightDateCell = new PdfPCell(new Phrase(todayStr, normalFont));
        rightDateCell.setBorder(PdfPCell.NO_BORDER);
        rightDateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightDateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleTable.addCell(rightDateCell);

        titleTable.setSpacingBefore(10);
        document.add(titleTable);

        // Filters
        if (request.categoryFilter() != null || request.positionsFilter() != null || request.statusFilter() != null) {
            Paragraph filtersPara = new Paragraph();
            filtersPara.setAlignment(Element.ALIGN_CENTER);
            filtersPara.setSpacingBefore(5);
            if (request.categoryFilter() != null) filtersPara.add(new Phrase(request.categoryFilter() + "\n", filterFont));
            if (request.positionsFilter() != null) filtersPara.add(new Phrase(request.positionsFilter() + "\n", filterFont));
            if (request.statusFilter() != null) filtersPara.add(new Phrase(request.statusFilter() + "\n", filterFont));
            document.add(filtersPara);
        }

        // Table
        if (request.data() != null && !request.data().isEmpty()) {
            List<Map<String, Object>> data = request.data();
            String[] headers = data.get(0).keySet().toArray(new String[0]);
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(20);

            for (String header : headers) {
                addTableHeader(table, header, boldFont);
            }

            Map<String, BigDecimal> totals = new HashMap<>();
            Set<String> numericColumns = new java.util.HashSet<>();

            for (Map<String, Object> item : data) {
                for (String header : headers) {
                    Object val = item.get(header);
                    String text = val != null ? val.toString() : "";
                    
                    if (val instanceof Number num) {
                        numericColumns.add(header);
                        BigDecimal current = totals.getOrDefault(header, BigDecimal.ZERO);
                        totals.put(header, current.add(new BigDecimal(num.toString())));
                        text = String.format(num instanceof Double || num instanceof Float || num instanceof BigDecimal ? "%.2f" : "%s", val);
                    }
                    table.addCell(new Phrase(text, normalFont));
                }
            }

            // Totals Row
            if (!totals.isEmpty()) {
                int firstNumericIdx = -1;
                for (int i = 0; i < headers.length; i++) {
                    if (numericColumns.contains(headers[i])) {
                        firstNumericIdx = i;
                        break;
                    }
                }

                for (int i = 0; i < headers.length; i++) {
                    if (numericColumns.contains(headers[i])) {
                        PdfPCell cell = new PdfPCell(new Phrase(String.format("%.2f", totals.get(headers[i])), boldFont));
                        table.addCell(cell);
                    } else {
                        String label = (i == firstNumericIdx - 1 && i >= 0) ? "Totals" : "";
                        PdfPCell cell = new PdfPCell(new Phrase(label, boldFont));
                        if (!label.isEmpty()) {
                            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            cell.setPaddingRight(5);
                        }
                        table.addCell(cell);
                    }
                }
            }
            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }

    private Paragraph createCenteredPara(String text, Font font, int spacingBefore) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        if (spacingBefore > 0) p.setSpacingBefore(spacingBefore);
        return p;
    }

    private PdfPCell createNoBorderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }

    private void addTableHeader(PdfPTable table, String headerTitle, Font font) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(new Color(255, 255, 255));
        header.setBorderWidth(0.5f);
        header.setPhrase(new Phrase(headerTitle, font));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(5);
        table.addCell(header);
    }
}
