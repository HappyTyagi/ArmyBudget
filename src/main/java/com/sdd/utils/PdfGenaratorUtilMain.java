package com.sdd.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sdd.entities.HrData;
import com.sdd.exception.SDDException;
import com.sdd.response.CDAReportResponse;
import com.sdd.response.CDAReportSubResponse;
import com.sdd.response.CbReportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.sdd.response.FilePathResponse;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Component
public class PdfGenaratorUtilMain {

    private static final String UTF_8 = "UTF-8";

    @SuppressWarnings("rawtypes")
    public void createPdfAllocation(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse, HrData hrData) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        float[] pointColumnWidths = {150F, 150F, 150F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setSpacingAfter(20);
        table.setWidthPercentage(100);

        Paragraph paragraphs = new Paragraph();
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
        paragraphs.add(new Chunk("ALLOCATION REPORT " + "( " + hrData.getUnit().toUpperCase() + " )", boldFont));
        paragraphs.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(paragraphs);
        document.add(new Paragraph("\n"));


        if (filePathResponse.getSubHeadKey().contains("2037")) {
            table.addCell(boldText("OBJECT HEAD", 10, 25f));
        } else {
            table.addCell(boldText("DETAILED HEAD", 10, 25f));
        }


        table.addCell(boldText("UNIT NAME", 10, 25f));
        table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + "ALLOCATION (In " + filePathResponse.getAmountType() + ")", 10, 25f));

        double grandTotal = 0f;
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            String key11 = entry11.getKey();
            List<ReportSubModel> tabData11 = entry11.getValue();

            table.addCell(normalText(key11, 10, 25f));

            double allAmountData = 0f;
            for (Integer i = 0; i < tabData11.size(); i++) {

                if (i == 0) {
                    table.addCell(normalText(tabData11.get(i).getUnit(), 10, 25f));
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);

                } else {
                    table.addCell(normalText("", 10, 25f));
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getUnit()), 10, 25f));

                    Boolean isNumber = ConverterUtils.isNumber(tabData11.get(i).getAmount() + "");
                    if (isNumber) {
                        table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);

                    } else {
                        table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 10, 25f));
                    }
                }
                allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());
            }

            table.addCell(boldText("", 10, 25f));
            table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount"), 10, 25f));
            table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
        }

        table.addCell(boldText(ConverterUtils.addDecimalPoint("Grand Total"), 10, 25f));
        table.addCell(boldText("", 10, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);


        int maxlength = ConverterUtils.getMaximumLength(filePathResponse.getApproveName().length(), (filePathResponse.getApproveRank()).length());

        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
        Chunk approverName = new Chunk((ConverterUtils.addSpacaeInString(filePathResponse.getApproveName(), maxlength) + "\n" + ConverterUtils.addSpacaeInString(filePathResponse.getApproveRank(), maxlength)), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);

        document.add(table);
        document.add(paragraph);
        document.close();

    }


    @SuppressWarnings("rawtypes")
    public void createPdfRecipt(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse, HrData hrData) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        float[] pointColumnWidths = {100F, 350F, 100F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        Paragraph paragraphs = new Paragraph();
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
        paragraphs.add(new Chunk("RECEIPT REPORT " + "( " + hrData.getUnit().toUpperCase() + " )", boldFont));
        paragraphs.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(paragraphs);
        document.add(new Paragraph("\n"));


        table.addCell(boldText("MAJOR/MINOR/SUB HEAD", 10, 25f));

        if (filePathResponse.getSubHeadKey().equalsIgnoreCase("2037")) {
            table.addCell(boldText("REVENUE OBJECT HEAD", 10, 25f));
        } else {
            table.addCell(boldText("CAPITAL DETAILED HEAD", 10, 25f));
        }


        table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", 10, 25f));


        if (filePathResponse.getSubHeadKey().equalsIgnoreCase("2037")) {
            table.addCell(boldText(filePathResponse.getSubHeadKey() + "\n00.102.06", 10, 25f));
            table.addCell(boldText(filePathResponse.getRevenueOrCapital(), 10, 25f));
            table.addCell(boldText("", 10, 25f));
        } else {
            table.addCell(boldText(filePathResponse.getSubHeadKey() + "\n00.037.01", 10, 25f));
            table.addCell(boldText(filePathResponse.getRevenueOrCapital(), 10, 25f));
            table.addCell(boldText("", 10, 25f));
        }


        double grandTotal = 0f;
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            String key11 = entry11.getKey();
            List<ReportSubModel> tabData11 = entry11.getValue();

            double allAmountData = 0f;
            for (Integer i = 0; i < tabData11.size(); i++) {


                table.addCell(normalText("", 10, 25f));

                table.addCell(normalText(key11, 10, 25f));

                Boolean isNumber = ConverterUtils.isNumber(tabData11.get(i).getAmount() + "");
                if (isNumber) {
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
                } else {
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
                }


                allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());

            }
        }

        table.addCell(boldText("", 10, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint("Grand Total") + "(" + filePathResponse.getRevenueOrCapital() + ")", 10, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);


        int maxlength = ConverterUtils.getMaximumLength(filePathResponse.getApproveName().length(), (filePathResponse.getApproveRank()).length());

        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
        Chunk approverName = new Chunk((ConverterUtils.addSpacaeInString(filePathResponse.getApproveName(), maxlength) + "\n" + ConverterUtils.addSpacaeInString(filePathResponse.getApproveRank(), maxlength)), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);


        document.add(table);
        document.add(paragraph);
        document.close();

    }


    @SuppressWarnings("rawtypes")
    public void createPdfConsolidateRecipt(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse, HrData hrData) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        float[] pointColumnWidths = {100F, 350F, 100F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        Paragraph paragraphs = new Paragraph();
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
        paragraphs.add(new Chunk("RECEIPT REPORT " + "( " + hrData.getUnit().toUpperCase() + " )", boldFont));
        paragraphs.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(paragraphs);
        document.add(new Paragraph("\n"));


        String key = "";
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            key = entry11.getKey();
        }


