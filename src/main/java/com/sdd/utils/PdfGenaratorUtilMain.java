package com.sdd.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.sdd.response.CDAReportResponse;
import com.sdd.response.CDAReportSubResponse;
import com.sdd.response.CbReportResponse;
import org.springframework.stereotype.Component;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.sdd.response.FilePathResponse;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Component
public class PdfGenaratorUtilMain {

    private static final String UTF_8 = "UTF-8";

    @SuppressWarnings("rawtypes")
    public void createPdfAllocation(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        float[] pointColumnWidths = {150F, 150F, 150F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setSpacingAfter(20);

        table.addCell(boldText("SUB HEAD", 8, 25f));
        table.addCell(boldText("UNIT NAME", 8, 25f));
        table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", 8, 25f));

        double grandTotal = 0;
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            String key11 = entry11.getKey();
            List<ReportSubModel> tabData11 = entry11.getValue();

            table.addCell(normalText(key11, 8, 25f));

            double allAmountData = 0;
            for (Integer i = 0; i < tabData11.size(); i++) {

                if (i == 0) {
                    table.addCell(normalText(tabData11.get(i).getUnit(), 8, 25f));
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 8, 25f));
                } else {
                    table.addCell(normalText("", 8, 25f));
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getUnit()), 8, 25f));
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 8, 25f));
                }
                allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());


            }

            table.addCell(boldText("", 8, 25f));
            table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount"), 8, 25f));
            table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 8, 25f));
        }

        table.addCell(boldText(ConverterUtils.addDecimalPoint("Grand Total"), 8, 25f));
        table.addCell(boldText("", 8, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 8, 25f));


        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);
        Chunk approverName = new Chunk("           " + (filePathResponse.getApproveName() + "\n          " + filePathResponse.getApproveRank()), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_BOTTOM);

        document.add(table);
        document.add(paragraph);
        document.close();

    }


    @SuppressWarnings("rawtypes")
    public void createPdfRecipt(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        float[] pointColumnWidths = {100F, 350F, 100F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        table.addCell(boldText("MAJOR/MINOR/SUB HEAD", 8, 25f));
        table.addCell(boldText("DETAILED HEAD", 8, 25f));
        table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", 8, 25f));


        table.addCell(boldText(filePathResponse.getSubHeadKey(), 8, 25f));
        table.addCell(boldText(filePathResponse.getRevenueOrCapital(), 8, 25f));
        table.addCell(boldText("", 8, 25f));


        double grandTotal = 0;
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            String key11 = entry11.getKey();
            List<ReportSubModel> tabData11 = entry11.getValue();

//            table.addCell(normalText(key11, 8, 25f));

            double allAmountData = 0;
            for (Integer i = 0; i < tabData11.size(); i++) {


//                    table.addCell(normalText("", 8, 25f));
//                    table.addCell(normalText(tabData11.get(i).getUnit(), 8, 25f));
//                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 8, 25f));
//
                table.addCell(normalText("", 8, 25f));
                table.addCell(normalText(key11, 8, 25f));
//                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getUnit()), 8, 25f));
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 8, 25f));

                allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());


            }

//            table.addCell(boldText("", 8, 25f));
//            table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount"), 8, 25f));
//            table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 8, 25f));
        }

        table.addCell(boldText("", 8, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint("Grand Total") + "(" + filePathResponse.getRevenueOrCapital() + ")", 8, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 8, 25f));


        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);
        Chunk approverName = new Chunk((filePathResponse.getApproveName() + "\n" + filePathResponse.getApproveRank()), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);


