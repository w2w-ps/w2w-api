package com.w2w.api.reports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import com.w2w.api.config.CurrentTenant;
import com.w2w.api.tenant.Company;
import com.w2w.api.reports.dto.ReportRequest;
import com.w2w.api.tenant.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TenantService tenantService;

    public ReportService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Transactional(readOnly = true)
    public byte[] generateCsv(ReportRequest request) throws IOException {
        if (request.data() == null || request.data().isEmpty()) {
            return new byte[0];
        }

        try {
            JasperPrint jasperPrint = createJasperPrint(request);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleWriterExporterOutput(baos));
            exporter.exportReport();
            return baos.toByteArray();
        } catch (JRException e) {
            throw new IOException("Error generating CSV report", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(ReportRequest request) {
        try {
            JasperPrint jasperPrint = createJasperPrint(request);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    private JasperPrint createJasperPrint(ReportRequest request) throws JRException {
        List<Map<String, Object>> data = request.data();
        if (data == null || data.isEmpty()) {
            throw new RuntimeException("No data to report");
        }

        Company company = tenantService.getCompanyById(CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        JasperDesign design = new JasperDesign();
        design.setName(request.title());
        design.setPageWidth(595); // A4
        design.setPageHeight(842);
        design.setColumnWidth(555);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        // Fields Definition (Dynamic)
        String[] headers = data.get(0).keySet().toArray(new String[0]);
        for (String header : headers) {
            JRDesignField field = new JRDesignField();
            field.setName(header);
            field.setValueClass(Object.class);
            design.addField(field);
        }

        // Title Band
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(100);

        JRDesignStaticText titleText = new JRDesignStaticText();
        titleText.setX(0);
        titleText.setY(0);
        titleText.setWidth(555);
        titleText.setHeight(30);
        titleText.setText(company.getCompanyName());
        titleText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        titleText.setFontSize(18f);
        titleText.setBold(true);
        titleBand.addElement(titleText);

        JRDesignStaticText subTitleText = new JRDesignStaticText();
        subTitleText.setX(0);
        subTitleText.setY(30);
        subTitleText.setWidth(555);
        subTitleText.setHeight(20);
        subTitleText.setText(request.title());
        subTitleText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        subTitleText.setFontSize(14f);
        titleBand.addElement(subTitleText);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String range = request.startDate().format(dtf) + " - " + request.endDate().format(dtf);
        JRDesignStaticText rangeText = new JRDesignStaticText();
        rangeText.setX(0);
        rangeText.setY(50);
        rangeText.setWidth(555);
        rangeText.setHeight(20);
        rangeText.setText(range);
        rangeText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        titleBand.addElement(rangeText);

        design.setTitle(titleBand);

        // Header Band
        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(20);
        int colWidth = 555 / headers.length;

        for (int i = 0; i < headers.length; i++) {
            JRDesignStaticText headerText = new JRDesignStaticText();
            headerText.setX(i * colWidth);
            headerText.setY(0);
            headerText.setWidth(colWidth);
            headerText.setHeight(20);
            headerText.setText(headers[i].toUpperCase());
            headerText.setBold(true);
            headerText.setMode(ModeEnum.OPAQUE);
            headerText.setBackcolor(new Color(230, 230, 230));
            headerBand.addElement(headerText);
        }
        design.setColumnHeader(headerBand);

        // Detail Band
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(20);

        for (int i = 0; i < headers.length; i++) {
            JRDesignTextField textField = new JRDesignTextField();
            textField.setX(i * colWidth);
            textField.setY(0);
            textField.setWidth(colWidth);
            textField.setHeight(20);
            textField.setExpression(new JRDesignExpression("$F{" + headers[i] + "}"));
            detailBand.addElement(textField);
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detailBand);

        // Summary Band (Totals)
        JRDesignBand summaryBand = new JRDesignBand();
        summaryBand.setHeight(30);

        // Optional filter text
        StringBuilder filters = new StringBuilder("Filters: ");
        if (request.categoryFilter() != null)
            filters.append("Category: ").append(request.categoryFilter()).append(" ");
        if (request.positionsFilter() != null)
            filters.append("Positions: ").append(request.positionsFilter()).append(" ");
        if (request.statusFilter() != null)
            filters.append("Status: ").append(request.statusFilter());

        JRDesignStaticText filterText = new JRDesignStaticText();
        filterText.setX(0);
        filterText.setY(5);
        filterText.setWidth(555);
        filterText.setHeight(15);
        filterText.setText(filters.toString());
        filterText.setFontSize(8f);
        summaryBand.addElement(filterText);

        design.setSummary(summaryBand);

        JasperReport report = JasperCompileManager.compileReport(design);
        Map<String, Object> params = new HashMap<>();
        return JasperFillManager.fillReport(report, params, new JRMapCollectionDataSource((java.util.Collection) data));
    }
}