//        if (key.equalsIgnoreCase("2037")) {
        table.addCell(boldText("MAJOR/MINOR/SUB HEAD", 10, 25f));
        table.addCell(boldText("HEAD", 10, 25f));
        table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", 10, 25f));

//        } else {
//            table.addCell(boldText("MAJOR/MINOR/SUB HEAD", 10, 25f));
//            table.addCell(boldText("OBJECT HEAD", 10, 25f));
//            table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", 10, 25f));
//        }


        double grandTotal = 0f;
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            String key11 = entry11.getKey();
            List<ReportSubModel> tabData11 = entry11.getValue();

            if (key11.equalsIgnoreCase("2037")) {
                table.addCell(boldText(key11 + "\n00.102.06", 10, 25f));
                table.addCell(boldText("REVENUE OBJECT HEAD", 10, 25f));
                table.addCell(boldText("", 10, 25f));
            } else {
                table.addCell(boldText(key11 + "\n00.037.01", 10, 25f));
                table.addCell(boldText("CAPITAL DETAILED HEAD", 10, 25f));
                table.addCell(boldText("", 10, 25f));
            }

            double allAmountData = 0f;
            for (Integer i = 0; i < tabData11.size(); i++) {

                table.addCell(normalText("", 10, 25f));
                table.addCell(normalText(tabData11.get(i).getBudgetHead().getSubHeadDescr(), 10, 25f));
//                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getUnit()), 10, 25f));
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);

                allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());

            }


            if (key11.equalsIgnoreCase("2037")) {

                table.addCell(boldText("", 10, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount") + "(REVENUE)", 10, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);

            } else {
                table.addCell(boldText("", 10, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount") + "(CAPITAL)", 10, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
            }


        }

        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
            table.addCell(boldText("", 10, 25f));
            table.addCell(boldText(ConverterUtils.addDecimalPoint("Grand Total"), 10, 25f));
            table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 10, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
        }


        int maxlength = ConverterUtils.getMaximumLength(filePathResponse.getApproveName().length(), (filePathResponse.getApproveRank()).length());

        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
        Chunk approverName = new Chunk((ConverterUtils.addSpacaeInString(filePathResponse.getApproveName(), maxlength) + "\n" + ConverterUtils.addSpacaeInString(filePathResponse.getApproveRank(), maxlength)), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);


        document.add(table);
        document.add(paragraph);
        document.close();

    }

    public void createCdaMainReport(HashMap<String, List<CDAReportResponse>> map, CDAReportSubResponse cadSubReport, String path, double grandTotal, HashMap<String, String> coloumWiseAmount, FilePathResponse filePathResponse, HrData hrData) throws Exception {


        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();
        document.newPage();


        Font font = new Font(Font.FontFamily.COURIER, 15, Font.BOLD);
        Paragraph preface = new Paragraph();
        preface.setAlignment(Element.ALIGN_CENTER);

        if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
            Chunk header = new Chunk("\n" + "CDA WISE/OBJECT HEAD WISE CONTROL FIGURES FOR " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear() + "\n" + "\n", font);
            preface.add(header);
        } else {
            Chunk header = new Chunk("\n" + "CDA WISE/DETAILED HEAD WISE CONTROL FIGURES FOR " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear() + "\n" + "\n", font);
            preface.add(header);
        }


        String reOrCapital = "";
        if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
            reOrCapital = "REVENUE";
        } else {
            reOrCapital = "CAPITAL";
        }

        Chunk revenue = new Chunk(reOrCapital + "\n" + "\n", font);
        preface.add(revenue);

        Chunk thiredHead = new Chunk("Major Head " + cadSubReport.getMajorHead() + ", Sub Major Head 00, Minor Head " + cadSubReport.getMinorHead() + ") (In " + cadSubReport.getAmountType() + ")" + "\n" + "\n" + "\n" + "\n", font);
        preface.add(thiredHead);


        List<CDAReportResponse> tabData1 = map.get("Sub Head");
        PdfPTable table = new PdfPTable(tabData1.size() + 1);

        table.setWidthPercentage(100);
        table.setSpacingAfter(1);


        table.addCell(boldText("object", 6, 35f));
        for (Integer i = 0; i < tabData1.size(); i++) {
            table.addCell(boldText(tabData1.get(i).getName(), 6, 20f));
        }
        table.setHeaderRows(1);
        table.setSkipFirstHeader(true);

        table.addCell(boldText("object", 6, 35f));
        for (Integer i = 0; i < tabData1.size(); i++) {
            table.addCell(boldText(tabData1.get(i).getName(), 6, 20f));
        }
        for (Map.Entry<String, List<CDAReportResponse>> entry : map.entrySet()) {
            String key = entry.getKey();

            if (!key.equalsIgnoreCase("Sub Head")) {
                List<CDAReportResponse> tabData = entry.getValue();
                table.addCell(boldText(key, 7, 35f));
                for (Integer i = 0; i < tabData.size(); i++) {
                    Boolean isNumber = ConverterUtils.isNumber(tabData.get(i).getName() + "");
                    if (isNumber) {
                        table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(i).getName()), 7, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
                    } else {
                        table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(i).getName()), 7, 20f));
                    }
                }
            }
        }


        table.addCell(boldText("Grand Total", 7, 20f));

        double grandAllTotal11 = 0;
        for (Map.Entry<String, String> entry : coloumWiseAmount.entrySet()) {
            String tabData = entry.getValue();

            Boolean isNumber = ConverterUtils.isNumber(tabData + "");
            if (isNumber) {
                table.addCell(boldText(ConverterUtils.addDecimalPoint(tabData), 7, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
            } else {
                table.addCell(boldText(ConverterUtils.addDecimalPoint(tabData), 7, 20f));
            }

            if (isNumber) {
                grandAllTotal11 = grandAllTotal11 + Double.parseDouble(ConverterUtils.addDecimalPoint(tabData));
            } else {
                grandAllTotal11 = grandAllTotal11 + Double.parseDouble(ConverterUtils.addDecimalPoint(tabData));
            }

        }


//        double grandAllTotal = 0;
//        try {
//            for (Map.Entry<String, List<CDAReportResponse>> entry : map.entrySet()) {
//                String key = entry.getKey();
//
//                if (!key.equalsIgnoreCase("Sub Head")) {
//                    List<CDAReportResponse> tabData = entry.getValue();
//                    for (Integer i = 0; i < tabData.size(); i++) {
//                        Boolean isNumber = ConverterUtils.isNumber(tabData.get(i).getName() + "");
//                        if (isNumber) {
//                            grandAllTotal = grandAllTotal + Double.parseDouble(ConverterUtils.addDecimalPoint(tabData.get(i).getName()));
//                        } else {
//                            grandAllTotal = grandAllTotal + Double.parseDouble(ConverterUtils.addDecimalPoint(tabData.get(i).getName()));
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//        }

//        grandAllTotal = grandAllTotal / 2;

        table.addCell(boldText(ConverterUtils.addDecimalPoint((grandAllTotal11) + ""), 7, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
        int maxlength = ConverterUtils.getMaximumLength(filePathResponse.getApproveName().length(), (filePathResponse.getApproveRank()).length());

        Phrase phrase = new Phrase();
        Chunk approverName = new Chunk((ConverterUtils.addSpacaeInString(filePathResponse.getApproveName(), maxlength) + "\n" + ConverterUtils.addSpacaeInString(filePathResponse.getApproveRank(), maxlength)), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);

        document.add(preface);
        document.add(table);
        document.close();

    }

    public void createReserveFundnReport(HashMap<String, List<CDAReportResponse>> map, CDAReportSubResponse cadSubReport, String path, double grandTotal, double allocationGrandTotal, FilePathResponse filePathResponse, HrData hrData) throws Exception {


        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();
        document.newPage();


        Font font = new Font(Font.FontFamily.COURIER, 15, Font.BOLD);
        Chunk header = new Chunk("\n" + "RESERVE FUND " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear() + " " + "( " + hrData.getUnit().toUpperCase() + " )" + "\n" + "\n", font);
        Paragraph preface = new Paragraph();
        preface.setAlignment(Element.ALIGN_CENTER);
        preface.add(header);

        String reOrCapital = "";
        if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
            reOrCapital = "REVENUE";
        } else {
            reOrCapital = "CAPITAL";
        }

        Chunk revenue = new Chunk(reOrCapital + "\n" + "\n", font);
        preface.add(revenue);

        Chunk thiredHead = new Chunk("Major Head " + cadSubReport.getMajorHead() + ", Sub Major Head 00, Minor Head " + cadSubReport.getMinorHead() + ") (In " + cadSubReport.getAmountType() + ")" + "\n" + "\n" + "\n" + "\n", font);
        preface.add(thiredHead);


        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
            table.addCell(boldText("Revenue Object Head", 12, 20f));
        } else {
            table.addCell(boldText("Capital Detailed Head", 12, 20f));
        }


        table.addCell(boldText("Allocation Amount", 12, 20f));
        table.addCell(boldText("Reserve Fund", 12, 20f));


        for (Map.Entry<String, List<CDAReportResponse>> entry : map.entrySet()) {
            String key = entry.getKey();

            List<CDAReportResponse> tabData = entry.getValue();
            table.addCell(boldText(key, 12, 35f));

            Boolean isNumber = ConverterUtils.isNumber(tabData.get(0).getAllocationAmount() + "");
            if (isNumber) {
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(0).getAllocationAmount()), 12, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
            } else {
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(0).getAllocationAmount()), 12, 20f));
            }


            Boolean isNumber11 = ConverterUtils.isNumber(tabData.get(0).getName() + "");
            if (isNumber11) {
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(0).getName()), 12, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
            } else {
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(0).getName()), 12, 20f));
            }
        }

        table.addCell(boldText("Grand Total", 13, 20f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint(allocationGrandTotal + ""), 13, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 13, 20f)).setHorizontalAlignment(Element.ALIGN_RIGHT);


        int maxlength = ConverterUtils.getMaximumLength(filePathResponse.getApproveName().length(), (filePathResponse.getApproveRank()).length());

        Phrase phrase = new Phrase();
        Chunk approverName = new Chunk((ConverterUtils.addSpacaeInString(filePathResponse.getApproveName(), maxlength) + "\n" + ConverterUtils.addSpacaeInString(filePathResponse.getApproveRank(), maxlength)), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);


        document.add(preface);
        document.add(table);
        document.close();
    }


    @SuppressWarnings("rawtypes")
    public void createContigentBillReport(CbReportResponse cbReportResponse, String path, HrData hrData) throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = dateFormat.format(cbReportResponse.getCbData().getCbDate());
        Double bill = (Double.parseDouble(cbReportResponse.getCurrentBillAmount()));
        bill = bill * 100 / (100 + Double.parseDouble(cbReportResponse.getGetGst()));
        String billFormat = String.format("%.2f", bill);
        bill = Double.parseDouble(billFormat);
        double gst = (Double.parseDouble(cbReportResponse.getCurrentBillAmount()));
        gst = gst - bill;

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        Phrase phrase = new Phrase();
        Font normalFont = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.NORMAL);
        Font font = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.BOLD);


        Chunk billNumber1 = new Chunk("Contingent Bill No. ", normalFont);
        Chunk billNumber2 = new Chunk(cbReportResponse.getCbData().getCbNo() + "                           ", font);
        Chunk billNumber3 = new Chunk("                        " + " Dated: ", normalFont);
        Chunk billNumber4 = new Chunk(date(dateString) + " \n", font);
        phrase.add(billNumber1);
        phrase.add(billNumber2);
        phrase.add(billNumber3);
        phrase.add(billNumber4);

        Font fontHeader = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        Chunk contigentBill = new Chunk("                                                                                   " + "CONTINGENT BILL  " + " \n\n", fontHeader);
        phrase.add(contigentBill);