//        document.add(new Paragraph("\n"));
//        Paragraph heading2 = new Paragraph(filePathResponse.getApproveName() + "\n" + filePathResponse.getUnit() + "\n" + filePathResponse.getApproveRank());
//        heading2.setAlignment(Paragraph.ALIGN_RIGHT);
//        document.add(heading2);


        document.add(table);
        document.add(paragraph);
        document.close();

    }


    @SuppressWarnings("rawtypes")
    public void createPdfConsolidateRecipt(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse) throws Exception {

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        float[] pointColumnWidths = {100F, 350F, 100F};
        PdfPTable table = new PdfPTable(3);
        table.setWidths(pointColumnWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        table.addCell(boldText("MAJOR/MINOR/SUB HEAD", 8, 25f));
        table.addCell(boldText("DETAILED HEAD", 8, 25f));
        table.addCell(boldText(filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", 8, 25f));


        double grandTotal = 0;
        for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
            String key11 = entry11.getKey();
            List<ReportSubModel> tabData11 = entry11.getValue();

            if (key11.equalsIgnoreCase("2037")) {
                table.addCell(boldText(key11, 8, 25f));
                table.addCell(boldText("REVENUE", 8, 25f));
                table.addCell(boldText("", 8, 25f));
            } else {
                table.addCell(boldText(key11, 8, 25f));
                table.addCell(boldText("CAPITAL", 8, 25f));
                table.addCell(boldText("", 8, 25f));
            }

            double allAmountData = 0;
            for (Integer i = 0; i < tabData11.size(); i++) {

                table.addCell(normalText("", 8, 25f));
                table.addCell(normalText(tabData11.get(i).getBudgetHead().getSubHeadDescr(), 8, 25f));
//                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getUnit()), 8, 25f));
                table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), 8, 25f));

                allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());

            }


            if (key11.equalsIgnoreCase("2037")) {

                table.addCell(boldText("", 8, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount") + "(REVENUE)", 8, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 8, 25f));

            } else {
                table.addCell(boldText("", 8, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint("Total Amount") + "(CAPITAL)", 8, 25f));
                table.addCell(boldText(ConverterUtils.addDecimalPoint(allAmountData + ""), 8, 25f));
            }


        }

        table.addCell(boldText("", 8, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint("Grand Total"), 8, 25f));
        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 8, 25f));


        Phrase phrase = new Phrase();
        Font font = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);
        Chunk approverName = new Chunk((filePathResponse.getApproveName() + "\n" + filePathResponse.getApproveRank()), font);
        phrase.add(approverName);
        Paragraph paragraph = new Paragraph();
        paragraph.add(phrase);
        paragraph.setAlignment(Element.ALIGN_RIGHT);


//        document.add(new Paragraph("\n"));
//        Paragraph heading2 = new Paragraph(filePathResponse.getApproveName() + "\n" + filePathResponse.getUnit() + "\n" + filePathResponse.getApproveRank());
//        heading2.setAlignment(Paragraph.ALIGN_RIGHT);
//        document.add(heading2);


        document.add(table);
        document.add(paragraph);
        document.close();

    }


    public void createCdaMainReport(HashMap<String, List<CDAReportResponse>> map, CDAReportSubResponse cadSubReport, String path, Float grandTotal, HashMap<String, String> coloumWiseAmount) throws Exception {


        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();
        document.newPage();


        Font font = new Font(Font.FontFamily.COURIER, 15, Font.BOLD);
        Chunk header = new Chunk("\n" + "CDA WISE/OBJECT HEAD WISE CONTROL FIGURES FOR " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear() + "\n" + "\n", font);
        Paragraph preface = new Paragraph();
        preface.setAlignment(Element.ALIGN_CENTER);
        preface.add(header);

        String reOrCapital = "";
        if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
            reOrCapital = "REVENUE";
        } else {
              reOrCapital = "CAPITAL" ;
        }

        Chunk revenue = new Chunk(reOrCapital + "\n" + "\n", font);
        preface.add(revenue);

        Chunk thiredHead = new Chunk("Major Head " + cadSubReport.getMajorHead() + ". Sub Major Head 00. Minor Head " + cadSubReport.getMinorHead() + ") (In " + cadSubReport.getAmountType() + ")" + "\n" + "\n" + "\n" + "\n", font);
        preface.add(thiredHead);


        List<CDAReportResponse> tabData1 = map.get("Sub Head");
//        float[] pointColumnWidths = {50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F, 50F};
        PdfPTable table = new PdfPTable(tabData1.size() + 1);
        table.setWidthPercentage(100);
