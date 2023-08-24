package com.sdd.utils;


import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.sdd.entities.HrData;
import com.sdd.exception.SDDException;
import com.sdd.response.CDAReportResponse;
import com.sdd.response.CDAReportSubResponse;
import com.sdd.response.FilePathResponse;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.List;

@Component
public class DocxGenaratorUtil {


    @SuppressWarnings("rawtypes")
    public void createDocAllocation(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse, HrData hrData) throws Exception {

        try {


            XWPFDocument document = new XWPFDocument();
            FileOutputStream out = new FileOutputStream(new File(path));


            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("ALLOCATION REPORT " + "( " + hrData.getUnit().toUpperCase() + " )");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);


            XWPFTable table = document.createTable();
            table.setWidth("100%");
//                XWPFParagraph para = document.createParagraph();
//                XWPFRun run = para.createRun();

            XWPFTableRow tableRowOne = table.getRow(0);
            if (filePathResponse.getSubHeadKey().contains("2037")) {

                XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
                boldText(paragraphtableRowOne.createRun(), 10, "OBJECT HEAD", true);
            } else {
                XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
                boldText(paragraphtableRowOne.createRun(), 10, "DETAILED HEAD", true);
            }


            XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 10, "UNIT NAME", true);

            XWPFParagraph paragraphtableRowOne11 = tableRowOne.addNewTableCell().addParagraph();
            boldText(paragraphtableRowOne11.createRun(), 10, filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", true);

            double grandTotal = 0f;
            for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
                String key11 = entry11.getKey();
                List<ReportSubModel> tabData11 = entry11.getValue();

                XWPFTableRow tableRow = table.createRow();
                tableRow.getCell(0).setText(key11);

                double allAmountData = 0f;
                for (Integer i = 0; i < tabData11.size(); i++) {

                    if (i == 0) {
//                            tableRow.getCell(1).setText(tabData11.get(i).getUnit());
                        XWPFParagraph paragraph = tableRow.getCell(1).addParagraph();
                        normalText(paragraph.createRun(), 10, tabData11.get(i).getUnit(), false);

                        XWPFParagraph paragraph11 = tableRow.getCell(2).addParagraph();
                        normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), false);
                        paragraph11.setAlignment(ParagraphAlignment.RIGHT);

                    } else {
                        XWPFTableRow tableRow11 = table.createRow();
                        tableRow11.getCell(0).setText("");

                        XWPFParagraph paragraph = tableRow11.getCell(1).addParagraph();
                        normalText(paragraph.createRun(), 10, tabData11.get(i).getUnit(), false);

                        XWPFParagraph paragraph11 = tableRow11.getCell(2).addParagraph();
                        normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), false);
                        paragraph11.setAlignment(ParagraphAlignment.RIGHT);
                    }

                    allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                    grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());

                }

                XWPFTableRow latRow = table.createRow();
                XWPFParagraph total1 = latRow.getCell(1).addParagraph();
                boldText(total1.createRun(), 10, "Total Amount", true);
                XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
                boldText(total1111.createRun(), 10, ConverterUtils.addDecimalPoint(allAmountData + ""), true);
                total1111.setAlignment(ParagraphAlignment.RIGHT);
            }


            XWPFTableRow latRow = table.createRow();
            XWPFParagraph total1 = latRow.getCell(0).addParagraph();
            boldText(total1.createRun(), 10, "Grand Total", true);
            XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
            boldText(total1111.createRun(), 10, ConverterUtils.addDecimalPoint(grandTotal + ""), true);
            total1111.setAlignment(ParagraphAlignment.RIGHT);

            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();

            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, filePathResponse.getApproveName() + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);

            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, filePathResponse.getApproveRank() + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();


        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), e.toString());
        }
    }


    @SuppressWarnings("rawtypes")
    public void createDocRecipt(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse, HrData hrData) throws Exception {

        try {


            XWPFDocument document = new XWPFDocument();
            FileOutputStream out = new FileOutputStream(new File(path));

            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("RECIEPT REPORT " + "( " + hrData.getUnit().toUpperCase() + " )");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            XWPFTable table = document.createTable();
            table.setWidth("100%");
//                XWPFParagraph para = document.createParagraph();
//                XWPFRun run = para.createRun();


            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 10, "MAJOR/MINOR/SUB HEAD", true);


            if (filePathResponse.getSubHeadKey().equalsIgnoreCase("2037")) {
                XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
                boldText(paragraphtableRowOne1.createRun(), 10, "REVENUE OBJECT HEAD", true);

            } else {
                XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
                boldText(paragraphtableRowOne1.createRun(), 10, "CAPITAL DETAILED HEAD", true);
            }


            XWPFParagraph paragraphtableRowOne11 = tableRowOne.addNewTableCell().addParagraph();
            boldText(paragraphtableRowOne11.createRun(), 10, filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", true);


            XWPFTableRow tableRowOne11 = table.createRow();
//            XWPFParagraph paragraphtableRowOne22 = tableRowOne11.getCell(0).addParagraph();
//            boldText(paragraphtableRowOne22.createRun(), 10, filePathResponse.getSubHeadKey(), true);


            if (filePathResponse.getSubHeadKey().equalsIgnoreCase("2037")) {
                XWPFParagraph paragraphtableRowOne22 = tableRowOne11.getCell(0).addParagraph();
                boldText(paragraphtableRowOne22.createRun(), 10, filePathResponse.getSubHeadKey() + "\n00.102.06", true);
            } else {
                XWPFParagraph paragraphtableRowOne22 = tableRowOne11.getCell(0).addParagraph();
                boldText(paragraphtableRowOne22.createRun(), 10, filePathResponse.getSubHeadKey() + "\n00.037.01", true);
            }


            XWPFParagraph paragraphtableRowOne111 = tableRowOne11.getCell(1).addParagraph();
            boldText(paragraphtableRowOne111.createRun(), 10, filePathResponse.getRevenueOrCapital(), true);

            XWPFParagraph paragraphtableRowOne1111 = tableRowOne11.getCell(2).addParagraph();
            boldText(paragraphtableRowOne1111.createRun(), 10, "", true);


            double grandTotal = 0f;
            for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
                String key11 = entry11.getKey();
                List<ReportSubModel> tabData11 = entry11.getValue();

//                XWPFTableRow tableRow = table.createRow();
//                tableRow.getCell(0).setText(key11);

                double allAmountData = 0f;
                for (Integer i = 0; i < tabData11.size(); i++) {

                    XWPFTableRow tableRow11 = table.createRow();
                    tableRow11.getCell(0).setText("");

                    XWPFParagraph paragraph = tableRow11.getCell(1).addParagraph();
                    normalText(paragraph.createRun(), 10, key11, false);

                    XWPFParagraph paragraph11 = tableRow11.getCell(2).addParagraph();
                    normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), false);
                    paragraph11.setAlignment(ParagraphAlignment.RIGHT);

                    allAmountData = allAmountData + Float.parseFloat(tabData11.get(i).getAmount());
                    grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());

                }
            }


            XWPFTableRow latRow = table.createRow();
            XWPFParagraph total1 = latRow.getCell(1).addParagraph();
            boldText(total1.createRun(), 10, "Grand Total", true);
            XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
            boldText(total1111.createRun(), 10, ConverterUtils.addDecimalPoint(grandTotal + ""), true);
            total1111.setAlignment(ParagraphAlignment.RIGHT);

            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();

            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, filePathResponse.getApproveName() + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);


            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, filePathResponse.getApproveRank() + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);


            document.write(out);
            out.close();
            document.close();

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), e.toString());
        }
    }


    @SuppressWarnings("rawtypes")
    public void createDocConsolidateRecipt(HashMap<String, List<ReportSubModel>> hashMap, String path, FilePathResponse filePathResponse, HrData hrData) throws Exception {

        try {


            XWPFDocument document = new XWPFDocument();
            FileOutputStream out = new FileOutputStream(new File(path));

            XWPFTable table = document.createTable();
            table.setWidth("100%");
//                XWPFParagraph para = document.createParagraph();
//                XWPFRun run = para.createRun();

            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("RECEIPT REPORT " + "( " + hrData.getUnit().toUpperCase() + " )");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);


            String key = "";
            for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
                key = entry11.getKey();
            }