//        Chunk normalTexet = new Chunk(cbReportResponse.getOnAccountData() + " \n", normalFont);
//        phrase.add(normalTexet);


        Chunk totalAmount = new Chunk("Total Amount/ Budget allotted                                                                                    (INR)  " + cbReportResponse.getAllocatedAmount() + " \n", normalFont);
        phrase.add(totalAmount);

        Chunk progressiveExpen = new Chunk("Progressive expenditure including this bill                                                                 (INR)  " + cbReportResponse.getExpenditureAmount() + " \n", normalFont);
        phrase.add(progressiveExpen);

        Chunk balanceAmount = new Chunk("Balance Fund                                                                                                               (INR)  " + cbReportResponse.getRemeningAmount() + " \n\n", normalFont);
        phrase.add(balanceAmount);


        Chunk authoritya = new Chunk("Authority: (a) " + cbReportResponse.getOnAurthyData() + " \n", normalFont);
        phrase.add(authoritya);


        Chunk authorityb1 = new Chunk("               : (b) " + cbReportResponse.getUnitData().getDescr() + "  Sanction No. ", normalFont);
        Chunk authorityb2 = new Chunk(cbReportResponse.getCbData().getSectionNumber() + " \n\n", font);
        phrase.add(authorityb1);
        phrase.add(authorityb2);


        float[] pointColumnWidths = {30F, 330F, 90F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        table.addCell(boldText("Sr.", 10, 25f));
        table.addCell(boldText("Details of Expenditure", 10, 25f));
        table.addCell(boldText("Amount (in INR)", 10, 25f));

        table.addCell(normalText("01", 9, 50f));
//        table.addCell(normalText("Expenditure incurred towards quaterly payment for the 3rd otr from 01 Sep 22 to 30 Nov 22 in respect of Hirring of Designer/Developer IT Manpower (Project-SDOT) through " + cbReportResponse.getCbData().getVendorName() + " vibe Invoiice/bill " + cbReportResponse.getCbData().getInvoiceNO() + " Dated " + cbReportResponse.getCbData().getInvoiceDate(), 10, 50f));
        table.addCell(normalText(cbReportResponse.getOnAccountData() + " through " + cbReportResponse.getCbData().getVendorName() + " vide Invoice/bill " + cbReportResponse.getCbData().getInvoiceNO() + " Dated " + cbReportResponse.getCbData().getInvoiceDate(), 10, 50f));
//        table.addCell(normalText(cbReportResponse.getOnAccountData() + " vide Invoice/bill " + cbReportResponse.getCbData().getInvoiceNO() + " Dated " + cbReportResponse.getCbData().getInvoiceDate(), 10, 50f));
        table.addCell(normalText(ConverterUtils.addDecimalPoint(bill.toString()), 9, 50f)).setHorizontalAlignment(Element.ALIGN_RIGHT);


        table.addCell(normalText("", 9, 25f));
        table.addCell(normalText("GST " + cbReportResponse.getCbData().getGst() + " % ", 9, 25f));
        table.addCell(normalText(ConverterUtils.addDecimalPoint(String.format("%.2f", gst)), 9, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);


        table.addCell(normalText("", 9, 25f));
        table.addCell(normalText("TOTAL ", 9, 25f));
        table.addCell(normalText(ConverterUtils.addDecimalPoint(cbReportResponse.getCurrentBillAmount()), 9, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(normalText("", 9, 25f));
        table.addCell(boldText("Amount in words (Rupees " + convertDecimaltoString(cbReportResponse.getCurrentBillAmount()) + ")", 9, 25f)).setHorizontalAlignment(Element.ALIGN_RIGHT);
        ;
        table.addCell(normalText("", 9, 25f));


        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);


        document.add(paragraph);
        document.add(table);


        Phrase phraseFooter = new Phrase();

        Chunk certifyBy = new Chunk("Certify that:-" + " \n", normalFont);
        phraseFooter.add(certifyBy);


        Chunk certifya = new Chunk("(a) Items has/have been taken on charge." + " \n", normalFont);
        phraseFooter.add(certifya);

        Chunk certifyb = new Chunk("(b) The rates is/are fair and reasonable." + " \n", normalFont);
        phraseFooter.add(certifyb);


        Chunk certifyc1 = new Chunk("(c) The Expenditure incurred is creditable to Major Head ", normalFont);
        Chunk certifyc2 = new Chunk(cbReportResponse.getBudgetHead().getMajorHead() + ",", font);
        Chunk certifyc3 = new Chunk(" Sub Major Head 00, Minor Head -", normalFont);
        Chunk certifyc4 = new Chunk(cbReportResponse.getBudgetHead().getMinorHead(), font);
        Chunk certifyc5 = new Chunk(cbReportResponse.getBudgetHead().getDetailHeadType() + "", normalFont);
        Chunk certifyc6 = new Chunk(cbReportResponse.getBudgetHead().getSubHeadDescr() + " \n", font);
//        Chunk certifyc7 = new Chunk(".Category Code ", normalFont);
//        Chunk certifyc8 = new Chunk(cbReportResponse.getBudgetHead().getBudgetHeadId() + " \n", font);
        phraseFooter.add(certifyc1);
        phraseFooter.add(certifyc2);
        phraseFooter.add(certifyc3);
        phraseFooter.add(certifyc4);
        phraseFooter.add(certifyc5);
        phraseFooter.add(certifyc6);
//        phraseFooter.add(certifyc7);
//        phraseFooter.add(certifyc8);

        Chunk certifyd = new Chunk("(d) The expenditure has been incurred in the interest of the state." + " \n ", normalFont);
        phraseFooter.add(certifyd);


        PdfPTable tables1 = new PdfPTable(4);
        tables1.setWidthPercentage(100);

        PdfPCell cell100 = new PdfPCell(new Phrase(""));
        PdfPCell cell200 = new PdfPCell(new Phrase(""));
        PdfPCell cell300 = new PdfPCell(new Phrase(""));
        PdfPCell cell400 = new PdfPCell(new Phrase(cbReportResponse.getVerifer().getFullName() + " \n" + cbReportResponse.getVerifer().getRank()));

        cell100.setBorder(0);
        cell200.setBorder(0);
        cell300.setBorder(0);
        cell400.setBorder(0);
        cell400.setPadding(20);

        tables1.addCell(cell100);
        tables1.addCell(cell200);
        tables1.addCell(cell300);
        tables1.addCell(cell400);

        phraseFooter.add(tables1);


        Font counterSign = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        Chunk counterSigner = new Chunk("                                                               COUNTERSIGNED" + " \n", counterSign);
        phraseFooter.add(counterSigner);


        PdfPTable tables11 = new PdfPTable(4);
        tables11.setWidthPercentage(100);

        PdfPCell cell10011 = new PdfPCell(new Phrase(hrData.getUnit()));
        PdfPCell cell20011 = new PdfPCell(new Phrase(""));
        PdfPCell cell30011 = new PdfPCell(new Phrase(""));
        PdfPCell cell40011 = new PdfPCell(new Phrase(cbReportResponse.getApprover().getFullName() + " \n" + cbReportResponse.getApprover().getRank()));

        cell10011.setBorder(0);
        cell20011.setBorder(0);
        cell30011.setBorder(0);
        cell40011.setBorder(0);
        cell40011.setPadding(20);

        tables11.addCell(cell10011);
        tables11.addCell(cell20011);
        tables11.addCell(cell30011);
        tables11.addCell(cell40011);

        phraseFooter.add(tables11);


        Chunk fileNumber = new Chunk("File No. " + cbReportResponse.getCbData().getFileID() + "\n", font);
        phraseFooter.add(fileNumber);

        Chunk fileDated = new Chunk("Date " + date(cbReportResponse.getCbData().getFileDate()) + "\n", font);
        phraseFooter.add(fileDated);


        Paragraph footeraragraph = new Paragraph();
        footeraragraph.add(phraseFooter);


        document.add(footeraragraph);
        document.close();

    }


    private PdfPCell boldText(String text, int fontSize, float cellHeight) {
        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, fontSize, Font.BOLD);
        Chunk world = new Chunk(text, font);
        phrase.add(world);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        PdfPCell cell = new PdfPCell(phrase);
        cell.setMinimumHeight(cellHeight);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell normalText(String text, int fontSize, float cellHeight) {

        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.HELVETICA, fontSize, Font.NORMAL);
        Chunk world = new Chunk(text, font);
        phrase.add(world);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        PdfPCell cell = new PdfPCell(phrase);
        cell.setMinimumHeight(cellHeight);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public static String date(String dateInput) throws Exception {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date date = inputFormat.parse(dateInput);
        String output = outputFormat.format(date);

        return output;
    }


    private static String convertDecimaltoString(String str) {
        String words = "";

        double x = Double.parseDouble(str);
        String whole = convertNumberToWords((long) x);
        long y = Long.parseLong(str.substring(str.indexOf('.') + 1));
        String decimal = (convertDecimalToWords(y, str.substring(str.indexOf('.') + 1).length()));
        if (decimal == "")
            return whole;
        else
            words = whole + " point " + decimal;
        return words;
    }

    private static String convertDecimalToWords(long y, int length) {
        String words = "";
        String[] units = {"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        while (y != 0) {
            words = words + " " + units[(int) (y / Math.pow(10, length - 1))];
            y = (long) (y % Math.pow(10, length - 1));
            length--;
        }
        return words;
    }

    public static String convertNumberToWords(long number) {
        if (number == 0) {
            return "Zero";
        }
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        String[] thousands = {"", "Hundred", "Thousand", "Lakh", "Crore"};
        int i = 0;
        String words = "";
        while (number > 0) {
            String str = "";
            long j = number / 10000000;
            if (number >= 10000000) {
                if (number / 10000000 >= 100) {
                    words = convertNumberToWords(number / 10000000) + " " + thousands[4];
                    number = number % 10000000;
                } else {
                    int x = (int) number / 10000000;
                    System.out.println("X:" + x);
                    if (x > 20) {
                        int y = x / 10;
                        str = str + tens[y] + " ";
                        x = x % 10;
                        System.out.println("Y:" + y);

                    }
                    words = words + " " + str + units[x] + " " + thousands[4];
                    number = number % 10000000;
                }
            } else if (number >= 100000) {
                int x = (int) number / 100000;
                System.out.println("X:" + x);
                if (x > 20) {
                    int y = x / 10;
                    str = str + tens[y] + " ";
                    x = x % 10;
                    System.out.println("Y:" + y);
                }
                words = words + " " + str + units[x] + " " + thousands[3];
                number = number % 100000;
            } else if (number >= 1000) {
                int x = (int) number / 1000;
                System.out.println("X:" + x);
                if (x > 20) {
                    int y = x / 10;
                    str = str + tens[y] + " ";
                    x = x % 10;
                    System.out.println("Y:" + y);
                }
                words = words + " " + str + units[x] + " " + thousands[2];
                number = number % 1000;
            } else if (number >= 100) {
                int x = (int) number / 100;
                System.out.println("X:" + x);
                if (x > 20) {
                    int y = x / 10;
                    str = str + tens[y] + " ";
                    x = x % 10;
                    System.out.println("Y:" + y);
                }
                words = words + " " + str + units[x] + " " + thousands[1];
                number = number % 100;
            } else {
                int x = (int) number;
                System.out.println("X:" + x);
                if (x > 20) {
                    int y = x / 10;
                    str = str + tens[y] + " ";
                    x = x % 10;
                    System.out.println("Y:" + y);
                }
                words = words + " " + str + units[x] + " " + thousands[0];
                number = number / 100;
            }
        }
        System.out.println(words);
        return words.trim();
    }


}