//        table.setWidths(pointColumnWidths);
        table.setSpacingAfter(10);


        table.addCell(boldText("object", 6, 35f));
        for (Integer i = 0; i < tabData1.size(); i++) {
            table.addCell(boldText(tabData1.get(i).getName(), 5, 20f));
        }


        for (Map.Entry<String, List<CDAReportResponse>> entry : map.entrySet()) {
            String key = entry.getKey();

            if (!key.equalsIgnoreCase("Sub Head")) {
                List<CDAReportResponse> tabData = entry.getValue();
                table.addCell(boldText(key, 5, 35f));
                for (Integer i = 0; i < tabData.size(); i++) {
                    table.addCell(normalText(ConverterUtils.addDecimalPoint(tabData.get(i).getName()), 6, 20f));
                }
            }
        }
        table.addCell(boldText("Grand Total", 5, 20f));
//        for (Integer i = 0; i < tabData1.size(); i++) {
//            if (i == (tabData1.size() - 1)) {
//
//            } else {

        for (Map.Entry<String, String> entry : coloumWiseAmount.entrySet()) {
            String tabData = entry.getValue();
            table.addCell(boldText(ConverterUtils.addDecimalPoint(tabData), 6, 20f));
        }
//            }
//        }

        table.addCell(boldText(ConverterUtils.addDecimalPoint(grandTotal + ""), 6, 20f));


        document.add(preface);
        document.add(table);
        document.close();

    }


    @SuppressWarnings("rawtypes")
    public void createContigentBillReport(CbReportResponse cbReportResponse, String path) throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = dateFormat.format(cbReportResponse.getCbData().getCbDate());
        Float bill = (Float.parseFloat(cbReportResponse.getCurrentBillAmount()));
        bill = bill * 100 / (100 + Float.parseFloat(cbReportResponse.getGetGst()));
        String billFormat = String.format("%.2f", bill);
        bill = Float.parseFloat(billFormat);
        Float gst = (Float.parseFloat(cbReportResponse.getCurrentBillAmount()));
        gst = gst - bill;

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();


        Phrase phrase = new Phrase();
        Font normalFont = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.NORMAL);
        Font font = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.BOLD);


        Chunk billNumber1 = new Chunk("Contingent Bill No. ", normalFont);
        Chunk billNumber2 = new Chunk(cbReportResponse.getCbData().getCbNo() + "                           ", font);
        Chunk billNumber3 = new Chunk("                                             " + " Dated: ", normalFont);
        Chunk billNumber4 = new Chunk(date(dateString) + " \n\n", font);
        phrase.add(billNumber1);
        phrase.add(billNumber2);
        phrase.add(billNumber3);
        phrase.add(billNumber4);

        Font fontHeader = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.BOLD);
        Chunk contigentBill = new Chunk("                                                                                   " + "CONTINGENT BILL  " + " \n\n", fontHeader);
        phrase.add(contigentBill);


        Chunk normalTexet = new Chunk(cbReportResponse.getOnAccountData() + " \n\n", normalFont);
        phrase.add(normalTexet);


        Chunk totalAmount = new Chunk("Total Amount/ Budget allotted                                                                   (INR)  " + cbReportResponse.getAllocatedAmount() + " \n", normalFont);
        phrase.add(totalAmount);

        Chunk progressiveExpen = new Chunk("Progressive expenditure including this bill                                                (INR)  " + cbReportResponse.getExpenditureAmount() + " \n", normalFont);
        phrase.add(progressiveExpen);

        Chunk balanceAmount = new Chunk("Progressive expenditure including this bill                                                (INR)  " + cbReportResponse.getRemeningAmount() + " \n\n", normalFont);
        phrase.add(balanceAmount);


        Chunk authoritya = new Chunk("Authority: (a) " + cbReportResponse.getOnAurthyData() + " \n", normalFont);
        phrase.add(authoritya);


        Chunk authorityb1 = new Chunk("               : (b) " + cbReportResponse.getUnitData().getDescr() + "  Sanction No. ", normalFont);
        Chunk authorityb2 = new Chunk(cbReportResponse.getAuthorityDetails().getAuthority() + " \n\n\n", font);
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
        table.addCell(normalText("Expenditure incurred towards quaterly payment for the 3rd otr from 01 Sep 22 to 30 Nov 22 in respect of Hirring of Designer/Developer IT Manpower (Project-SDOT) through " + cbReportResponse.getCbData().getVendorName() + " vibe Invoiice/bill " + cbReportResponse.getCbData().getInvoiceNO() + " Dated " + cbReportResponse.getCbData().getInvoiceDate(), 10, 50f));
        table.addCell(normalText(ConverterUtils.addDecimalPoint("(INR)" + bill.toString()), 9, 50f));


        table.addCell(normalText("", 9, 25f));
        table.addCell(normalText("GST " + cbReportResponse.getCbData().getGst() + " % ", 9, 25f));
        table.addCell(normalText(ConverterUtils.addDecimalPoint("(INR)" + String.format("%.2f", gst)), 9, 25f));


        table.addCell(normalText("", 9, 25f));
        table.addCell(normalText("TOTAL ", 9, 25f));
        table.addCell(normalText(ConverterUtils.addDecimalPoint("(INR)" + cbReportResponse.getCurrentBillAmount()), 9, 25f));

        table.addCell(normalText("", 9, 25f));
        table.addCell(boldText("Amount in words (Rupees " + convertDecimaltoString(cbReportResponse.getCurrentBillAmount()) + ")", 9, 25f));
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
        Chunk certifyc2 = new Chunk(cbReportResponse.getBudgetHead().getMajorHead(), font);
        Chunk certifyc3 = new Chunk(" Customs, Sub Major Head 00, Minor Head ", normalFont);
        Chunk certifyc4 = new Chunk(cbReportResponse.getBudgetHead().getMinorHead(), font);
        Chunk certifyc5 = new Chunk(", -preventive & other function 06 CG Organisation under Sub Head: ", normalFont);
        Chunk certifyc6 = new Chunk(cbReportResponse.getBudgetHead().getSubHeadDescr(), font);
        Chunk certifyc7 = new Chunk(".Category Code ", normalFont);
        Chunk certifyc8 = new Chunk(cbReportResponse.getBudgetHead().getBudgetCodeId() + " \n", font);
        phraseFooter.add(certifyc1);
        phraseFooter.add(certifyc2);
        phraseFooter.add(certifyc3);
        phraseFooter.add(certifyc4);
        phraseFooter.add(certifyc5);
        phraseFooter.add(certifyc6);
        phraseFooter.add(certifyc7);
        phraseFooter.add(certifyc8);

        Chunk certifyd = new Chunk("(d) The expenditure has been incurred in the interest of the state." + " \n \n", normalFont);
        phraseFooter.add(certifyd);


        Chunk veriferSign = new Chunk("                                                                                                                                                 " + cbReportResponse.getVerifer().getFullName() + " \n" + "                                                                                                                                          " + cbReportResponse.getVerifer().getRank() + " \n\n", normalFont);
        phraseFooter.add(veriferSign);


        Font counterSign = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        Chunk counterSigner = new Chunk("                                                           COUNTERSIGNED" + " \n" + " \n", counterSign);
        phraseFooter.add(counterSigner);


        Chunk footer11 = new Chunk("Coast Guard Headquarters", font);
        Chunk footer12 = new Chunk("                                                                                                    " + cbReportResponse.getApprover().getFullName() + " \n", normalFont);
        phraseFooter.add(footer11);
        phraseFooter.add(footer12);


        Chunk footer21 = new Chunk("National Stadium Complex", font);
        Chunk footer22 = new Chunk("                                                                                               " + cbReportResponse.getApprover().getRank() + " \n", normalFont);
        phraseFooter.add(footer21);
        phraseFooter.add(footer22);


        Chunk footer3 = new Chunk("New Delhi-110001 " + "\n\n", font);
        phraseFooter.add(footer3);


        Chunk fileNumber = new Chunk("File No. " + cbReportResponse.getCbData().getFileID() + "\n", font);
        phraseFooter.add(fileNumber);


        Chunk fileDated = new Chunk("Date " + date(cbReportResponse.getCbData().getFileDate()) + "\n\n", font);
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

        float x = Float.parseFloat(str);
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