//            if (key.equalsIgnoreCase("2037")) {

            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 10, "MAJOR/MINOR/SUB HEAD", true);

            XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 10, "HEAD", true);

            XWPFParagraph paragraphtableRowOne11 = tableRowOne.addNewTableCell().addParagraph();
            boldText(paragraphtableRowOne11.createRun(), 10, filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", true);

//            } else {
//
//                XWPFTableRow tableRowOne = table.getRow(0);
//                XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
//                boldText(paragraphtableRowOne.createRun(), 10, "MAJOR/MINOR/SUB HEAD", true);
//
//                XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
//                boldText(paragraphtableRowOne1.createRun(), 10, "DETAILED HEAD", true);
//
//                XWPFParagraph paragraphtableRowOne11 = tableRowOne.addNewTableCell().addParagraph();
//                boldText(paragraphtableRowOne11.createRun(), 10, filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", true);
//
//            }


            double grandTotal = 0f;
            for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
                String key11 = entry11.getKey();
                List<ReportSubModel> tabData11 = entry11.getValue();

                XWPFTableRow tableRow = table.createRow();
                XWPFParagraph tableView = tableRow.getCell(0).addParagraph();


                if (key11.equalsIgnoreCase("2037")) {
                    boldText(tableView.createRun(), 10, key11 + "\n00.102.06", true);
                    XWPFParagraph tableView11 = tableRow.getCell(1).addParagraph();
                    boldText(tableView11.createRun(), 10, "REVENUE OBJECT HEAD", true);
                } else {
                    boldText(tableView.createRun(), 10, key11 + "\n00.037.01", true);
                    XWPFParagraph tableView22 = tableRow.getCell(1).addParagraph();
                    boldText(tableView22.createRun(), 10, "CAPITAL DETAILED HEAD", true);
                }


                double allAmountData = 0f;
                for (Integer i = 0; i < tabData11.size(); i++) {

                    XWPFTableRow tableRow11 = table.createRow();
                    tableRow11.getCell(0).setText("");

                    XWPFParagraph paragraph = tableRow11.getCell(1).addParagraph();
                    normalText(paragraph.createRun(), 10, tabData11.get(i).getBudgetHead().getSubHeadDescr(), false);

                    XWPFParagraph paragraph11 = tableRow11.getCell(2).addParagraph();
                    normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint(tabData11.get(i).getAmount()), false);
                    paragraph11.setAlignment(ParagraphAlignment.RIGHT);

                    allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
                    grandTotal = grandTotal + Double.parseDouble(tabData11.get(i).getAmount());

                }

                if (key11.equalsIgnoreCase("2037")) {

                    XWPFTableRow latRow = table.createRow();
                    XWPFParagraph total1 = latRow.getCell(1).addParagraph();
                    boldText(total1.createRun(), 10, "Total Amount" + "(REVENUE)", true);
                    XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
                    boldText(total1111.createRun(), 10, ConverterUtils.addDecimalPoint(allAmountData + ""), true);
                    total1111.setAlignment(ParagraphAlignment.RIGHT);
                } else {

                    XWPFTableRow latRow = table.createRow();
                    XWPFParagraph total1 = latRow.getCell(1).addParagraph();
                    boldText(total1.createRun(), 10, "Total Amount" + "(CAPITAL)", true);
                    XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
                    boldText(total1111.createRun(), 10, ConverterUtils.addDecimalPoint(allAmountData + ""), true);
                    total1111.setAlignment(ParagraphAlignment.RIGHT);
                }
            }


            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {

                XWPFTableRow latRow = table.createRow();
                XWPFParagraph total1 = latRow.getCell(1).addParagraph();
                boldText(total1.createRun(), 10, "Grand Total", true);
                XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
                boldText(total1111.createRun(), 10, ConverterUtils.addDecimalPoint(grandTotal + ""), true);
                total1111.setAlignment(ParagraphAlignment.RIGHT);

            }


            int maxlength = ConverterUtils.getMaximumLength(filePathResponse.getApproveName().length(), (filePathResponse.getApproveRank()).length());


            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();

            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, ConverterUtils.addSpacaeInStringInWord(filePathResponse.getApproveName(), maxlength), true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);

            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, ConverterUtils.addSpacaeInStringInWord(filePathResponse.getApproveRank(), maxlength), true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);


            document.write(out);
            out.close();
            document.close();


        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), e.toString());
        }
    }


    public void createCdaMainReportDoc(HashMap<String, List<CDAReportResponse>> map, CDAReportSubResponse cadSubReport, String path, double grandTotal, HashMap<String, String> coloumWiseAmount, HrData hrData) throws Exception {

        try {

            XWPFDocument document = new XWPFDocument();
            FileOutputStream out = new FileOutputStream(new File(path));

            CTDocument1 ctDocument = document.getDocument();
            CTBody ctBody = ctDocument.getBody();
            CTSectPr ctSectPr = (ctBody.isSetSectPr()) ? ctBody.getSectPr() : ctBody.addNewSectPr();
            CTPageSz ctPageSz = (ctSectPr.isSetPgSz()) ? ctSectPr.getPgSz() : ctSectPr.addNewPgSz();
            ctPageSz.setOrient(STPageOrientation.LANDSCAPE);

            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();

            List<CDAReportResponse> tabData1 = map.get("Sub Head");
            if (tabData1.size() > 12) {
                ctPageSz.setW(java.math.BigInteger.valueOf(Math.round(120 * 1440))); //11 inches
                ctPageSz.setH(java.math.BigInteger.valueOf(Math.round(8.5 * 1440))); //8.5 inches
            }


//            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
//            XWPFTable table11 = header.createTable(1,0);
//            XWPFTableRow tableRowOne11 = table11.getRow(0);
//            XWPFParagraph paragraphtableRowOne99 = tableRowOne11.addNewTableCell().addParagraph();
//            boldText(paragraphtableRowOne99.createRun(), 10, "object", true);
//
//            for (Integer i = 0; i < tabData1.size(); i++) {
//                Boolean isNumber = ConverterUtils.isNumber(tabData1.get(i).getName() + "");
//                if (isNumber) {
//                    XWPFParagraph paragraphtableRowOne1 = tableRowOne11.addNewTableCell().addParagraph();
//                    boldText(paragraphtableRowOne1.createRun(), 10, ConverterUtils.addDecimalPoint(tabData1.get(i).getName()), true);
//                    paragraphtableRowOne1.setAlignment(ParagraphAlignment.RIGHT);
//                } else {
//                    XWPFParagraph paragraphtableRowOne1 = tableRowOne11.addNewTableCell().addParagraph();
//                    boldText(paragraphtableRowOne1.createRun(), 10, ConverterUtils.addDecimalPoint(tabData1.get(i).getName()), true);
//                }
//            }


            String reOrCapital = "";
            if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
                reOrCapital = "REVENUE";
            } else {
                reOrCapital = "CAPITAL";
            }


            mainParagraph = document.createParagraph();
            mainParagraph.setAlignment(ParagraphAlignment.CENTER);


            if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
                boldText(mainParagraph.createRun(), 20, "\n" + "CDA WISE/OBJECT HEAD WISE CONTROL FIGURES FOR " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear(), true);
                mainParagraph.createRun().addBreak();
            } else {
                boldText(mainParagraph.createRun(), 20, "\n" + "CDA WISE/DETAILED HEAD WISE CONTROL FIGURES FOR " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear(), true);
                mainParagraph.createRun().addBreak();
            }


            mainParagraph = document.createParagraph();
            mainParagraph.setAlignment(ParagraphAlignment.CENTER);
            boldText(mainParagraph.createRun(), 20, reOrCapital, true);
            mainParagraph.createRun().addBreak();


            mainParagraph = document.createParagraph();
            mainParagraph.setAlignment(ParagraphAlignment.CENTER);
            boldText(mainParagraph.createRun(), 20, "Major Head " + cadSubReport.getMajorHead() + ", Sub Major Head 00, Minor Head " + cadSubReport.getMinorHead() + ") (In " + cadSubReport.getAmountType() + ")", true);

            mainParagraph.createRun().addBreak();
            mainParagraph.createRun().addBreak();
            mainParagraph.createRun().addBreak();

            XWPFTable table = document.createTable();
            table.setWidth("100%");


            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 10, "object", true);


            for (Integer i = 0; i < tabData1.size(); i++) {
                Boolean isNumber = ConverterUtils.isNumber(tabData1.get(i).getName() + "");
                if (isNumber) {
                    XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
                    boldText(paragraphtableRowOne1.createRun(), 10, ConverterUtils.addDecimalPoint(tabData1.get(i).getName()), true);
                    paragraphtableRowOne1.setAlignment(ParagraphAlignment.RIGHT);
                } else {
                    XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
                    boldText(paragraphtableRowOne1.createRun(), 10, ConverterUtils.addDecimalPoint(tabData1.get(i).getName()), true);
                }
            }

            for (Map.Entry<String, List<CDAReportResponse>> entry : map.entrySet()) {
                String key = entry.getKey();

                if (!key.equalsIgnoreCase("Sub Head")) {
                    List<CDAReportResponse> tabData = entry.getValue();
                    XWPFTableRow tableRow11 = table.createRow();
//                    tableRow11.getCell(0).setText(key);
                    XWPFParagraph paragraphtableRowOne11 = tableRow11.getCell(0).addParagraph();
                    boldText(paragraphtableRowOne11.createRun(), 10, key, true);


                    for (Integer i = 0; i < tabData.size(); i++) {

                        Boolean isNumber = ConverterUtils.isNumber(tabData.get(i).getName() + "");
                        if (isNumber) {
                            XWPFParagraph paragraph11 = tableRow11.getCell(i + 1).addParagraph();
                            normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint(tabData.get(i).getName()), false);
                            paragraph11.setAlignment(ParagraphAlignment.RIGHT);
                        } else {
                            XWPFParagraph paragraph11 = tableRow11.getCell(i + 1).addParagraph();
                            normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint(tabData.get(i).getName()), false);
                        }


                    }
                }
            }


            int ih = 0;
            XWPFTableRow tableRow11 = table.createRow();
            tableRow11.getCell(ih).setText("Grand Total");

//            for (Integer i = 0; i < tabData1.size(); i++) {
//                if (i == (tabData1.size() - 1)) {
//                    } else {
//
//                }
//            }

            double grandAllTotal11 = 0;
            for (Map.Entry<String, String> entry : coloumWiseAmount.entrySet()) {
                String tabData = entry.getValue();
                Boolean isNumber = ConverterUtils.isNumber(tabData + "");
                if (isNumber) {
                    XWPFParagraph paragraph11 = tableRow11.getCell(ih + 1).addParagraph();
                    normalText(paragraph11.createRun(), 10, tabData, false);
                    ih++;
                    paragraph11.setAlignment(ParagraphAlignment.RIGHT);
                } else {
                    XWPFParagraph paragraph11 = tableRow11.getCell(ih + 1).addParagraph();
                    normalText(paragraph11.createRun(), 10, tabData, false);
                    ih++;
                }

                if (isNumber) {
                    grandAllTotal11 = grandAllTotal11 + Double.parseDouble(ConverterUtils.addDecimalPoint(tabData));
                } else {
                    grandAllTotal11 = grandAllTotal11 + Double.parseDouble(ConverterUtils.addDecimalPoint(tabData));
                }

            }

            XWPFParagraph paragraph11 = tableRow11.getCell(ih + 1).addParagraph();
            normalText(paragraph11.createRun(), 10, ConverterUtils.addDecimalPoint((grandAllTotal11) + ""), false);


            document.write(out);
            out.close();
            document.close();

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), e.toString());
        }
    }


    public void createReserveFundnReport(HashMap<String, List<CDAReportResponse>> map, CDAReportSubResponse cadSubReport, String path, double grandTotal, double allocationGrandTotal, HrData hrData) throws Exception {

        try {

            XWPFDocument document = new XWPFDocument();
            FileOutputStream out = new FileOutputStream(new File(path));
            String reOrCapital = "";


            if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
                reOrCapital = "REVENUE";
            } else {
                reOrCapital = "CAPITAL";
            }

            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();

            mainParagraph = document.createParagraph();
            mainParagraph.setAlignment(ParagraphAlignment.CENTER);
            boldText(mainParagraph.createRun(), 17, "\n" + "RESERVE FUND " + cadSubReport.getAllocationType() + " " + cadSubReport.getFinYear() + " " + "( " + hrData.getUnit().toUpperCase() + " )", true);
            mainParagraph.createRun().addBreak();


            mainParagraph = document.createParagraph();
            mainParagraph.setAlignment(ParagraphAlignment.CENTER);
            boldText(mainParagraph.createRun(), 17, reOrCapital, true);
            mainParagraph.createRun().addBreak();


            mainParagraph = document.createParagraph();
            mainParagraph.setAlignment(ParagraphAlignment.CENTER);
            boldText(mainParagraph.createRun(), 17, "Major Head " + cadSubReport.getMajorHead() + ", Sub Major Head 00, Minor Head " + cadSubReport.getMinorHead() + ") (In " + cadSubReport.getAmountType() + ")", true);

            mainParagraph.createRun().addBreak();
            mainParagraph.createRun().addBreak();
            mainParagraph.createRun().addBreak();

            XWPFTable table = document.createTable();
            table.setWidth("100%");


            if (cadSubReport.getMajorHead().equalsIgnoreCase("2037")) {
                XWPFTableRow tableRowOne = table.getRow(0);
                XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
                boldText(paragraphtableRowOne.createRun(), 13, "Revenue Object Head", true);

                XWPFParagraph paragraphtableRowTwo = tableRowOne.addNewTableCell().addParagraph();
                boldText(paragraphtableRowTwo.createRun(), 13, "Allocation Amount", true);

                XWPFParagraph paragraphtableRowThree = tableRowOne.addNewTableCell().addParagraph();
                boldText(paragraphtableRowThree.createRun(), 13, "Reserve Fund", true);

            } else {
                XWPFTableRow tableRowOne = table.getRow(0);
                XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
                boldText(paragraphtableRowOne.createRun(), 13, "Capital Detailed Head", true);

                XWPFParagraph paragraphtableRowTwo = tableRowOne.addNewTableCell().addParagraph();
                boldText(paragraphtableRowTwo.createRun(), 13, "Allocation Amount", true);

                XWPFParagraph paragraphtableRowThree = tableRowOne.addNewTableCell().addParagraph();
                boldText(paragraphtableRowThree.createRun(), 13, "Reserve Fund", true);

            }


            for (Map.Entry<String, List<CDAReportResponse>> entry : map.entrySet()) {
                String key = entry.getKey();

                List<CDAReportResponse> tabData = entry.getValue();
                XWPFTableRow tableRow11 = table.createRow();

                XWPFParagraph paragraphtableRowOne11 = tableRow11.getCell(0).addParagraph();
                boldText(paragraphtableRowOne11.createRun(), 12, key, true);


                XWPFParagraph paragraph11 = tableRow11.getCell(1).addParagraph();
                normalText(paragraph11.createRun(), 12, ConverterUtils.addDecimalPoint(tabData.get(0).getAllocationAmount()), false);
                paragraph11.setAlignment(ParagraphAlignment.RIGHT);

                XWPFParagraph paragraph123 = tableRow11.getCell(2).addParagraph();
                normalText(paragraph123.createRun(), 12, ConverterUtils.addDecimalPoint(tabData.get(0).getName()), false);
                paragraph123.setAlignment(ParagraphAlignment.RIGHT);
            }

            XWPFTableRow tableRow11 = table.createRow();
            XWPFParagraph paragraph1234 = tableRow11.getCell(0).addParagraph();
            boldText(paragraph1234.createRun(), 13, ConverterUtils.addDecimalPoint("Grand Total"), true);


            XWPFParagraph paragraph11 = tableRow11.getCell(1).addParagraph();
            normalText(paragraph11.createRun(), 13, ConverterUtils.addDecimalPoint(allocationGrandTotal + ""), true);
            paragraph11.setAlignment(ParagraphAlignment.RIGHT);

            XWPFParagraph paragraph111 = tableRow11.getCell(2).addParagraph();
            normalText(paragraph111.createRun(), 13, ConverterUtils.addDecimalPoint(grandTotal + ""), true);
            paragraph111.setAlignment(ParagraphAlignment.RIGHT);

            document.write(out);
            out.close();
            document.close();

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), e.toString());
        }
    }


    private void boldText(XWPFRun run, int fontSize, String text, boolean bold) {
        run.setFontFamily("Calibre LIght");
        run.setFontSize(fontSize);
        run.setColor("000000");
        run.setText(text);
        run.setBold(bold);

    }

    private void normalText(XWPFRun run, int fontSize, String text, boolean bold) {
        run.setFontFamily("Calibre LIght");
        run.setFontSize(fontSize);
        run.setColor("000000");
        run.setText(text);
        run.setBold(bold);
    }


}
