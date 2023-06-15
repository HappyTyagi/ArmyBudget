package com.sdd.service.Impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.sdd.entities.*;
import com.sdd.entities.repository.*;
import com.sdd.exception.SDDException;
import com.sdd.jwt.HeaderUtils;
import com.sdd.jwt.JwtUtils;
import com.sdd.jwtParse.TokenParseData;
import com.sdd.request.CDAReportRequest;
import com.sdd.request.ReportRequest;
import com.sdd.request.SubHeadWiseAllocationReportReq;
import com.sdd.request.UnitWiseAllocationReport;
import com.sdd.response.*;
import com.sdd.service.MangeReportService;
import com.sdd.utils.*;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


@Service
public class MangeReportImpl implements MangeReportService {

    private static final String UTF_8 = "UTF-8";

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    MangeInboxOutBoxRepository mangeInboxOutBoxRepository;


    @Autowired
    BudgetAllocationRepository budgetAllocationRepository;

    @Autowired
    BudgetFinancialYearRepository budgetFinancialYearRepository;

    @Autowired
    AuthorityRepository authorityRepository;

    @Autowired
    AllocationRepository allocationRepository;


    @Autowired
    SubHeadRepository subHeadRepository;


    @Autowired
    CdaParkingRepository cdaParkingRepository;


    @Autowired
    AmountUnitRepository amountUnitRepository;


    @Autowired
    CdaParkingTransRepository cdaParkingTransRepository;

    @Autowired
    private BudgetAllocationDetailsRepository budgetAllocationDetailsRepository;

    @Autowired
    CgUnitRepository cgUnitRepository;

    @Autowired
    HrDataRepository hrDataRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private HeaderUtils headerUtils;

    @Autowired
    private PdfGenaratorUtil pdfGenaratorUtil;

    @Autowired
    private PdfGenaratorUtilMain pdfGenaratorUtilMain;

    @Autowired
    private DocxGenaratorUtil docxGenaratorUtil;

    @Autowired
    private ContigentBillRepository contigentBillRepository;

    @Autowired
    private BudgetRebaseRepository budgetRebaseRepository;

    @Autowired
    private CgStationRepository cgStationRepository;


    @Override
    public ApiResponse<List<FilePathResponse>> getAllocationReport(String authGroupId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }


        String fileName = "AllocationReport" + hrData.getUnitId();

        HashMap<String, List<ReportSubModel>> hashMap = new LinkedHashMap<>();
        List<BudgetAllocationDetails> budgetAllocationReport = budgetAllocationDetailsRepository.findByAuthGroupIdAndIsDeleteOrderByTransactionIdAsc(authGroupId, "0");
//        List<BudgetAllocationDetails> budgetAllocationReport = budgetAllocationDetailsRepository.findByAuthGroupIdAndIsDelete(authGroupId, "0");


        List<MangeInboxOutbox> mangeInboxOutbox = mangeInboxOutBoxRepository.findByGroupId(authGroupId);

        if (mangeInboxOutbox.size() > 0) {

            if (mangeInboxOutbox.get(0).getIsBgcg().equalsIgnoreCase("BR")) {
                fileName = "BudgetReceipt" + hrData.getUnitId();
            } else {
                fileName = "AllocationReport" + hrData.getUnitId();
            }
        }


        if (budgetAllocationReport.size() <= 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND");
        }


        for (Integer j = 0; j < budgetAllocationReport.size(); j++) {

            BudgetHead budgetHead = subHeadRepository.findByBudgetCodeId(budgetAllocationReport.get(j).getSubHead());
            CgUnit cgUnit = cgUnitRepository.findByUnit(budgetAllocationReport.get(j).getToUnit());
            AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(budgetAllocationReport.get(j).getAmountType());

            if (hashMap.containsKey(budgetHead.getSubHeadDescr())) {

                List<ReportSubModel> reportMaindata = hashMap.get(budgetHead.getSubHeadDescr());
                ReportSubModel subModel = new ReportSubModel();
                subModel.setType(budgetAllocationReport.get(j).getAllocTypeId());
                subModel.setRemark(budgetAllocationReport.get(j).getTransactionId());
                subModel.setUnit(cgUnit.getDescr());
                subModel.setAmountType(amountUnit.getAmountType());
                subModel.setBudgetHead(budgetHead);
                subModel.setAmount(budgetAllocationReport.get(j).getAllocationAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYear());


                if (Double.parseDouble(budgetAllocationReport.get(j).getAllocationAmount()) != 0) {
                    reportMaindata.add(subModel);
                    hashMap.put(budgetHead.getSubHeadDescr(), reportMaindata);
                }
            } else {
                List<ReportSubModel> reportMaindata = new ArrayList<ReportSubModel>();
                ReportSubModel subModel = new ReportSubModel();
                subModel.setType(budgetAllocationReport.get(j).getAllocTypeId());
                subModel.setRemark(budgetAllocationReport.get(j).getTransactionId());
                subModel.setUnit(cgUnit.getDescr());
                subModel.setBudgetHead(budgetHead);
                subModel.setAmount(budgetAllocationReport.get(j).getAllocationAmount());
                subModel.setAmountType(amountUnit.getAmountType());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYear());

                if (Double.parseDouble(budgetAllocationReport.get(j).getAllocationAmount()) != 0) {
                    reportMaindata.add(subModel);
                    hashMap.put(budgetHead.getSubHeadDescr(), reportMaindata);
                }
            }
        }


        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();
        FilePathResponse filePathResponse = new FilePathResponse();
        List<ReportSubModel> tabData = new ArrayList<ReportSubModel>();
        String key = "";
        for (Map.Entry<String, List<ReportSubModel>> entry : hashMap.entrySet()) {
            key = entry.getKey();
            tabData.addAll(entry.getValue());


            List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
            if (hrDataList.size() == 0) {
                throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
            }

            String approverPId = "";

            for (Integer k = 0; k < hrDataList.size(); k++) {
                HrData findHrData = hrDataList.get(k);
                if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                    approverPId = findHrData.getPid();
                    filePathResponse.setApproveName(findHrData.getFullName());
                    filePathResponse.setApproveRank(findHrData.getRank());
                }
            }

            if (approverPId.isEmpty()) {
                throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO APPROVE ROLE FOUND THIS UNIT.PLEASE ADD  ROLE FIRST");
            }

        }


        AllocationType allocationType = allocationRepository.findByAllocTypeId(tabData.get(0).getType());
        BudgetFinancialYear budgetFinancialYear = budgetFinancialYearRepository.findBySerialNo(tabData.get(0).getFinYear());

        filePathResponse.setFinYear(budgetFinancialYear.getFinYear());
        filePathResponse.setUnit(tabData.get(0).getUnit());
        filePathResponse.setSubHead(key);
        filePathResponse.setType(allocationType.getAllocDesc());
        filePathResponse.setAmountType(tabData.get(0).getAmountType());
        filePathResponse.setRemark(tabData.get(0).getRemark());


        try {
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
            pdfGenaratorUtilMain.createPdfAllocation(hashMap, filePath, filePathResponse);
            filePathResponse.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
            filePathResponse.setFileName(fileName);
            dtoList.add(filePathResponse);

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
        }

        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }


    @Override
    public ApiResponse<List<FilePathResponse>> getAllocationReportDoc(String authGroupId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }


        HashMap<String, List<ReportSubModel>> hashMap = new LinkedHashMap<>();
//        List<BudgetAllocation> budgetAllocationReport = budgetAllocationRepository.findByAuthGroupIdAndIsFlagOrderBySubHeadAsc(authGroupId, "0");

        List<BudgetAllocationDetails> budgetAllocationReport = budgetAllocationDetailsRepository.findByAuthGroupIdAndIsDeleteOrderByTransactionIdAsc(authGroupId, "0");

        if (budgetAllocationReport.size() <= 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND");
        }

        for (Integer j = 0; j < budgetAllocationReport.size(); j++) {

            BudgetHead budgetHead = subHeadRepository.findByBudgetCodeId(budgetAllocationReport.get(j).getSubHead());
            CgUnit cgUnit = cgUnitRepository.findByUnit(budgetAllocationReport.get(j).getToUnit());
            AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(budgetAllocationReport.get(j).getAmountType());

            if (hashMap.containsKey(budgetHead.getSubHeadDescr())) {

                List<ReportSubModel> reportMaindata = hashMap.get(budgetHead.getSubHeadDescr());
                ReportSubModel subModel = new ReportSubModel();
                subModel.setType(budgetAllocationReport.get(j).getAllocTypeId());
                subModel.setRemark(budgetAllocationReport.get(j).getTransactionId());
                subModel.setUnit(cgUnit.getDescr());
                subModel.setBudgetHead(budgetHead);
                subModel.setAmountType(amountUnit.getAmountType());
                subModel.setAmount(budgetAllocationReport.get(j).getAllocationAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYear());

                if (Double.parseDouble(budgetAllocationReport.get(j).getAllocationAmount()) != 0) {
                    reportMaindata.add(subModel);
                    hashMap.put(budgetHead.getSubHeadDescr(), reportMaindata);
                }
            } else {
                List<ReportSubModel> reportMaindata = new ArrayList<ReportSubModel>();
                ReportSubModel subModel = new ReportSubModel();
                subModel.setType(budgetAllocationReport.get(j).getAllocTypeId());
                subModel.setRemark(budgetAllocationReport.get(j).getTransactionId());
                subModel.setUnit(cgUnit.getDescr());
                subModel.setBudgetHead(budgetHead);
                subModel.setAmount(budgetAllocationReport.get(j).getAllocationAmount());
                subModel.setAmountType(amountUnit.getAmountType());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYear());

                if (Double.parseDouble(budgetAllocationReport.get(j).getAllocationAmount()) != 0) {
                    reportMaindata.add(subModel);
                    hashMap.put(budgetHead.getSubHeadDescr(), reportMaindata);
                }
            }
        }

        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        FilePathResponse filePathResponse = new FilePathResponse();
        List<ReportSubModel> tabData = new ArrayList<ReportSubModel>();
        String key = "";
        for (Map.Entry<String, List<ReportSubModel>> entry : hashMap.entrySet()) {
            key = entry.getKey();
            tabData.addAll(entry.getValue());


            List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
            if (hrDataList.size() == 0) {
                throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
            }

            String approverPId = "";

            for (Integer k = 0; k < hrDataList.size(); k++) {
                HrData findHrData = hrDataList.get(k);
                if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                    approverPId = findHrData.getPid();
                    filePathResponse.setApproveName(findHrData.getFullName());
                    filePathResponse.setApproveRank(findHrData.getRank());
                }
            }

            if (approverPId.isEmpty()) {
                throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO APPROVE ROLE FOUND THIS UNIT.PLEASE ADD  ROLE FIRST");
            }

        }


        AllocationType allocationType = allocationRepository.findByAllocTypeId(tabData.get(0).getType());
        BudgetFinancialYear budgetFinancialYear = budgetFinancialYearRepository.findBySerialNo(tabData.get(0).getFinYear());

        filePathResponse.setFinYear(budgetFinancialYear.getFinYear());
        filePathResponse.setUnit(tabData.get(0).getUnit());
        filePathResponse.setSubHead(key);
        filePathResponse.setType(allocationType.getAllocDesc());
        filePathResponse.setAmountType(tabData.get(0).getAmountType());
        filePathResponse.setRemark(tabData.get(0).getRemark());


        try {

            String fileName = "AllocationReport" + hrData.getUnitId();
            List<MangeInboxOutbox> mangeInboxOutbox = mangeInboxOutBoxRepository.findByGroupId(authGroupId);

            if (mangeInboxOutbox.size() > 0) {

                if (mangeInboxOutbox.get(0).getIsBgcg().equalsIgnoreCase("BR")) {
                    fileName = "BudgetReceipt" + hrData.getUnitId();
                } else {
                    fileName = "AllocationReport" + hrData.getUnitId();
                }
            }
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + fileName + ".docx";
            docxGenaratorUtil.createDocAllocation(hashMap, filePath, filePathResponse);
            filePathResponse.setPath(HelperUtils.FILEPATH + fileName + ".docx");
            filePathResponse.setFileName(fileName);
            dtoList.add(filePathResponse);

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
        }

        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });


    }


    @Override
    public ApiResponse<List<FilePathResponse>> getAllocationReportRevised(ReportRequest reportRequest) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }


        if (reportRequest.getBudgetFinancialYearId() == null || reportRequest.getBudgetFinancialYearId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "FINANCIAL YEAR ID CAN NOT BE BLANK");
        }

        if (reportRequest.getUnitId() == null || reportRequest.getUnitId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "UNIT ID CAN NOT BE BLANK");
        }


        CgUnit cgFromUnit = cgUnitRepository.findByUnit(reportRequest.getUnitId());
        if (cgFromUnit == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TO UNIT ID");
        }

        BudgetFinancialYear budgetFinancialYear = budgetFinancialYearRepository.findBySerialNo(reportRequest.getBudgetFinancialYearId());
        if (budgetFinancialYear == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID FINANCIAL YEAR ID");
        }

        HashMap<String, List<ReportSubModel>> hashMap = new HashMap<>();
        List<BudgetAllocation> budgetAllocationReport = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndStatusAndIsFlagAndIsBudgetRevision(reportRequest.getUnitId(), reportRequest.getBudgetFinancialYearId(), reportRequest.getBudgetFinancialYearId(), "Approved", "Approved", "0", "0");

        if (budgetAllocationReport.size() <= 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND");
        }
        String fileName = hrData.getUnitId() + System.currentTimeMillis();


        for (Integer j = 0; j < budgetAllocationReport.size(); j++) {

            if (hashMap.containsKey(budgetAllocationReport.get(j).getSubHead())) {
                List<ReportSubModel> reportMaindata = hashMap.get(budgetAllocationReport.get(j).getSubHead());
                ReportSubModel subModel = new ReportSubModel();
                subModel.setUnit(budgetAllocationReport.get(j).getToUnit());
                subModel.setType(budgetAllocationReport.get(j).getAllocationTypeId());
                subModel.setRemark(budgetAllocationReport.get(j).getRefTransId());
                subModel.setAmount(budgetAllocationReport.get(j).getAllocationAmount());
                subModel.setRevisedAmount(budgetAllocationReport.get(j).getRevisedAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYear());
                reportMaindata.add(subModel);
                hashMap.put(budgetAllocationReport.get(j).getSubHead(), reportMaindata);
            } else {
                List<ReportSubModel> reportMaindata = new ArrayList<ReportSubModel>();
                ReportSubModel subModel = new ReportSubModel();
                subModel.setUnit(budgetAllocationReport.get(j).getToUnit());
                subModel.setType(budgetAllocationReport.get(j).getAllocationTypeId());
                subModel.setRemark(budgetAllocationReport.get(j).getRefTransId());
                subModel.setAmount(budgetAllocationReport.get(j).getAllocationAmount());
                subModel.setRevisedAmount(budgetAllocationReport.get(j).getRevisedAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYear());
                reportMaindata.add(subModel);
                hashMap.put(budgetAllocationReport.get(j).getSubHead(), reportMaindata);
            }
        }


        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        for (Map.Entry<String, List<ReportSubModel>> entry : hashMap.entrySet()) {
            String key = entry.getKey();
            List<ReportSubModel> tabData = entry.getValue();
            FilePathResponse filePathResponse = new FilePathResponse();

            filePathResponse.setFinYear(tabData.get(0).getFinYear());
            filePathResponse.setUnit(tabData.get(0).getUnit());
            filePathResponse.setSubHead(key);
            filePathResponse.setType(tabData.get(0).getType());
            filePathResponse.setRemark(tabData.get(0).getRemark());

            try {
                FilePathResponse dto = new FilePathResponse();
                String templateName = "report-allocation-revised.html";
                File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                File file = new File(filePath);
                pdfGenaratorUtil.createPdf(templateName, hashMap, file);
                dto.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                dto.setFileName(fileName);
                dtoList.add(dto);


            } catch (Exception e) {
                throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
            }

        }

        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getContingentBillReport(ReportRequest reportRequest) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        CbReportResponse cbReportResponse = new CbReportResponse();


        if (reportRequest.getCbId() == null || reportRequest.getCbId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "CB ID CAN NOT BE BLANK");
        }


        ContigentBill cbData = contigentBillRepository.findByCbIdAndIsFlagAndIsUpdate(reportRequest.getCbId(), "0", "0");

        if (cbData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND.");
        }

        String fileName = "ContingentBill" + hrData.getUnitId() + cbData.getCbId();


        double allocationAmount = 0;
        double balanceAmount = 0;
        List<BudgetAllocation> modBudgetAllocations = budgetAllocationRepository.findByToUnitAndSubHeadAndIsFlagAndIsBudgetRevision(hrData.getUnitId(), cbData.getBudgetHeadID(), "0", "0");
        if (modBudgetAllocations.size() == 0) {

        } else {
            allocationAmount = 0;
            for (Integer i = 0; i < modBudgetAllocations.size(); i++) {
                AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(modBudgetAllocations.get(i).getAmountType());
                allocationAmount = allocationAmount + (Double.parseDouble(modBudgetAllocations.get(i).getAllocationAmount()) * amountUnit.getAmount());
//                balanceAmount = balanceAmount + (Double.parseDouble(modBudgetAllocations.get(i).getBalanceAmount()) * amountUnit.getAmount());
            }
        }


        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverCbPId = "";
        String veriferCbPId = "";
        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.CBVERIFER)) {
                veriferCbPId = findHrData.getPid();
            }
            if (findHrData.getRoleId().contains(HelperUtils.CBAPPROVER)) {
                approverCbPId = findHrData.getPid();
            }
        }

        if (approverCbPId.isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO CB APPROVE ROLE FOUND THIS UNIT.PLEASE ADD  ROLE FIRST");
        }
        if (veriferCbPId.isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO CB VERIFIER ROLE FOUND THIS UNIT.PLEASE ADD  ROLE FIRST");
        }


        double expenditure = 0;
        List<ContigentBill> cbExpendure = contigentBillRepository.findByCbUnitIdAndBudgetHeadIDAndIsFlagAndIsUpdate(hrData.getUnitId(), cbData.getBudgetHeadID(), "0", "0");
        if (cbExpendure.size() == 0) {

        } else {
            expenditure = 0;
            for (Integer i = 0; i < cbExpendure.size(); i++) {
                expenditure = expenditure + Double.parseDouble(cbExpendure.get(i).getCbAmount());
            }
        }
        List<Authority> authorityDetails = authorityRepository.findByAuthGroupId(cbData.getAuthGroupId());
        CgUnit unit = cgUnitRepository.findByUnit(cbData.getCbUnitId());
        BudgetHead budgetHead = subHeadRepository.findByBudgetCodeId(cbData.getBudgetHeadID());
        HrData approverId = hrDataRepository.findByPidAndIsActive(approverCbPId, "1");
        HrData verifer = hrDataRepository.findByPidAndIsActive(veriferCbPId, "1");

        cbReportResponse.setAuthorityDetails(authorityDetails.get(0));
        cbReportResponse.setApprover(approverId);
        cbReportResponse.setVerifer(verifer);

        cbReportResponse.setOnAccountData(cbData.getOnAccountOf());
        cbReportResponse.setGetGst(cbData.getGst());
        cbReportResponse.setOnAurthyData(cbData.getAuthorityDetails());
        cbReportResponse.setExpenditureAmount(String.format("%.2f", expenditure));
        cbReportResponse.setCurrentBillAmount(String.format("%.2f", Double.parseDouble(cbData.getCbAmount())));
        cbReportResponse.setAllocatedAmount(String.format("%.2f", allocationAmount));
        cbReportResponse.setCbData(cbData);
        cbReportResponse.setUnitData(unit);
        cbReportResponse.setBudgetHead(budgetHead);
        cbReportResponse.setBalanceAmount(String.format("%.2f", (balanceAmount - expenditure)));
        cbReportResponse.setRemeningAmount(String.format("%.2f", ((allocationAmount - expenditure))));

        String hindiAmount = ConverterUtils.convert((new Double(cbData.getCbAmount())).longValue());
        cbReportResponse.setHindiAmount(hindiAmount);

        HashMap<String, List<ReportSubModel>> hashMap = new HashMap<>();
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        try {
            FilePathResponse dto = new FilePathResponse();
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
//            pdfGenaratorUtil.createCbReportPdfSample(templateName, cbReportResponse, file);
            pdfGenaratorUtilMain.createContigentBillReport(cbReportResponse, filePath);
            dto.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
            dto.setFileName(fileName);
            dtoList.add(dto);

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
        }

//        }

        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });

    }


    @Override
    public ApiResponse<List<FilePathResponse>> getContingentBillReportDoc(ReportRequest reportRequest) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        CbReportResponse cbReportResponse = new CbReportResponse();


        if (reportRequest.getCbId() == null || reportRequest.getCbId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "CB ID CAN NOT BE BLANK");
        }


        ContigentBill cbData = contigentBillRepository.findByCbIdAndIsFlagAndIsUpdate(reportRequest.getCbId(), "0", "0");

        if (cbData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND.");
        }

        String fileName = "ContingentBill" + hrData.getUnitId() + cbData.getCbId();


        double allocationAmount = 0;
        double balanceAmount = 0;
        List<BudgetAllocation> modBudgetAllocations = budgetAllocationRepository.findByToUnitAndSubHeadAndIsFlagAndIsBudgetRevision(hrData.getUnitId(), cbData.getBudgetHeadID(), "0", "0");
        if (modBudgetAllocations.size() == 0) {

        } else {
            allocationAmount = 0;
            for (Integer i = 0; i < modBudgetAllocations.size(); i++) {
                AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(modBudgetAllocations.get(i).getAmountType());
                allocationAmount = allocationAmount + (Double.parseDouble(modBudgetAllocations.get(i).getAllocationAmount()) * amountUnit.getAmount());
//                balanceAmount = balanceAmount + (Double.parseDouble(modBudgetAllocations.get(i).getBalanceAmount()) * amountUnit.getAmount());
            }
        }


        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverCbPId = "";
        String veriferCbPId = "";
        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.CBVERIFER)) {
                veriferCbPId = findHrData.getPid();
            }
            if (findHrData.getRoleId().contains(HelperUtils.CBAPPROVER)) {
                approverCbPId = findHrData.getPid();
            }
        }

        if (approverCbPId.isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO CB APPROVE ROLE FOUND THIS UNIT.PLEASE ADD  ROLE FIRST");
        }
        if (veriferCbPId.isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO CB VERIFIER ROLE FOUND THIS UNIT.PLEASE ADD  ROLE FIRST");
        }


        double expenditure = 0;
        List<ContigentBill> cbExpendure = contigentBillRepository.findByCbUnitIdAndBudgetHeadIDAndIsFlagAndIsUpdate(hrData.getUnitId(), cbData.getBudgetHeadID(), "0", "0");
        if (cbExpendure.size() == 0) {

        } else {
            expenditure = 0;
            for (Integer i = 0; i < cbExpendure.size(); i++) {
                expenditure = expenditure + Double.parseDouble(cbExpendure.get(i).getCbAmount());
            }
        }
        List<Authority> authorityDetails = authorityRepository.findByAuthGroupId(cbData.getAuthGroupId());
        CgUnit unit = cgUnitRepository.findByUnit(cbData.getCbUnitId());
        BudgetHead budgetHead = subHeadRepository.findByBudgetCodeId(cbData.getBudgetHeadID());
        HrData approverId = hrDataRepository.findByPidAndIsActive(approverCbPId, "1");
        HrData verifer = hrDataRepository.findByPidAndIsActive(veriferCbPId, "1");

        cbReportResponse.setAuthorityDetails(authorityDetails.get(0));
        cbReportResponse.setApprover(approverId);
        cbReportResponse.setVerifer(verifer);

        cbReportResponse.setOnAccountData(cbData.getOnAccountOf());
        cbReportResponse.setGetGst(cbData.getGst());
        cbReportResponse.setOnAurthyData(cbData.getAuthorityDetails());
        cbReportResponse.setExpenditureAmount(String.format("%.2f", expenditure));
        cbReportResponse.setCurrentBillAmount(String.format("%.2f", Double.parseDouble(cbData.getCbAmount())));
        cbReportResponse.setAllocatedAmount(String.format("%.2f", allocationAmount));
        cbReportResponse.setCbData(cbData);
        cbReportResponse.setUnitData(unit);
        cbReportResponse.setBudgetHead(budgetHead);
        cbReportResponse.setBalanceAmount(String.format("%.2f", (balanceAmount - expenditure)));
        cbReportResponse.setRemeningAmount(String.format("%.2f", ((allocationAmount - expenditure))));

        String hindiAmount = ConverterUtils.convert(Long.parseLong(cbData.getCbAmount()));
        cbReportResponse.setHindiAmount(hindiAmount);

        HashMap<String, List<ReportSubModel>> hashMap = new HashMap<>();
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        try {
            XWPFDocument document = new XWPFDocument();
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + fileName + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));

            XWPFTable table = document.createTable();
            table.setWidth("100%");
//                XWPFParagraph para = document.createParagraph();
//                XWPFRun run = para.createRun();


//            XWPFTableRow tableRowOne = table.getRow(0);
//            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
//            boldText(paragraphtableRowOne.createRun(), 10, "SUB HEAD", true);
//
//            XWPFParagraph paragraphtableRowOne1 = tableRowOne.addNewTableCell().addParagraph();
//            boldText(paragraphtableRowOne1.createRun(), 10, "UNIT NAME", true);
//
//            XWPFParagraph paragraphtableRowOne11 = tableRowOne.addNewTableCell().addParagraph();
//            boldText(paragraphtableRowOne11.createRun(), 10, filePathResponse.getType() + " (" + filePathResponse.getFinYear() + ") \n" + " ALLOCATION (In " + filePathResponse.getAmountType() + ")", true);


//            for (Map.Entry<String, List<ReportSubModel>> entry11 : hashMap.entrySet()) {
//                String key11 = entry11.getKey();
//                List<ReportSubModel> tabData11 = entry11.getValue();
//
//                XWPFTableRow tableRow = table.createRow();
//                tableRow.getCell(0).setText(key11);
//                double allAmountData = 0;
//                for (Integer i = 0; i < tabData11.size(); i++) {
//
//                    if (i == 0) {
////                            tableRow.getCell(1).setText(tabData11.get(i).getUnit());
//                        XWPFParagraph paragraph = tableRow.getCell(1).addParagraph();
//                        normalText(paragraph.createRun(), 10, tabData11.get(i).getUnit(), false);
//
////                            tableRow.getCell(2).setText(tabData11.get(i).getAmount());
//                        XWPFParagraph paragraph11 = tableRow.getCell(2).addParagraph();
//                        normalText(paragraph11.createRun(), 10, tabData11.get(i).getAmount(), false);
//
//                    } else {
//                        XWPFTableRow tableRow11 = table.createRow();
//                        tableRow11.getCell(0).setText("");
//                        XWPFParagraph paragraph = tableRow11.getCell(1).addParagraph();
//                        normalText(paragraph.createRun(), 10, tabData11.get(i).getUnit(), false);
//
//                        XWPFParagraph paragraph11 = tableRow11.getCell(2).addParagraph();
//                        normalText(paragraph11.createRun(), 10, tabData11.get(i).getAmount(), false);
//                    }
//
//
//                    allAmountData = allAmountData + Double.parseDouble(tabData11.get(i).getAmount());
//
//                    XWPFTableRow latRow = table.createRow();
//
//                    XWPFParagraph total1 = latRow.getCell(1).addParagraph();
//                    boldText(total1.createRun(), 10, "Total Amount", true);
//
//                    XWPFParagraph total1111 = latRow.getCell(2).addParagraph();
//                    boldText(total1111.createRun(), 10, allAmountData + "", true);
//
//                }
//            }


//            //create first row
//            XWPFParagraph mainParagraph = document.createParagraph();
//            mainParagraph = document.createParagraph();
//            mainParagraph.createRun().addBreak();
//            mainParagraph = document.createParagraph();
//            boldText(mainParagraph.createRun(), 10, filePathResponse.getApproveName() + "", true);
//            mainParagraph = document.createParagraph();
//            normalText(mainParagraph.createRun(), 10, filePathResponse.getApproveRank() + "", true);


            // Line 2
            // Creating object for line 2
//                XWPFRun line2 = paragraph.createRun();

            // Formatting line1 by setting italic
//                line2.setText("Formatted with Italics");
//                line2.setItalic(true);
//                line2.addBreak();

            // Line 3
            // Creating object for line 3
//                XWPFRun line3 = paragraph.createRun();

            // Formatting line3 by setting
            // color & font size
//                line3.setColor("73fc03");
//                line3.setFontSize(20);
//                line3.setText(" Formatted with Color");

            // Step 6: Saving changes to document
            document.write(out);

            // Step 7: Closing the connections
            out.close();
            document.close();


            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + fileName + ".docx");
            dto.setFileName(fileName);
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
        }

//        }

        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });

    }


    @Override
    public ApiResponse<List<FilePathResponse>> getAllocationReportUnitWise(ReportRequest reportRequest) {
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        return null;
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getAllocationReportSubHead(ReportRequest reportRequest) {
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        return null;
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getReAllocationReport(ReportRequest reportRequest) {
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        return null;
    }

    @Override
    public ApiResponse<FilePathResponse> getCdaParkingReport(CDAReportRequest cdaReportRequest) {
        HashMap<String, List<CDAReportResponse>> allCdaData = new LinkedHashMap<String, List<CDAReportResponse>>();
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        FilePathResponse dtoList = new FilePathResponse();
        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        String fileName = "CdaParkingReport" + hrData.getUnitId();
        CDAReportSubResponse cadSubReport = new CDAReportSubResponse();


        if (cdaReportRequest.getCdaType() == null || cdaReportRequest.getCdaType().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "CDA TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getFinancialYearId() == null || cdaReportRequest.getFinancialYearId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "FIN YEAD ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getAmountType() == null || cdaReportRequest.getAmountType().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "AMOUNT TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getMajorHead() == null || cdaReportRequest.getMajorHead().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "MAJOR HEAD ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getSubHeadType() == null || cdaReportRequest.getSubHeadType().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "SUB HEAD TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getAllocationTypeId() == null || cdaReportRequest.getAllocationTypeId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "ALLOCATION TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getMinorHead() == null || cdaReportRequest.getMinorHead().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "MINOR HEAD ID CAN NOT BE BLANK:key - minorHead");
        }

        BudgetFinancialYear budgetFinancialYear = budgetFinancialYearRepository.findBySerialNo(cdaReportRequest.getFinancialYearId());
        if (budgetFinancialYear == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID FINANCIAL YEAR ID");
        }

        AllocationType allocationType = allocationRepository.findByAllocTypeId(cdaReportRequest.getAllocationTypeId());
        if (allocationType == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID ALLOCATION TYPE ID");
        }

        AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(cdaReportRequest.getAmountType());
        if (allocationType == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID ALLOCATION TYPE ID");
        }

        cadSubReport.setFinYear(budgetFinancialYear.getFinYear());
        cadSubReport.setMajorHead(cdaReportRequest.getMajorHead());
        cadSubReport.setMinorHead(cdaReportRequest.getMinorHead());
        cadSubReport.setAllocationType(allocationType.getAllocDesc());
        cadSubReport.setAmountType(amountUnit.getAmountType());


        HashMap<String, String> coloumWiseAmount = new LinkedHashMap<String, String>();


        Float grandTotal = 0f;
        if (cdaReportRequest.getBudgetHeadId() == null && cdaReportRequest.getUnitId() == null) {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID) && cdaReportRequest.getCdaType().contains("112233")) {

                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
                List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findAllByOrderByCdaNameAsc();
                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);


                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId());

                            Float amount = 0f;

                            for (int m = 0; m < cdaData.size(); m++) {
                                if (cdaData.get(m).getTotalParkingAmount() == null) {
                                    amount = amount;
                                } else {
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                    grandTotal = grandTotal + amount;
                                }
                            }


                            CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                            if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                                Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                                coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                            } else {
                                coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                            }


                            totalAmount = totalAmount + amount;
                            cdaReportResponse = new CDAReportResponse();
                            cdaReportResponse.setName(amount + "");
                            cdaReportList.add(cdaReportResponse);
                        }
                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("CDA Wise Report(ALL CDA)");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                    pdfGenaratorUtilMain.createCdaMainReport(allCdaData, cadSubReport, filePath, grandTotal, coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);


                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }

            } else if (cdaReportRequest.getCdaType().contains("112244")) {


                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
                List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                            Float amount = 0f;

                            for (int m = 0; m < cdaData.size(); m++) {
                                if (cdaData.get(m).getTotalParkingAmount() == null) {
                                    amount = amount;
                                } else {
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                    grandTotal = grandTotal + amount;
                                }
                            }


                            CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                            if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                                Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                                coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                            } else {
                                coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                            }

                            totalAmount = totalAmount + amount;
                            cdaReportResponse = new CDAReportResponse();
                            cdaReportResponse.setName(amount + "");
                            cdaReportList.add(cdaReportResponse);
                        }
                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("CDA Wise Report(Mumbai CDA)");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                    File file = new File(filePath);
                    pdfGenaratorUtilMain.createCdaMainReport(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);
                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }


            } else {

                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
                CdaParking cdaParkingTotalList = cdaParkingRepository.findByGinNo(cdaReportRequest.getCdaType());

                if (cdaParkingTotalList == null) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID CDA ID YEAR ID");
                }


                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(cdaParkingTotalList.getCdaName());
                cdaReportList.add(cdaReportResponse);

                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList != null) {
//                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                        List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                        Float amount = 0f;

                        for (int m = 0; m < cdaData.size(); m++) {
                            if (cdaData.get(m).getTotalParkingAmount() == null) {
                                amount = amount;
                            } else {
                                amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                grandTotal = grandTotal + amount;
                            }

                        }

                        CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.getGinNo());

                        if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                            Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                            coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                        } else {
                            coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                        }

                        totalAmount = totalAmount + amount;
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName(amount + "");
                        cdaReportList.add(cdaReportResponse);

                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("CDA Wise Report(ALL CDA)");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                    File file = new File(filePath);
                    pdfGenaratorUtilMain.createCdaMainReport(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);


                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }

            }


        } else {

//            01     SubHeadWise
//            02     UnitWise


            if (cdaReportRequest.getReportType() == null || cdaReportRequest.getReportType().isEmpty()) {
                throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "REPORT TYPE CAN NOT BE BLANK");
            }

            if (cdaReportRequest.getReportType().equalsIgnoreCase("01")) {


                if (cdaReportRequest.getBudgetHeadId() == null || cdaReportRequest.getBudgetHeadId().isEmpty()) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "BUDGET HEAD ID CAN NOT BE BLANK");
                }


                BudgetHead subHead = subHeadRepository.findByBudgetCodeId(cdaReportRequest.getBudgetHeadId());
                if (subHead == null) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID BUDGET HEAD ID");
                }

                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<CdaParking> cdaParkingTotalList = new ArrayList<CdaParking>();

                if (cdaReportRequest.getCdaType().contains("112233")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findAllByOrderByCdaNameAsc();
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else if (cdaReportRequest.getCdaType().contains("112244")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else {
                    CdaParking singleCda = cdaParkingRepository.findByGinNo(cdaReportRequest.getCdaType());
                    cdaParkingTotalList.add(singleCda);
                }


                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);


                cdaReportList = new ArrayList<>();
                cdaReportResponse = new CDAReportResponse();

                cdaReportResponse.setName(subHead.getSubHeadDescr());

                Float totalAmount = 0f;
                if (cdaParkingTotalList.size() > 0) {
                    for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                        List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                        Float amount = 0f;

                        for (int m = 0; m < cdaData.size(); m++) {
                            if (cdaData.get(m).getTotalParkingAmount() == null) {
                                amount = amount;
                            } else {
                                amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                grandTotal = grandTotal + amount;
                            }
                        }

                        CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                        if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                            Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                            coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                        } else {
                            coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                        }

                        totalAmount = totalAmount + amount;
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName(amount + "");
                        cdaReportList.add(cdaReportResponse);
                    }
                } else {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName("0");
                    cdaReportList.add(cdaReportResponse);
                }

                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(totalAmount + "");
                cdaReportResponse.setReportType("SubHead Wise CDA Report");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);

                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                    File file = new File(filePath);
                    pdfGenaratorUtilMain.createCdaMainReport(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);
                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }


            } else if (cdaReportRequest.getReportType().equalsIgnoreCase("02")) {

                if (cdaReportRequest.getUnitId() == null || cdaReportRequest.getUnitId().isEmpty()) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "UNIT ID CAN NOT BE BLANK");
                }

                CgUnit cgUnit = cgUnitRepository.findByUnit(cdaReportRequest.getUnitId());
                if (cgUnit == null) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID UNIT ID");
                }


                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());


                List<CdaParking> cdaParkingTotalList = new ArrayList<CdaParking>();

                if (cdaReportRequest.getCdaType().contains("112233")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findAllByOrderByCdaNameAsc();
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else if (cdaReportRequest.getCdaType().contains("112244")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else {
                    CdaParking singleCda = cdaParkingRepository.findByGinNo(cdaReportRequest.getCdaType());
                    cdaParkingTotalList.add(singleCda);
                }


                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeIdAndUnitId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId(), cgUnit.getUnit());
                            Float amount = 0f;

                            for (int m = 0; m < cdaData.size(); m++) {
                                if (cdaData.get(m).getTotalParkingAmount() == null) {
                                    amount = amount;
                                } else {
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                    grandTotal = grandTotal + amount;
                                }
                            }

                            CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                            if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                                Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                                coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                            } else {
                                coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                            }

                            totalAmount = totalAmount + amount;
                            cdaReportResponse = new CDAReportResponse();
                            cdaReportResponse.setName(amount + "");
                            cdaReportList.add(cdaReportResponse);
                        }
                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("Unit Wise CDA Report");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                    pdfGenaratorUtilMain.createCdaMainReport(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);
                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }
            }


        }


        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<FilePathResponse>() {
        });
    }


    @Override
    public ApiResponse<FilePathResponse> getCdaParkingReportDoc(CDAReportRequest cdaReportRequest) {
        HashMap<String, List<CDAReportResponse>> allCdaData = new LinkedHashMap<String, List<CDAReportResponse>>();
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        FilePathResponse dtoList = new FilePathResponse();
        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        String fileName = "CdaParkingReport" + hrData.getUnitId();
        CDAReportSubResponse cadSubReport = new CDAReportSubResponse();


        if (cdaReportRequest.getCdaType() == null || cdaReportRequest.getCdaType().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "CDA TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getFinancialYearId() == null || cdaReportRequest.getFinancialYearId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "FIN YEAD ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getAmountType() == null || cdaReportRequest.getAmountType().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "AMOUNT TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getMajorHead() == null || cdaReportRequest.getMajorHead().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "MAJOR HEAD ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getSubHeadType() == null || cdaReportRequest.getSubHeadType().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "SUB HEAD TYPE ID CAN NOT BE BLANK");
        }

        if (cdaReportRequest.getAllocationTypeId() == null || cdaReportRequest.getAllocationTypeId().isEmpty()) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "ALLOCATION TYPE ID CAN NOT BE BLANK");
        }

        BudgetFinancialYear budgetFinancialYear = budgetFinancialYearRepository.findBySerialNo(cdaReportRequest.getFinancialYearId());
        if (budgetFinancialYear == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID FINANCIAL YEAR ID");
        }

        AllocationType allocationType = allocationRepository.findByAllocTypeId(cdaReportRequest.getAllocationTypeId());
        if (allocationType == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID ALLOCATION TYPE ID");
        }

        AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(cdaReportRequest.getAmountType());
        if (allocationType == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID ALLOCATION TYPE ID");
        }

        cadSubReport.setFinYear(budgetFinancialYear.getFinYear());
        cadSubReport.setMajorHead(cdaReportRequest.getMajorHead());
        cadSubReport.setMinorHead(cdaReportRequest.getMajorHead());
        cadSubReport.setAllocationType(allocationType.getAllocDesc());
        cadSubReport.setAmountType(amountUnit.getAmountType());

        HashMap<String, String> coloumWiseAmount = new LinkedHashMap<String, String>();
        Float grandTotal = 0f;
        if (cdaReportRequest.getBudgetHeadId() == null && cdaReportRequest.getUnitId() == null) {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID) && cdaReportRequest.getCdaType().contains("112233")) {

                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
                List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findAllByOrderByCdaNameAsc();
                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                            Float amount = 0f;

                            for (int m = 0; m < cdaData.size(); m++) {
                                if (cdaData.get(m).getTotalParkingAmount() == null) {
                                    amount = amount;
                                } else {
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                    grandTotal = grandTotal + amount;
                                }

                            }

                            CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                            if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                                Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                                coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                            } else {
                                coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                            }

                            totalAmount = totalAmount + amount;
                            cdaReportResponse = new CDAReportResponse();
                            cdaReportResponse.setName(amount + "");
                            cdaReportList.add(cdaReportResponse);
                        }
                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("CDA Wise Report(ALL CDA)");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".docx";
                    File file = new File(filePath);
                    docxGenaratorUtil.createCdaMainReportDoc(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".docx");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);


                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }

            }


//            else if (cdaReportRequest.getCdaType().contains("All CDA")) {
//
//                List<CDAReportResponse> cdaReportList = new ArrayList<>();
//                CDAReportResponse cdaReportResponse = new CDAReportResponse();
//
//                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
//                List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findAllByOrderByCdaNameAsc();
//                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
//                    cdaReportResponse = new CDAReportResponse();
//                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
//                    cdaReportList.add(cdaReportResponse);
//                }
//                allCdaData.put("Sub Head", cdaReportList);
//
//
//                for (int i = 0; i < subHeadsData.size(); i++) {
//                    cdaReportList = new ArrayList<>();
//                    cdaReportResponse = new CDAReportResponse();
//
//                    BudgetHead subHead = subHeadsData.get(i);
//                    cdaReportResponse.setName(subHead.getSubHeadDescr());
//
//
//                    CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
//                    List<CgUnit> unitDataList = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
//                    Float totalAmount = 0f;
//                    for (int s = 0; s < cdaParkingTotalList.size(); s++) {
//                        if (cdaParkingTotalList.size() > 0) {
//                            for (int k = 0; k < cdaParkingTotalList.size(); k++) {
//                                List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndUnitId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", unitDataList.get(s).getUnit());
//                                Float amount = 0f;
//                                for (int m = 0; m < cdaData.size(); m++) {
//                                    if (cdaData.get(m).getTotalParkingAmount() == null) {
//                                        amount = amount;
//                                    } else {
//                                        amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
//                                        grandTotal = grandTotal + amount;
//                                    }
//                                }
//
//                                totalAmount = totalAmount + amount;
//                                cdaReportResponse = new CDAReportResponse();
//                                cdaReportResponse.setName(amount + "");
//                                cdaReportList.add(cdaReportResponse);
//                            }
//                        } else {
//                            cdaReportResponse = new CDAReportResponse();
//                            cdaReportResponse.setName("0");
//                            cdaReportList.add(cdaReportResponse);
//                        }
//                    }
//                    cdaReportResponse = new CDAReportResponse();
//                    cdaReportResponse.setName(totalAmount + "");
//                    cdaReportList.add(cdaReportResponse);
//                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
//                }
//
//
//                try {
//
//                    
//                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
//                    if (!folder.exists()) {
//                        folder.mkdirs();
//                    }
//                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
//                    File file = new File(filePath);
//                  pdfGenaratorUtilMain.createCdaMainReport(allCdaData, cadSubReport, filePath, grandTotal);
//                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
//                    dtoList.setFileName(fileName);
//
//                } catch (Exception e) {
//                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
//                }
//
//            }
            else if (cdaReportRequest.getCdaType().contains("112244")) {


                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
                List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                            Float amount = 0f;

                            for (int m = 0; m < cdaData.size(); m++) {
                                if (cdaData.get(m).getTotalParkingAmount() == null) {
                                    amount = amount;
                                } else {
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                    grandTotal = grandTotal + amount;
                                }
                            }

                            CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                            if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                                Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                                coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                            } else {
                                coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                            }
                            totalAmount = totalAmount + amount;
                            cdaReportResponse = new CDAReportResponse();
                            cdaReportResponse.setName(amount + "");
                            cdaReportList.add(cdaReportResponse);
                        }
                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("CDA Wise Report(Mumbai CDA)");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".docx";
                    docxGenaratorUtil.createCdaMainReportDoc(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".docx");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);
                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }


            } else {

                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());
                CdaParking cdaParkingTotalList = cdaParkingRepository.findByGinNo(cdaReportRequest.getCdaType());

                if (cdaParkingTotalList == null) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID CDA ID YEAR ID");
                }


                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(cdaParkingTotalList.getCdaName());
                cdaReportList.add(cdaReportResponse);

                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList != null) {
//                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                        List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                        Float amount = 0f;

                        for (int m = 0; m < cdaData.size(); m++) {
                            if (cdaData.get(m).getTotalParkingAmount() == null) {
                                amount = amount;
                            } else {
                                amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                grandTotal = grandTotal + amount;
                            }

                        }

                        CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.getGinNo());

                        if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                            Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                            coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                        } else {
                            coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                        }

                        totalAmount = totalAmount + amount;
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName(amount + "");
                        cdaReportList.add(cdaReportResponse);

                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("CDA Wise Report(ALL CDA)");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".docx";
                    File file = new File(filePath);
                    docxGenaratorUtil.createCdaMainReportDoc(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".docx");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);


                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }

            }


        } else {

//            01     SubHeadWise
//            02     UnitWise


            if (cdaReportRequest.getReportType() == null || cdaReportRequest.getReportType().isEmpty()) {
                throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "REPORT TYPE CAN NOT BE BLANK");
            }

            if (cdaReportRequest.getReportType().equalsIgnoreCase("01")) {


                if (cdaReportRequest.getBudgetHeadId() == null || cdaReportRequest.getBudgetHeadId().isEmpty()) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "BUDGET HEAD ID CAN NOT BE BLANK");
                }


                BudgetHead subHead = subHeadRepository.findByBudgetCodeId(cdaReportRequest.getBudgetHeadId());
                if (subHead == null) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID BUDGET HEAD ID");
                }

                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<CdaParking> cdaParkingTotalList = new ArrayList<CdaParking>();

                if (cdaReportRequest.getCdaType().contains("112233")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findAllByOrderByCdaNameAsc();
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else if (cdaReportRequest.getCdaType().contains("112244")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else {
                    CdaParking singleCda = cdaParkingRepository.findByGinNo(cdaReportRequest.getCdaType());
                    cdaParkingTotalList.add(singleCda);
                }


                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);


                cdaReportList = new ArrayList<>();
                cdaReportResponse = new CDAReportResponse();

                cdaReportResponse.setName(subHead.getSubHeadDescr());

                Float totalAmount = 0f;
                if (cdaParkingTotalList.size() > 0) {
                    for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                        List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId());
                        Float amount = 0f;

                        for (int m = 0; m < cdaData.size(); m++) {
                            if (cdaData.get(m).getTotalParkingAmount() == null) {
                                amount = amount;
                            } else {
                                amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                grandTotal = grandTotal + amount;
                            }
                        }
                        CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                        if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                            Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                            coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                        } else {
                            coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                        }


                        totalAmount = totalAmount + amount;
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName(amount + "");
                        cdaReportList.add(cdaReportResponse);
                    }
                } else {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName("0");
                    cdaReportList.add(cdaReportResponse);
                }

                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(totalAmount + "");
                cdaReportResponse.setReportType("SubHead Wise CDA Report");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);

                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".docx";

                    docxGenaratorUtil.createCdaMainReportDoc(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".docx");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);
                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }


            } else if (cdaReportRequest.getReportType().equalsIgnoreCase("02")) {

                if (cdaReportRequest.getUnitId() == null || cdaReportRequest.getUnitId().isEmpty()) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "UNIT ID CAN NOT BE BLANK");
                }

                CgUnit cgUnit = cgUnitRepository.findByUnit(cdaReportRequest.getUnitId());
                if (cgUnit == null) {
                    throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID UNIT ID");
                }


                List<CDAReportResponse> cdaReportList = new ArrayList<>();
                CDAReportResponse cdaReportResponse = new CDAReportResponse();

                List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadAndSubHeadTypeIdOrderBySerialNumberAsc(cdaReportRequest.getMajorHead(), cdaReportRequest.getSubHeadType());


                List<CdaParking> cdaParkingTotalList = new ArrayList<CdaParking>();

                if (cdaReportRequest.getCdaType().contains("112233")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findAllByOrderByCdaNameAsc();
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else if (cdaReportRequest.getCdaType().contains("112244")) {
                    List<CdaParking> cdaParkingTotalList11 = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
                    cdaParkingTotalList.addAll(cdaParkingTotalList11);
                } else {
                    CdaParking singleCda = cdaParkingRepository.findByGinNo(cdaReportRequest.getCdaType());
                    cdaParkingTotalList.add(singleCda);
                }


                for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                    cdaReportList.add(cdaReportResponse);
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName("Total Amount");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put("Sub Head", cdaReportList);

                for (int i = 0; i < subHeadsData.size(); i++) {
                    cdaReportList = new ArrayList<>();
                    cdaReportResponse = new CDAReportResponse();

                    BudgetHead subHead = subHeadsData.get(i);
                    cdaReportResponse.setName(subHead.getSubHeadDescr());

                    Float totalAmount = 0f;
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndAndAllocTypeIdAndUnitId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", cdaReportRequest.getAllocationTypeId(), cgUnit.getUnit());
                            Float amount = 0f;

                            for (int m = 0; m < cdaData.size(); m++) {
                                if (cdaData.get(m).getTotalParkingAmount() == null) {
                                    amount = amount;
                                } else {
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                    grandTotal = grandTotal + amount;
                                }
                            }

                            CdaParking ginWiseData = cdaParkingRepository.findByGinNo(cdaParkingTotalList.get(k).getGinNo());

                            if (coloumWiseAmount.containsKey(ginWiseData.getCdaName())) {
                                Double colmount = Double.parseDouble(coloumWiseAmount.get(ginWiseData.getCdaName()));
                                coloumWiseAmount.put(ginWiseData.getCdaName(), (colmount + amount) + "");

                            } else {
                                coloumWiseAmount.put(ginWiseData.getCdaName(), amount + "");
                            }


                            totalAmount = totalAmount + amount;
                            cdaReportResponse = new CDAReportResponse();
                            cdaReportResponse.setName(amount + "");
                            cdaReportList.add(cdaReportResponse);
                        }
                    } else {
                        cdaReportResponse = new CDAReportResponse();
                        cdaReportResponse.setName("0");
                        cdaReportList.add(cdaReportResponse);
                    }

                    cdaReportResponse = new CDAReportResponse();
                    cdaReportResponse.setName(totalAmount + "");
                    cdaReportResponse.setReportType("Unit Wise CDA Report");
                    cdaReportList.add(cdaReportResponse);
                    allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
                }
                try {


                    File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                    String filePath = folder.getAbsolutePath() + "/" + fileName + ".docx";

                    docxGenaratorUtil.createCdaMainReportDoc(allCdaData, cadSubReport, filePath, grandTotal,coloumWiseAmount);
                    dtoList.setPath(HelperUtils.FILEPATH + fileName + ".docx");
                    dtoList.setFileName(fileName);
                    dtoList.setAllCdaData(allCdaData);
                } catch (Exception e) {
                    throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), e.toString());
                }
            }


        }


        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<FilePathResponse>() {
        });
    }


    @Override
    public ApiResponse<List<FilePathResponse>> getUnitWiseAllocationReport(UnitWiseAllocationReport reportRequest) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        CgUnit subUnit = cgUnitRepository.findByUnit(reportRequest.getUnitId());

        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(reportRequest.getFinYearId());

        if (reportRequest.getUnitId() == null || reportRequest.getUnitId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getFinYearId() == null || reportRequest.getFinYearId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getAmountTypeId() == null || reportRequest.getAmountTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getAllocationTypeId() == null || reportRequest.getAllocationTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(reportRequest.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();
        List<BudgetAllocation> budgetAllocationsDetalis1 = budgetAllocationRepository.findByToUnitAndFinYearAndAllocationTypeIdAndIsBudgetRevision(reportRequest.getUnitId(), reportRequest.getFinYearId(), reportRequest.getAllocationTypeId(), "0");
        List<BudgetAllocation> budgetAllocationsDetalis2 = budgetAllocationsDetalis1.stream().sorted(Comparator.comparing(data -> data.getSubHead().substring(data.getSubHead().length() - 2))).collect(Collectors.toList());
        List<BudgetAllocation> budgetAllocationsDetalis=budgetAllocationsDetalis2.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        String amtType = budgetAllocationsDetalis.get(0).getAmountType();
        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        try {
            Document document = new Document(PageSize.A4);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + "UnitWise_Allocation" + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();

            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk("UNIT WISE ALLOCATION REPORT", boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));

            PdfPTable table1 = new PdfPTable(2);
            table1.setWidthPercentage(100);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell1 = new PdfPCell(new Phrase("FINANCIAL YEAR : "+findyr.getFinYear(), cellFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("UNIT :"+subUnit.getDescr(), cellFont));

            table1.addCell(cell1);
            table1.addCell(cell2);
            document.add(table1);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            Font cellFont1 = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell01 = new PdfPCell(new Phrase("S.L", cellFont1));
            PdfPCell cell02 = new PdfPCell(new Phrase("REVENUE OBJECT HEAD", cellFont1));
            PdfPCell cell03 = new PdfPCell(new Phrase("ALLOCATION TYPE", cellFont1));
            PdfPCell cell04 = new PdfPCell(new Phrase("AMOUNT IN :("+amountIn+")", cellFont1));

            table.addCell(cell01);
            table.addCell(cell02);
            table.addCell(cell03);
            table.addCell(cell04);


            int i = 1;
            String finyear = "";
            String unit = "";
            Double amount;
            Double amountUnit;
            Double finAmount;
            Double sum = 0.0;
            for (BudgetAllocation row : budgetAllocationsDetalis) {
                finyear = findyr.getFinYear();
                amount = Double.valueOf(row.getAllocationAmount());
                AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                if (amountTypeObj == null) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                }
                amountUnit = amountTypeObj.getAmount();
                finAmount = amount * amountUnit / reqAmount;
                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(row.getSubHead());
                AllocationType type = allocationRepository.findByAllocTypeId(row.getAllocationTypeId());

                table.addCell(String.valueOf(i));
                table.addCell(bHead.getSubHeadDescr());
                table.addCell(type.getAllocDesc().toUpperCase());
                table.addCell(String.format("%1$0,1.4f", new BigDecimal(finAmount)));

                i++;
                sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
            }
            Font cellFont2 = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell20 = new PdfPCell(new Phrase("TOTAL", cellFont2));
            PdfPCell cell21 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sum)), cellFont2));
            table.addCell(cell20);
            table.addCell("");
            table.addCell("");
            table.addCell(cell21);

            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + "UnitWise_Allocation" + ".pdf");
            dto.setFileName("UnitWise_Allocation");
            dtoList.add(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }
    @Override
    public ApiResponse<List<FilePathResponse>> getUnitWiseAllocationReportDoc(UnitWiseAllocationReport reportRequest) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if (reportRequest.getUnitId() == null || reportRequest.getUnitId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getFinYearId() == null || reportRequest.getFinYearId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getAmountTypeId() == null || reportRequest.getAmountTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getAllocationTypeId() == null || reportRequest.getAllocationTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        CgUnit subUnit = cgUnitRepository.findByUnit(reportRequest.getUnitId());
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(reportRequest.getFinYearId());
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(reportRequest.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();
        List<BudgetAllocation> budgetAllocationsDetalis1 = budgetAllocationRepository.findByToUnitAndFinYearAndAllocationTypeIdAndIsBudgetRevision(reportRequest.getUnitId(), reportRequest.getFinYearId(), reportRequest.getAllocationTypeId(), "0");
        List<BudgetAllocation> budgetAllocationsDetalis2 = budgetAllocationsDetalis1.stream().sorted(Comparator.comparing(data -> data.getSubHead().substring(data.getSubHead().length() - 2))).collect(Collectors.toList());
        List<BudgetAllocation> budgetAllocationsDetalis=budgetAllocationsDetalis2.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        int size = budgetAllocationsDetalis.size();
        try {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("UNIT WISE ALLOCATION REPORT");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + "UnitWise_Allocation_Report" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));

            XWPFTable table = document.createTable(1, 2);
            table.setWidth("100%");
            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 12, "FINANCIAL YEAR : " + findyr.getFinYear(), true);
            XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 12, "UNIT : " + subUnit.getDescr(), true);

            XWPFParagraph spacingParagraph = document.createParagraph();
            spacingParagraph.setSpacingAfter(02);

            XWPFTable table1 = document.createTable(size + 1, 4);
            table1.setWidth("100%");
            XWPFTableRow tableRow = table1.getRow(0);
            XWPFParagraph paragraphtableRow0 = tableRow.getCell(0).addParagraph();
            boldText(paragraphtableRow0.createRun(), 12, "SERIAL NO", true);
            XWPFParagraph paragraphtableRow1 = tableRow.getCell(1).addParagraph();
            boldText(paragraphtableRow1.createRun(), 12, "SUB HEAD", true);
            XWPFParagraph paragraphtableRow2 = tableRow.getCell(2).addParagraph();
            boldText(paragraphtableRow2.createRun(), 12, "ALLOCATION TYPE", true);
            XWPFParagraph paragraphtableRow3 = tableRow.getCell(3).addParagraph();
            boldText(paragraphtableRow3.createRun(), 12, "ALLOCATION AMOUNT IN:  (" + amountIn + ")", true);

            int count = 1;
            Double amount;
            Double amountUnit;
            Double finAmount;
            Double sum = 0.0;
            for (Integer k = 0; k < budgetAllocationsDetalis.size(); k++) {
                amount = Double.valueOf(budgetAllocationsDetalis.get(k).getAllocationAmount());
                AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(budgetAllocationsDetalis.get(k).getAmountType());
                if (amountTypeObj == null) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                }
                amountUnit = amountTypeObj.getAmount();
                finAmount = amount * amountUnit / reqAmount;
                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(budgetAllocationsDetalis.get(k).getSubHead());
                AllocationType type = allocationRepository.findByAllocTypeId(budgetAllocationsDetalis.get(k).getAllocationTypeId());

                XWPFTableRow tableRows = table1.getRow(k + 1);
                XWPFParagraph paragraphtableRow01 = tableRows.getCell(0).addParagraph();
                boldText(paragraphtableRow01.createRun(), 10, Integer.toString(count), true);

                XWPFParagraph paragraphtableRow11 = tableRows.getCell(1).addParagraph();
                boldText(paragraphtableRow11.createRun(), 10, bHead.getSubHeadDescr(), true);

                XWPFParagraph paragraphtableRow21 = tableRows.getCell(2).addParagraph();
                boldText(paragraphtableRow21.createRun(), 10, type.getAllocDesc().toUpperCase(), true);

                XWPFParagraph paragraphtableRow31 = tableRows.getCell(3).addParagraph();
                boldText(paragraphtableRow31.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(finAmount)), true);
                count++;
                sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());

            }
            XWPFTable table222 = document.createTable(1, 4);
            table222.setWidth("100%");
            XWPFTableRow tableRowOne222 = table222.getRow(0);
            XWPFParagraph paragraphtableRowOne222 = tableRowOne222.getCell(0).addParagraph();
            boldText(paragraphtableRowOne222.createRun(), 12, "TOTAL", true);
            XWPFParagraph paragraphtableRowOne1222 = tableRowOne222.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1222.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne2222 = tableRowOne222.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2222.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne2233 = tableRowOne222.getCell(3).addParagraph();
            boldText(paragraphtableRowOne2233.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sum)), true);

            String names = approveName;
            String unitName = hrData.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + "UnitWise_Allocation_Report" + ".docx");
            dto.setFileName("UnitWise_Allocation_Report");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<BeReportResp>> getUnitWiseAllocationReportExcel(UnitWiseAllocationReport reportRequest) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<BeReportResp> dtoList = new ArrayList<BeReportResp>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if (reportRequest.getUnitId() == null || reportRequest.getUnitId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "UNIT ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getFinYearId() == null || reportRequest.getFinYearId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getAmountTypeId() == null || reportRequest.getAmountTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (reportRequest.getAllocationTypeId() == null || reportRequest.getAllocationTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "ALLOCATION TYPE ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        CgUnit subUnit = cgUnitRepository.findByUnit(reportRequest.getUnitId());
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(reportRequest.getFinYearId());
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(reportRequest.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();
        List<BudgetAllocation> budgetAllocationsDetalis1 = budgetAllocationRepository.findByToUnitAndFinYearAndAllocationTypeIdAndIsBudgetRevision(reportRequest.getUnitId(), reportRequest.getFinYearId(), reportRequest.getAllocationTypeId(), "0");
        List<BudgetAllocation> budgetAllocationsDetalis2 = budgetAllocationsDetalis1.stream().sorted(Comparator.comparing(data -> data.getSubHead().substring(data.getSubHead().length() - 2))).collect(Collectors.toList());
        List<BudgetAllocation> budgetAllocationsDetalis=budgetAllocationsDetalis2.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        int size = budgetAllocationsDetalis.size();
        try {

            int count = 1;
            Double amount;
            Double amountUnit;
            Double finAmount;
            Double sum = 0.0;
            for (Integer k = 0; k < budgetAllocationsDetalis.size(); k++) {
                amount = Double.valueOf(budgetAllocationsDetalis.get(k).getAllocationAmount());
                AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(budgetAllocationsDetalis.get(k).getAmountType());
                if (amountTypeObj == null) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
                    }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                }
                amountUnit = amountTypeObj.getAmount();
                finAmount = amount * amountUnit / reqAmount;
                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(budgetAllocationsDetalis.get(k).getSubHead());
                AllocationType type = allocationRepository.findByAllocTypeId(budgetAllocationsDetalis.get(k).getAllocationTypeId());

                BeReportResp resp = new BeReportResp();
                resp.setFinYear(findyr.getFinYear());
                resp.setAllocationType(type.getAllocDesc().toUpperCase());
                resp.setAmountIn(amountIn);
                resp.setBudgetHead(bHead.getSubHeadDescr());
                resp.setUnitName(subUnit.getDescr());
                resp.setAllocationAmount(String.format("%1$0,1.4f", new BigDecimal(finAmount)));

                count++;
                sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                dtoList.add(resp);

            }
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<BeReportResp>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getSubHeadWiseAllocationReport(SubHeadWiseAllocationReportReq req) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (req.getSubHeadId() == null || req.getSubHeadId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "SUBHEAD ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getFinYearId() == null || req.getFinYearId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getAmountTypeId() == null || req.getAmountTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getAllocationTypeId() == null || req.getAllocationTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(req.getFinYearId());
        List<BudgetAllocation> budgetAllocationsDetalis1 = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(req.getSubHeadId(), req.getFinYearId(), req.getAllocationTypeId(), "0");
        List<BudgetAllocation> budgetAllocationsDetalis=budgetAllocationsDetalis1.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

        BudgetHead bHead = subHeadRepository.findByBudgetCodeId(req.getSubHeadId());
        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(req.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();

        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        try {
            Document document = new Document(PageSize.A4);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + "subhead-wise-allocation-report" + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();
            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk("SUBHEAD WISE ALLOCATION REPORT", boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));

            PdfPTable table1 = new PdfPTable(2);
            table1.setWidthPercentage(100);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell1 = new PdfPCell(new Phrase("FINANCIAL YEAR : "+findyr.getFinYear(), cellFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("SUBHEAD :"+bHead.getSubHeadDescr(), cellFont));

            table1.addCell(cell1);
            table1.addCell(cell2);
            document.add(table1);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            PdfPCell cell10 = new PdfPCell(new Phrase("S.L", cellFont));
            PdfPCell cell20 = new PdfPCell(new Phrase("UNIT", cellFont));
            PdfPCell cell30 = new PdfPCell(new Phrase("ALLOCATION TYPE", cellFont));
            PdfPCell cell40 = new PdfPCell(new Phrase("AMOUNT IN :("+amountIn+")", cellFont));

            table.addCell(cell10);
            table.addCell(cell20);
            table.addCell(cell30);
            table.addCell(cell40);

            int i = 1;
            String finyear = "";
            String unit = "";
            Double amount = 0.0;
            Double amountUnit;
            Double finAmount;
            float sum = 0;
            for (BudgetAllocation row : budgetAllocationsDetalis) {
                for (Integer k = 0; k < units.size(); k++) {
                    if (units.get(k).getUnit().equalsIgnoreCase(row.getToUnit())) {

                        AllocationType type = allocationRepository.findByAllocTypeId(row.getAllocationTypeId());
                        CgUnit unitN = cgUnitRepository.findByUnit(row.getToUnit());
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                        if (amountTypeObj == null) {
                            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                            }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                        }
                        amountUnit = amountTypeObj.getAmount();
                        if (row.getToUnit().equalsIgnoreCase(hrData.getUnitId())) {
                            continue;
                        } else
                            amount = Double.valueOf(row.getAllocationAmount());

                        finAmount = amount * amountUnit / reqAmount;

                        table.addCell(String.valueOf(i));
                        table.addCell(unitN.getDescr());
                        table.addCell(type.getAllocDesc().toUpperCase());
                        table.addCell(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                        i++;
                        sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                    }
                }
            }
            PdfPCell cell110 = new PdfPCell(new Phrase("TOTAL", cellFont));
            PdfPCell cell200 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sum)), cellFont));

            table.addCell(cell110);
            table.addCell("");
            table.addCell("");
            table.addCell(cell200);

            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + "subhead-wise-allocation-report" + ".pdf");
            dto.setFileName("subhead-wise-allocation-report");
            dtoList.add(dto);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }
    @Override
    public ApiResponse<List<FilePathResponse>> getSubHeadWiseAllocationReportDoc(SubHeadWiseAllocationReportReq req) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (req.getSubHeadId() == null || req.getSubHeadId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "SUBHEAD ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getFinYearId() == null || req.getFinYearId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getAmountTypeId() == null || req.getAmountTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getAllocationTypeId() == null || req.getAllocationTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(req.getFinYearId());
        BudgetHead bHead = subHeadRepository.findByBudgetCodeId(req.getSubHeadId());
        List<BudgetAllocation> budgetAllocationsDetaliss = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(req.getSubHeadId(), req.getFinYearId(), req.getAllocationTypeId(), "0");
        List<BudgetAllocation> budgetAllocationsDetalis1 = budgetAllocationsDetaliss.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
        List<BudgetAllocation> budgetAllocationsDetalis=budgetAllocationsDetalis1.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

        int size = budgetAllocationsDetalis.size();
        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(req.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("SUBHEAD WISE ALLOCATION REPORT");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + "SubHeadWise_Allocation_Report" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));

            XWPFTable table = document.createTable(1, 2);
            table.setWidth("100%");
            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 12, "FINANCIAL YEAR : " + findyr.getFinYear(), true);
            XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 12, "SUBHEAD : " + bHead.getSubHeadDescr(), true);

            XWPFParagraph spacingParagraph = document.createParagraph();
            spacingParagraph.setSpacingAfter(0);

            XWPFTable table1 = document.createTable(size + 1, 4);
            table1.setWidth("100%");
            XWPFTableRow tableRow = table1.getRow(0);
            XWPFParagraph paragraphtableRow0 = tableRow.getCell(0).addParagraph();
            boldText(paragraphtableRow0.createRun(), 12, "SERIAL NO", true);
            XWPFParagraph paragraphtableRow1 = tableRow.getCell(1).addParagraph();
            boldText(paragraphtableRow1.createRun(), 12, "UNIT", true);
            XWPFParagraph paragraphtableRow2 = tableRow.getCell(2).addParagraph();
            boldText(paragraphtableRow2.createRun(), 12, "ALLOCATION TYPE", true);
            XWPFParagraph paragraphtableRow3 = tableRow.getCell(3).addParagraph();
            boldText(paragraphtableRow3.createRun(), 12, "ALLOCATION AMOUNT IN:  (" + amountIn + ")", true);


            int i = 1;
            Double amount = 0.0;
            Double amountUnit;
            Double finAmount;
            float sum = 0;
            for (Integer r = 0; r < budgetAllocationsDetalis.size(); r++) {
                for (Integer k = 0; k < units.size(); k++) {
                    if (units.get(k).getUnit().equalsIgnoreCase(budgetAllocationsDetalis.get(r).getToUnit())) {

                        AllocationType type = allocationRepository.findByAllocTypeId(budgetAllocationsDetalis.get(r).getAllocationTypeId());
                        CgUnit unitN = cgUnitRepository.findByUnit(budgetAllocationsDetalis.get(r).getToUnit());
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(budgetAllocationsDetalis.get(r).getAmountType());
                        if (amountTypeObj == null) {
                            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                            }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                        }
                        amountUnit = amountTypeObj.getAmount();
                        amount = Double.valueOf(budgetAllocationsDetalis.get(r).getAllocationAmount());
                        finAmount = amount * amountUnit / reqAmount;

                        XWPFTableRow tableRows = table1.getRow(r + 1);
                        XWPFParagraph paragraphtableRow01 = tableRows.getCell(0).addParagraph();
                        boldText(paragraphtableRow01.createRun(), 10, Integer.toString(i), true);

                        XWPFParagraph paragraphtableRow11 = tableRows.getCell(1).addParagraph();
                        boldText(paragraphtableRow11.createRun(), 10, unitN.getDescr(), true);

                        XWPFParagraph paragraphtableRow21 = tableRows.getCell(2).addParagraph();
                        boldText(paragraphtableRow21.createRun(), 10, type.getAllocDesc().toUpperCase(), true);

                        XWPFParagraph paragraphtableRow31 = tableRows.getCell(3).addParagraph();
                        boldText(paragraphtableRow31.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(finAmount)), true);
                        i++;
                        sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                    }
                }
            }
            XWPFTable table222 = document.createTable(1, 4);
            table222.setWidth("100%");
            XWPFTableRow tableRowOne222 = table222.getRow(0);
            XWPFParagraph paragraphtableRowOne222 = tableRowOne222.getCell(0).addParagraph();
            boldText(paragraphtableRowOne222.createRun(), 12, "TOTAL", true);
            XWPFParagraph paragraphtableRowOne1222 = tableRowOne222.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1222.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne2222 = tableRowOne222.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2222.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne2233 = tableRowOne222.getCell(3).addParagraph();
            boldText(paragraphtableRowOne2233.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sum)), true);

            String names = approveName;
            String unitName = hrData.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + "SubHeadWise_Allocation_Report" + ".docx");
            dto.setFileName("SubHeadWise_Allocation_Report");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<BeReportResp>> getSubHeadWiseAllocationReportExcel(SubHeadWiseAllocationReportReq req) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<BeReportResp> dtoList = new ArrayList<BeReportResp>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (req.getSubHeadId() == null || req.getSubHeadId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "SUBHEAD ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getFinYearId() == null || req.getFinYearId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getAmountTypeId() == null || req.getAmountTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (req.getAllocationTypeId() == null || req.getAllocationTypeId().isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "ALLOCATION TYPE ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }

        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(req.getFinYearId());
        BudgetHead bHead = subHeadRepository.findByBudgetCodeId(req.getSubHeadId());
        List<BudgetAllocation> budgetAllocationsDetaliss = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(req.getSubHeadId(), req.getFinYearId(), req.getAllocationTypeId(), "0");
        List<BudgetAllocation> budgetAllocationsDetalis1 = budgetAllocationsDetaliss.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
        List<BudgetAllocation> budgetAllocationsDetalis=budgetAllocationsDetalis1.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(req.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {

            int i = 1;
            Double amount = 0.0;
            Double amountUnit;
            Double finAmount;
            float sum = 0;
            for (Integer r = 0; r < budgetAllocationsDetalis.size(); r++) {
                for (Integer k = 0; k < units.size(); k++) {
                    if (units.get(k).getUnit().equalsIgnoreCase(budgetAllocationsDetalis.get(r).getToUnit())) {

                        AllocationType type = allocationRepository.findByAllocTypeId(budgetAllocationsDetalis.get(r).getAllocationTypeId());
                        CgUnit unitN = cgUnitRepository.findByUnit(budgetAllocationsDetalis.get(r).getToUnit());
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(budgetAllocationsDetalis.get(r).getAmountType());
                        if (amountTypeObj == null) {
                            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
                            }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                        }
                        amountUnit = amountTypeObj.getAmount();
                        amount = Double.valueOf(budgetAllocationsDetalis.get(r).getAllocationAmount());
                        finAmount = amount * amountUnit / reqAmount;

                        BeReportResp resp = new BeReportResp();
                        resp.setFinYear(findyr.getFinYear());
                        resp.setAllocationType(type.getAllocDesc().toUpperCase());
                        resp.setAmountIn(amountIn);
                        resp.setBudgetHead(bHead.getSubHeadDescr());
                        resp.setUnitName(unitN.getDescr());
                        resp.setAllocationAmount(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                        dtoList.add(resp);

                        i++;
                        sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                    }
                }
            }
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<BeReportResp>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getBEAllocationReport(String finYearId, String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);
        AllocationType type = allocationRepository.findByAllocTypeId(allocationType);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();

        String amtType = "";
        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        try {
            Document document = new Document(PageSize.A4);
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + type.getAllocDesc().toUpperCase() + "_AllocationReport" + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();
            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk(type.getAllocDesc().toUpperCase()+" "+ "ALLOCATION REPORT", boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));


            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell1 = new PdfPCell(new Phrase("REVENUE OBJECT HEAD ", cellFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("UNIT", cellFont));
            PdfPCell cell3 = new PdfPCell(new Phrase(type.getAllocDesc().toUpperCase()+" "+findyr.getFinYear()+" "+"ALLOCATION IN: ("+amountIn+")", cellFont));

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);

            int i = 1;
            String finyear = "";
            String unit = "";
            float gdTotal = 0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetailss = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(subHeadId, finYearId, allocationType, "0");
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                int count = 0;
                float sum = 0;
                Double amount;
                Double amountUnit;
                Double finAmount = Double.valueOf(0);

                for (BudgetAllocation row : reportDetails) {

                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getToUnit())) {

                            if (row.getToUnit().equalsIgnoreCase(hrData.getUnitId())) {
                                continue;
                            } else
                                amount = Double.valueOf(row.getAllocationAmount());

                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(row.getSubHead());
                            CgUnit unitN = cgUnitRepository.findByUnit(row.getToUnit());

                            if (count == 0)
                                table.addCell(bHead.getSubHeadDescr());
                            else
                                table.addCell("");
                            table.addCell(unitN.getDescr());
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            gdTotal += sum;
                        }

                    }

                }
                if (count != 0) {
                    PdfPCell cell10 = new PdfPCell(new Phrase("TOTAL ", cellFont));
                    PdfPCell cell11 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sum)), cellFont));
                    table.addCell("");
                    table.addCell(cell10);
                    table.addCell(cell11);
                    count = 0;
                }

            }
            PdfPCell cell101 = new PdfPCell(new Phrase("GRAND TOTAL ", cellFont));
            PdfPCell cell111 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(gdTotal)), cellFont));
            table.addCell("");
            table.addCell(cell101);
            table.addCell(cell111);

            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + type.getAllocDesc().toUpperCase() + "_AllocationReport.pdf");
            dto.setFileName(type.getAllocDesc().toUpperCase() + "_AllocationReport.pdf");
            dtoList.add(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }
    @Override
    public ApiResponse<List<FilePathResponse>> getBEAllocationReportDoc(String finYearId, String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());

        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);
        AllocationType type = allocationRepository.findByAllocTypeId(allocationType);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText(type.getAllocDesc().toUpperCase() + "_ALLOCATION REPORT");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + type.getAllocDesc().toUpperCase() + "_Allocation_Report" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));
            XWPFTable table = document.createTable(1, 3);
            table.setWidth("100%");
            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 12, "REVENUE OBJECT HEAD ", true);
            XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 12, "UNIT ", true);
            XWPFParagraph paragraphtableRowOne2 = tableRowOne.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2.createRun(), 12, type.getAllocDesc().toUpperCase() + " " + findyr.getFinYear() + " " + "ALLOCATION IN: (" + amountIn + ")", true);

            float gdTotal = 0;
            int i = 1;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(subHeadId, finYearId, allocationType, "0");
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());
                int sz = reportDetails.size();
                if(sz<=0)
                    continue;
                    XWPFTable table11 = document.createTable(sz, 3);
                    table11.setWidth("100%");

                int count = 0;
                float sum = 0;
                Double amount;
                Double amountUnit;
                Double finAmount = Double.valueOf(0);
                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {

                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(reportDetails.get(r).getSubHead());
                            CgUnit unitN = cgUnitRepository.findByUnit(reportDetails.get(r).getToUnit());

                            XWPFTableRow tableRowOne111 = table11.getRow(r);
                            XWPFParagraph paragraphtableRowOne11 = tableRowOne111.getCell(0).addParagraph();

                            if (r == 0) {
                                boldText(paragraphtableRowOne11.createRun(), 10, bHead.getSubHeadDescr(), true);
                            } else {
                                boldText(paragraphtableRowOne11.createRun(), 10, "", true);
                            }
                            XWPFParagraph paragraphtableRow11 = tableRowOne111.getCell(1).addParagraph();
                            boldText(paragraphtableRow11.createRun(), 10, unitN.getDescr(), true);

                            XWPFParagraph paragraphtableRow21 = tableRowOne111.getCell(2).addParagraph();
                            boldText(paragraphtableRow21.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(finAmount)), true);
                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            gdTotal += sum;

                        }

                    }

                }
                if (count != 0) {
                    XWPFTable table222 = document.createTable(1, 3);
                    table222.setWidth("100%");
                    XWPFTableRow tableRowOne222 = table222.getRow(0);
                    XWPFParagraph paragraphtableRowOne222 = tableRowOne222.getCell(0).addParagraph();
                    boldText(paragraphtableRowOne222.createRun(), 12, "", true);
                    XWPFParagraph paragraphtableRowOne1222 = tableRowOne222.getCell(1).addParagraph();
                    boldText(paragraphtableRowOne1222.createRun(), 12, "TOTAL ", true);
                    XWPFParagraph paragraphtableRowOne2222 = tableRowOne222.getCell(2).addParagraph();
                    boldText(paragraphtableRowOne2222.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sum)), true);
                    count =0;
                }

            }
            XWPFTable table223 = document.createTable(1, 3);
            table223.setWidth("100%");
            XWPFTableRow tableRowOne223 = table223.getRow(0);
            XWPFParagraph paragraphtableRowOne223 = tableRowOne223.getCell(0).addParagraph();
            boldText(paragraphtableRowOne223.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne1223 = tableRowOne223.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1223.createRun(), 12, "GRAND TOTAL ", true);
            XWPFParagraph paragraphtableRowOne2223 = tableRowOne223.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2223.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(gdTotal)), true);

            String names = approveName;
            String unitName = hrData.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + type.getAllocDesc().toUpperCase() + "_Allocation_Report" + ".docx");
            dto.setFileName(type.getAllocDesc().toUpperCase() + "_Allocation_Report");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<BeReportResp>> getBEAllocationReportExcel(String finYearId, String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<BeReportResp> dtoList = new ArrayList<BeReportResp>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());

        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);
        AllocationType type = allocationRepository.findByAllocTypeId(allocationType);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {

            int i = 1;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(subHeadId, finYearId, allocationType, "0");
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                int count = 0;
                float sum = 0;
                Double amount;
                Double amountUnit;
                Double finAmount = Double.valueOf(0);
                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {

                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BeReportResp>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(reportDetails.get(r).getSubHead());
                            CgUnit unitN = cgUnitRepository.findByUnit(reportDetails.get(r).getToUnit());

                            BeReportResp resp = new BeReportResp();
                            resp.setFinYear(findyr.getFinYear());
                            resp.setAmountIn(amountIn);
                            resp.setAllocationType(type.getAllocDesc().toUpperCase());
                            if (r == 0) {
                                resp.setBudgetHead(bHead.getSubHeadDescr());
                            } else {
                                resp.setBudgetHead("");
                            }
                            resp.setUnitName(unitN.getDescr());
                            resp.setAllocationAmount(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());

                            dtoList.add(resp);
                        }
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<BeReportResp>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getREAllocationReport(String finYearId, String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();
        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();

        AllocationType allockData = allocationRepository.findByAllocTypeId(allocationType);
        String allocType = allockData.getAllocType();
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);


        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        String amtType = "";
        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        try {
            Document document = new Document(PageSize.A4);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + allocType.toUpperCase() + "_Revised_Allocation-Report.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();
            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk(allocType.toUpperCase()+" "+ "REVISED  ALLOCATION  REPORT", boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));

            PdfPTable tables = new PdfPTable(2);
            tables.setWidthPercentage(100);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cells = new PdfPCell(new Phrase(allocType.toUpperCase()+": "+findyr.getFinYear()+" "+"ALLOCATION", cellFont));
            PdfPCell cells0 = new PdfPCell(new Phrase("AMOUNT IN: ("+amountIn.toUpperCase()+")", cellFont));
            tables.addCell(cells);
            tables.addCell(cells0);
            document.add(tables);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            PdfPCell cell1 = new PdfPCell(new Phrase("REVENUE OBJECT HEAD ", cellFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("UNIT", cellFont));
            PdfPCell cell3 = new PdfPCell(new Phrase("ALLOCATION AMOUNT", cellFont));
            PdfPCell cell4 = new PdfPCell(new Phrase("ADDITIONAL AMOUNT", cellFont));
            PdfPCell cell5 = new PdfPCell(new Phrase("REVISED AMOUNT", cellFont));

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);
            table.addCell(cell5);

            int i = 1;
            String finyear = "";
            String unit = "";
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            float grTotalSum = 0;
            Double amount = Double.valueOf(0);
            Double amountUnit;
            Double finAmount;
            Double revisedAmount;
            Double reAmount;
            Double s2 = 0.0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetailss = budgetAllocationRepository.findBySubHeadAndAllocationTypeIdAndIsFlagAndIsBudgetRevision(subHeadId, allocationType, "0", "0");
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);

                int count = 0;
                float sumExisting = 0;
                float sumRE = 0;
                float total = 0;
                for (BudgetAllocation row : reportDetails) {

                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getToUnit())) {
                            if (row.getToUnit().equalsIgnoreCase(hrData.getUnitId())) {
                                continue;
                            }
                            amount = Double.valueOf(row.getAllocationAmount());
                            if (row.getRevisedAmount() != null) {
                                revisedAmount = Double.valueOf(row.getRevisedAmount());
                            } else
                                revisedAmount = 0.0;

                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            reAmount = revisedAmount * amountUnit / reqAmount;
                            String s = reAmount.toString();
                            if (s.contains("-")) {
                                String s1 = s.replace("-", "");
                                s2 = Double.parseDouble(s1);
                            }
                            CgUnit unitN = cgUnitRepository.findByUnit(row.getToUnit());

                            if (count == 0)
                                table.addCell(bHead.getSubHeadDescr());
                            else
                                table.addCell("");
                            table.addCell(unitN.getDescr());
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            if (reAmount < 0)
                                table.addCell("(-) "+String.format("%1$0,1.4f", new BigDecimal(s2)));
                            else if (reAmount > 0)
                                table.addCell("(+) "+String.format("%1$0,1.4f", new BigDecimal(reAmount)));
                            else
                                table.addCell(String.format("%1$0,1.4f", new BigDecimal(reAmount)));
                            table.addCell(String.format("%1$0,1.4f", (new BigDecimal((Float.parseFloat(Double.toString(finAmount)) + Float.parseFloat(Double.toString(reAmount)))))));
                            count++;
                            sumExisting += Float.parseFloat(new BigDecimal(Double.toString(finAmount)).toPlainString());
                            sumRE += Float.parseFloat(new BigDecimal(Double.toString(reAmount)).toPlainString());
                            grTotalAlloc += sumExisting;
                            grTotalAddition += sumRE;
                            grTotalSum += (sumExisting + sumRE);

                        }
                    }
                }
                if (count != 0) {
                    total = sumExisting + sumRE;
                    Double ss2 = 0.0;
                    String ss = Float.toString(sumRE);
                    if (ss.contains("-")) {
                        String ss1 = ss.replace("-", "");
                        ss2 = Double.parseDouble(ss1);
                    }
                    PdfPCell cell10 = new PdfPCell(new Phrase("TOTAL", cellFont));
                    PdfPCell cell20 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sumExisting)), cellFont));
                    PdfPCell cell301 = new PdfPCell(new Phrase("(-) "+String.format("%1$0,1.4f", new BigDecimal(ss2)), cellFont));
                    PdfPCell cell302 = new PdfPCell(new Phrase("(+) "+String.format("%1$0,1.4f", new BigDecimal(sumRE)), cellFont));
                    PdfPCell cell303 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sumRE)), cellFont));
                    PdfPCell cell40 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(total)), cellFont));

                    table.addCell(" ");
                    table.addCell(cell10);
                    table.addCell(cell20);
                    if (sumRE < 0)
                        table.addCell(cell301);
                    else if (sumRE > 0)
                        table.addCell(cell302);
                    else
                        table.addCell(cell303);
                    table.addCell(cell40);
                    count = 0;
                }

            }
            PdfPCell cell00 = new PdfPCell(new Phrase("GRAND TOTAL", cellFont));
            PdfPCell cell01 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), cellFont));
            PdfPCell cell02 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), cellFont));
            PdfPCell cell03 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalSum)), cellFont));

            table.addCell(" ");
            table.addCell(cell00);
            table.addCell(cell01);
            table.addCell(cell02);
            table.addCell(cell03);

            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + allocType.toUpperCase() + "_Revised_allocation-report.pdf");
            dto.setFileName(allocType.toUpperCase() + "_Revised_allocation-report.pdf");
            dtoList.add(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }
    @Override
    public ApiResponse<List<FilePathResponse>> getREAllocationReportDoc(String finYearId, String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();
        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }
        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        AllocationType allockData = allocationRepository.findByAllocTypeId(allocationType);
        String allocType = allockData.getAllocType();
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);


        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }

        try {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText(allocType.toUpperCase() + "_REVISED_ALLOCATION REPORT");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + allocType.toUpperCase() + "_Revised_Allocation_Report" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));

            XWPFTable tab = document.createTable(1, 2);
            tab.setWidth("100%");
            XWPFTableRow tab1 = tab.getRow(0);
            XWPFParagraph paragraph11 = tab1.getCell(0).addParagraph();
            boldText(paragraph11.createRun(), 10, allocType.toUpperCase() + " :" + findyr.getFinYear() + " :" + "ALLOCATION", true);
            XWPFParagraph paragraph22 = tab1.getCell(1).addParagraph();
            boldText(paragraph22.createRun(), 10, "AMOUNT IN :( " + amountIn.toUpperCase() + " )", true);
            XWPFParagraph zz = document.createParagraph();
            zz.setSpacingAfter(1);

            XWPFTable table = document.createTable(1, 5);
            table.setWidth("100%");
            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 12, "REVENUE OBJECT HEAD ", true);
            XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 12, "UNIT ", true);
            XWPFParagraph paragraphtableRowOne2 = tableRowOne.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2.createRun(), 12, "ALLOCATION", true);
            XWPFParagraph paragraphtableRowOne3 = tableRowOne.getCell(3).addParagraph();
            boldText(paragraphtableRowOne3.createRun(), 12, "ADDITIONAL", true);
            XWPFParagraph paragraphtableRowOne4 = tableRowOne.getCell(4).addParagraph();
            boldText(paragraphtableRowOne4.createRun(), 12, "REVISED", true);


            int i = 1;
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            float grTotalSum = 0;
            Double amount = Double.valueOf(0);
            Double amountUnit;
            Double finAmount;
            Double revisedAmount;
            Double reAmount;
            Double s2 = 0.0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndAllocationTypeIdAndIsFlagAndIsBudgetRevision(subHeadId, allocationType, "0", "0");
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);

                int sz = reportDetails.size();
                if(sz<=0)
                    continue;
                XWPFTable table11 = document.createTable(sz, 5);
                table11.setWidth("100%");

                int count = 0;
                float sumExisting = 0;
                float sumRE = 0;
                float total = 0;
                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            if (reportDetails.get(r).getRevisedAmount() != null) {
                                revisedAmount = Double.valueOf(reportDetails.get(r).getRevisedAmount());
                            } else
                                revisedAmount = 0.0;

                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            reAmount = revisedAmount * amountUnit / reqAmount;
                            Double add = finAmount + reAmount;
                            String s = reAmount.toString();
                            if (s.contains("-")) {
                                String s1 = s.replace("-", "");
                                s2 = Double.parseDouble(s1);
                            }
                            CgUnit unitN = cgUnitRepository.findByUnit(reportDetails.get(r).getToUnit());

                            XWPFTableRow tableRowOne111 = table11.getRow(r);
                            XWPFParagraph paragraphtableRowOne11 = tableRowOne111.getCell(0).addParagraph();

                            if (r == 0) {
                                boldText(paragraphtableRowOne11.createRun(), 10, bHead.getSubHeadDescr(), true);
                            } else {
                                boldText(paragraphtableRowOne11.createRun(), 10, "", true);
                            }
                            XWPFParagraph paragraphtableRow11 = tableRowOne111.getCell(1).addParagraph();
                            boldText(paragraphtableRow11.createRun(), 10, unitN.getDescr(), true);

                            XWPFParagraph paragraphtableRow21 = tableRowOne111.getCell(2).addParagraph();
                            boldText(paragraphtableRow21.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(finAmount)), true);

                            XWPFParagraph paragraphtableRow31 = tableRowOne111.getCell(3).addParagraph();
                            if (reAmount < 0)
                                boldText(paragraphtableRow31.createRun(), 10, "(-)" + String.format("%1$0,1.4f", new BigDecimal(s2)), true);
                            else if (reAmount > 0)
                                boldText(paragraphtableRow31.createRun(), 10, "(+)" + String.format("%1$0,1.4f", new BigDecimal(reAmount)), true);
                            else
                                boldText(paragraphtableRow31.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(reAmount)), true);

                            XWPFParagraph paragraphtableRow41 = tableRowOne111.getCell(4).addParagraph();
                            boldText(paragraphtableRow41.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(add)), true);

                            sumExisting += Float.parseFloat(new BigDecimal(Double.toString(finAmount)).toPlainString());
                            sumRE += Float.parseFloat(new BigDecimal(Double.toString(reAmount)).toPlainString());

                            grTotalAlloc += sumExisting;
                            grTotalAddition += sumRE;
                            grTotalSum += (sumExisting + sumRE);

                        }
                    }
                }
                total = sumExisting + sumRE;
                Double ss2 = 0.0;
                String ss = Float.toString(sumRE);
                if (ss.contains("-")) {
                    String ss1 = ss.replace("-", "");
                    ss2 = Double.parseDouble(ss1);
                }
                XWPFTable table222 = document.createTable(1, 5);
                table222.setWidth("100%");
                XWPFTableRow tableRowOne222 = table222.getRow(0);
                XWPFParagraph paragraphtableRowOne222 = tableRowOne222.getCell(0).addParagraph();
                boldText(paragraphtableRowOne222.createRun(), 12, "", true);
                XWPFParagraph paragraphtableRowOne1222 = tableRowOne222.getCell(1).addParagraph();
                boldText(paragraphtableRowOne1222.createRun(), 12, "TOTAL ", true);
                XWPFParagraph paragraphtableRowOne2222 = tableRowOne222.getCell(2).addParagraph();
                boldText(paragraphtableRowOne2222.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sumExisting)), true);
                XWPFParagraph paragraphtableRowOne2233 = tableRowOne222.getCell(3).addParagraph();
                if (sumRE < 0)
                    boldText(paragraphtableRowOne2233.createRun(), 12, "(-)" + String.format("%1$0,1.4f", new BigDecimal(ss2)), true);
                else if (sumRE > 0)
                    boldText(paragraphtableRowOne2233.createRun(), 12, "(+)" + String.format("%1$0,1.4f", new BigDecimal(sumRE)), true);
                else
                    boldText(paragraphtableRowOne2233.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sumRE)), true);
                XWPFParagraph paragraphtableRowOne2244 = tableRowOne222.getCell(4).addParagraph();
                boldText(paragraphtableRowOne2244.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(total)), true);
            }
            XWPFTable table223 = document.createTable(1, 5);
            table223.setWidth("100%");
            XWPFTableRow tableRowOne223 = table223.getRow(0);
            XWPFParagraph paragraphtableRowOne223 = tableRowOne223.getCell(0).addParagraph();
            boldText(paragraphtableRowOne223.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne1223 = tableRowOne223.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1223.createRun(), 12, "GRAND TOTAL ", true);
            XWPFParagraph paragraphtableRowOne2223 = tableRowOne223.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2223.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), true);
            XWPFParagraph paragraphtableRowOne2234 = tableRowOne223.getCell(3).addParagraph();
            boldText(paragraphtableRowOne2234.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), true);
            XWPFParagraph paragraphtableRowOne2245 = tableRowOne223.getCell(4).addParagraph();
            boldText(paragraphtableRowOne2245.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalSum)), true);

            String names = approveName;
            String unitName = hrData.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + allocType.toUpperCase() + "_Revised_Allocation_Report" + ".docx");
            dto.setFileName(allocType.toUpperCase() + "_Revised_Allocation_Report");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<RivisionReportResp>> getREAllocationReportExcel(String finYearId, String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<RivisionReportResp> dtoList = new ArrayList<RivisionReportResp>();
        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        AllocationType allockData = allocationRepository.findByAllocTypeId(allocationType);
        String allocType = allockData.getAllocType().toUpperCase();
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);


        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }

        try {

            int i = 1;
            Double amount = Double.valueOf(0);
            Double amountUnit;
            Double finAmount;
            Double revisedAmount;
            Double reAmount;
            Double s2 = 0.0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndAllocationTypeIdAndIsFlagAndIsBudgetRevision(subHeadId, allocationType, "0", "0");
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);

                int count = 0;
                float sumExisting = 0;
                float sumRE = 0;
                float total = 0;
                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            if (reportDetails.get(r).getRevisedAmount() != null) {
                                revisedAmount = Double.valueOf(reportDetails.get(r).getRevisedAmount());
                            } else
                                revisedAmount = 0.0;

                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            reAmount = revisedAmount * amountUnit / reqAmount;
                            Double add = finAmount + reAmount;
                            String s = reAmount.toString();
                            if (s.contains("-")) {
                                String s1 = s.replace("-", "");
                                s2 = Double.parseDouble(s1);
                            }
                            CgUnit unitN = cgUnitRepository.findByUnit(reportDetails.get(r).getToUnit());

                            RivisionReportResp res = new RivisionReportResp();
                            res.setFinYear(findyr.getFinYear());
                            res.setAmountIn(amountIn);
                            res.setAllocationType(allocType);
                            if (r == 0) {
                                res.setBudgetHead(bHead.getSubHeadDescr());
                            } else {
                                res.setBudgetHead("");
                            }
                            res.setUnitName(unitN.getDescr());
                            res.setAllocationAmount(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            if (reAmount < 0)
                                res.setAdditionalAmount("(-)" + String.format("%1$0,1.4f", new BigDecimal(s2)));
                            else if (reAmount > 0)
                                res.setAdditionalAmount("(+)" + String.format("%1$0,1.4f", new BigDecimal(reAmount)));
                            else
                                res.setAdditionalAmount(String.format("%1$0,1.4f", new BigDecimal(reAmount)));

                            res.setTotalAmount(String.format("%1$0,1.4f", new BigDecimal(add)));

                            dtoList.add(res);

                            sumExisting += Float.parseFloat(new BigDecimal(Double.toString(finAmount)).toPlainString());
                            sumRE += Float.parseFloat(new BigDecimal(Double.toString(reAmount)).toPlainString());

                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<RivisionReportResp>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getBEREAllocationReport(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeBE == null || allocationTypeBE.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE BE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeRE == null || allocationTypeRE.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE RE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeBE.equalsIgnoreCase(allocationTypeRE)) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "BE or RE ALLOCATION CAN NOT BE SAME", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AllocationType type = allocationRepository.findByAllocTypeId(allocationTypeBE);
        AllocationType types = allocationRepository.findByAllocTypeId(allocationTypeRE);

        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationTypeBE);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();

        String amtType = "";
        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);


        try {
            Document document = new Document(PageSize.A4);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + type.getAllocDesc().toUpperCase() + "And" + types.getAllocDesc().toUpperCase() + "_Allocation-Report.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();
            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk("RE  ALLOCATION  REPORT"+": "+findyr.getFinYear(), boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell1 = new PdfPCell(new Phrase("REVENUE OBJECT HEAD ", cellFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("UNIT", cellFont));
            PdfPCell cell3 = new PdfPCell(new Phrase(type.getAllocDesc().toUpperCase()+" "+"ALLOCATION AMOUNT IN: ("+amountIn+")", cellFont));
            PdfPCell cell4 = new PdfPCell(new Phrase(type.getAllocDesc().toUpperCase()+" "+"ALLOCATION AMOUNT IN: ("+amountIn+")", cellFont));

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);


            int i = 1;
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            String finyear = "";
            String unit = "";
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetails = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(subHeadId, finYearId, allocationTypeBE, "0");
                int count = 0;
                float sum = 0;
                Double amount = Double.valueOf(0);
                Double amountUnit;
                Double finAmount;
                float reSum = 0;
                Double reAmountUnit = 0.0;
                Double reFinalAmount;
                Double reTotalAmount = 0.0;

                for (BudgetAllocation row : reportDetails) {

                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getToUnit())) {

                            if (row.getToUnit().equalsIgnoreCase(hrData.getUnitId())) {
                                continue;
                            } else
                                amount = Double.valueOf(row.getAllocationAmount());
                            String unitIds = row.getToUnit();
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<BudgetAllocation> reData = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndIsBudgetRevision(unitIds, finYearId, subHeadId, allocationTypeRE, "0");
                            if (reData.size() <= 0) {
                                reFinalAmount = 0.0000;
                            } else {
                                if (row.getToUnit().equalsIgnoreCase(hrData.getUnitId())) {
                                    continue;
                                } else {
                                    reTotalAmount = Double.valueOf(reData.get(0).getAllocationAmount());
                                }
                                AmountUnit amountTypeRe = amountUnitRepository.findByAmountTypeId(reData.get(0).getAmountType());
                                reAmountUnit = amountTypeRe.getAmount();
                                reFinalAmount = reTotalAmount * reAmountUnit / reqAmount;
                            }
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);
                            CgUnit unitN = cgUnitRepository.findByUnit(unitIds);

                            if (count == 0)
                                table.addCell(bHead.getSubHeadDescr());
                            else
                                table.addCell(" ");
                            table.addCell(unitN.getDescr());
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(reFinalAmount)));

                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            reSum += Float.parseFloat(new BigDecimal(reFinalAmount).toPlainString());

                            grTotalAlloc += sum;
                            grTotalAddition += reSum;

                        }
                    }

                }
                if (count != 0) {
                    PdfPCell cell20 = new PdfPCell(new Phrase(" TOTAL", cellFont));
                    PdfPCell cell21 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sum)), cellFont));
                    PdfPCell cell22 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(reSum)), cellFont));

                    table.addCell(" ");
                    table.addCell(cell20);
                    table.addCell(cell21);
                    table.addCell(cell22);
                    count = 0;
                }

            }
            PdfPCell cell210 = new PdfPCell(new Phrase("GRAND TOTAL", cellFont));
            PdfPCell cell211 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), cellFont));
            PdfPCell cell212 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), cellFont));

            table.addCell(" ");
            table.addCell(cell210);
            table.addCell(cell211);
            table.addCell(cell212);

            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + type.getAllocDesc().toUpperCase() + "And" + types.getAllocDesc().toUpperCase() + "_Allocation-Report.pdf");
            dto.setFileName(type.getAllocDesc().toUpperCase() + "And" + types.getAllocDesc().toUpperCase() + "_Allocation-Report.pdf");
            dtoList.add(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }
    @Override
    public ApiResponse<List<FilePathResponse>> getBEREAllocationReportDoc(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeBE == null || allocationTypeBE.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE BE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeRE == null || allocationTypeRE.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE RE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeBE.equalsIgnoreCase(allocationTypeRE)) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "BE or RE ALLOCATION CAN NOT BE SAME", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }
        AllocationType type = allocationRepository.findByAllocTypeId(allocationTypeBE);
        AllocationType types = allocationRepository.findByAllocTypeId(allocationTypeRE);

        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationTypeBE);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("RE  ALLOCATION  REPORT"+": "+findyr.getFinYear());
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + type.getAllocDesc().toUpperCase() + "And" + types.getAllocDesc().toUpperCase() + "_Allocation_Report" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));
            XWPFTable table = document.createTable(1, 4);
            table.setWidth("100%");
            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 12, "REVENUE OBJECT HEAD ", true);
            XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 12, "UNIT ", true);
            XWPFParagraph paragraphtableRowOne2 = tableRowOne.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2.createRun(), 12, type.getAllocDesc().toUpperCase() + " " + "ALLOCATION IN:" + " (" + amountIn + " )", true);
            XWPFParagraph paragraphtableRowOne3 = tableRowOne.getCell(3).addParagraph();
            boldText(paragraphtableRowOne3.createRun(), 12, types.getAllocDesc().toUpperCase() + " " + "ALLOCATION IN:" + " (" + amountIn + " )", true);

            int i = 1;
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(subHeadId, finYearId, allocationTypeBE, "0");
                List<BudgetAllocation> reportDetails = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                int sz = reportDetails.size();
                if(sz<=0)
                    continue;
                XWPFTable table11 = document.createTable(sz, 4);
                table11.setWidth("100%");

                int count = 0;
                float sum = 0;
                Double amount = Double.valueOf(0);
                Double amountUnit;
                Double finAmount;
                float reSum = 0;
                Double reAmountUnit = 0.0;
                Double reFinalAmount;
                Double reTotalAmount = 0.0;

                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {

                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            String unitIds = reportDetails.get(r).getToUnit();
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<BudgetAllocation> reDatas = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndIsBudgetRevision(unitIds, finYearId, subHeadId, allocationTypeRE, "0");
                            List<BudgetAllocation> reData = reDatas.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());

                            if (reData.size() <= 0) {
                                reFinalAmount = 0.0000;
                            } else
                                reTotalAmount = Double.valueOf(reData.get(0).getAllocationAmount());
                            AmountUnit amountTypeRe = amountUnitRepository.findByAmountTypeId(reData.get(0).getAmountType());
                            reAmountUnit = amountTypeRe.getAmount();
                            reFinalAmount = reTotalAmount * reAmountUnit / reqAmount;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);
                            CgUnit unitN = cgUnitRepository.findByUnit(unitIds);

                            XWPFTableRow tableRowOne111 = table11.getRow(r);
                            XWPFParagraph paragraphtableRowOne11 = tableRowOne111.getCell(0).addParagraph();
                            if (r == 0) {
                                boldText(paragraphtableRowOne11.createRun(), 10, bHead.getSubHeadDescr(), true);
                            } else {
                                boldText(paragraphtableRowOne11.createRun(), 10, "", true);
                            }
                            XWPFParagraph paragraphtableRow11 = tableRowOne111.getCell(1).addParagraph();
                            boldText(paragraphtableRow11.createRun(), 10, unitN.getDescr(), true);

                            XWPFParagraph paragraphtableRow21 = tableRowOne111.getCell(2).addParagraph();
                            boldText(paragraphtableRow21.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(finAmount)), true);

                            XWPFParagraph paragraphtableRow31 = tableRowOne111.getCell(3).addParagraph();
                            boldText(paragraphtableRow31.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(reFinalAmount)), true);

                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            reSum += Float.parseFloat(new BigDecimal(reFinalAmount).toPlainString());

                            grTotalAlloc += sum;
                            grTotalAddition += reSum;

                        }
                    }
                }
                if (count != 0) {
                    XWPFTable table222 = document.createTable(1, 4);
                    table222.setWidth("100%");
                    XWPFTableRow tableRowOne222 = table222.getRow(0);
                    XWPFParagraph paragraphtableRowOne222 = tableRowOne222.getCell(0).addParagraph();
                    boldText(paragraphtableRowOne222.createRun(), 12, "", true);
                    XWPFParagraph paragraphtableRowOne1222 = tableRowOne222.getCell(1).addParagraph();
                    boldText(paragraphtableRowOne1222.createRun(), 12, "TOTAL ", true);
                    XWPFParagraph paragraphtableRowOne2222 = tableRowOne222.getCell(2).addParagraph();
                    boldText(paragraphtableRowOne2222.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sum)), true);
                    XWPFParagraph paragraphtableRowOne2233 = tableRowOne222.getCell(3).addParagraph();
                    boldText(paragraphtableRowOne2233.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(reSum)), true);
                    count = 0;
                }
            }

            XWPFTable table223 = document.createTable(1, 4);
            table223.setWidth("100%");
            XWPFTableRow tableRowOne223 = table223.getRow(0);
            XWPFParagraph paragraphtableRowOne220 = tableRowOne223.getCell(0).addParagraph();
            boldText(paragraphtableRowOne220.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne1220 = tableRowOne223.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1220.createRun(), 12, "GRAND TOTAL ", true);
            XWPFParagraph paragraphtableRowOne2220 = tableRowOne223.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2220.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), true);
            XWPFParagraph paragraphtableRowOne2230 = tableRowOne223.getCell(3).addParagraph();
            boldText(paragraphtableRowOne2230.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), true);

            String names = approveName;
            String unitName = hrData.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + type.getAllocDesc().toUpperCase() + "And" + types.getAllocDesc().toUpperCase() + "_Allocation_Report" + ".docx");
            dto.setFileName(type.getAllocDesc().toUpperCase() + "And" + types.getAllocDesc().toUpperCase() + "_Allocation_Report");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<BEREResponce>> getBEREAllocationReportExcel(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<BEREResponce> dtoList = new ArrayList<BEREResponce>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeBE == null || allocationTypeBE.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "ALLOCATION TYPE BE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeRE == null || allocationTypeRE.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "ALLOCATION TYPE RE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationTypeBE.equalsIgnoreCase(allocationTypeRE)) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "BE or RE ALLOCATION CAN NOT BE SAME", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AllocationType type = allocationRepository.findByAllocTypeId(allocationTypeBE);
        AllocationType types = allocationRepository.findByAllocTypeId(allocationTypeRE);

        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationTypeBE);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {

            int i = 1;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndFinYearAndAllocationTypeIdAndIsBudgetRevision(subHeadId, finYearId, allocationTypeBE, "0");
                List<BudgetAllocation> reportDetails = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());


                int count = 0;
                float sum = 0;
                Double amount = Double.valueOf(0);
                Double amountUnit;
                Double finAmount;
                float reSum = 0;
                Double reAmountUnit = 0.0;
                Double reFinalAmount;
                Double reTotalAmount = 0.0;

                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {

                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<BEREResponce>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            String unitIds = reportDetails.get(r).getToUnit();
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<BudgetAllocation> reDatas = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndIsBudgetRevision(unitIds, finYearId, subHeadId, allocationTypeRE, "0");
                            List<BudgetAllocation> reData = reDatas.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());

                            if (reData.size() <= 0) {
                                reFinalAmount = 0.0000;
                            } else
                                reTotalAmount = Double.valueOf(reData.get(0).getAllocationAmount());
                            AmountUnit amountTypeRe = amountUnitRepository.findByAmountTypeId(reData.get(0).getAmountType());
                            reAmountUnit = amountTypeRe.getAmount();
                            reFinalAmount = reTotalAmount * reAmountUnit / reqAmount;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);
                            CgUnit unitN = cgUnitRepository.findByUnit(unitIds);

                            BEREResponce res = new BEREResponce();
                            res.setFistAllocation(type.getAllocDesc().toUpperCase());
                            res.setSecondAllocation(types.getAllocDesc().toUpperCase());
                            res.setFinYear(findyr.getFinYear());
                            res.setAmountIn(amountIn);

                            if (r == 0) {
                                res.setBudgetHead(bHead.getSubHeadDescr());
                            } else {
                                res.setBudgetHead("");
                            }
                            res.setUnitName(unitN.getDescr());
                            res.setFistAllocationAmount(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            res.setSecondAllocationAmount(String.format("%1$0,1.4f", new BigDecimal(reFinalAmount)));

                            dtoList.add(res);


                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            reSum += Float.parseFloat(new BigDecimal(reFinalAmount).toPlainString());

                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<BEREResponce>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getMainBEAllocationReport(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }

        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (fromDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FROM DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (toDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "TO DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AllocationType type = allocationRepository.findByAllocTypeId(allocationType);
        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());

        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();

        String amtType = "";
        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        LocalDate date = LocalDate.parse(toDate, inputFormatter);
        String formattedDate = date.format(outputFormatter);

        LocalDate frmLocal = LocalDate.parse(fromDate);
        LocalDate resultDate = frmLocal.minusDays(1);
        LocalDateTime frmlocalDateTime = LocalDateTime.of(resultDate, LocalTime.MIDNIGHT);
        Timestamp fromDateFormate = Timestamp.valueOf(frmlocalDateTime);

        LocalDate localDa = LocalDate.parse(toDate);
        LocalDate resultDt = localDa.plusDays(1);
        LocalDateTime localDateTime = LocalDateTime.of(resultDt, LocalTime.MIDNIGHT);
        Timestamp toDateFormate = Timestamp.valueOf(localDateTime);

        try {
            Document document = new Document(PageSize.A4_LANDSCAPE);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + type.getAllocDesc().toUpperCase() + "_FER-budget-report.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();
            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk(type.getAllocDesc().toUpperCase()+" "+"FER_ALLOCATION REPORT", boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));

            PdfPTable tabless = new PdfPTable(1);
            tabless.setWidthPercentage(100);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell1ss = new PdfPCell(new Phrase(" COAST GUARD BUDGET : FY : "+findyr.getFinYear(), cellFont));
            cell1ss.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            PdfPCell cell1ss1 = new PdfPCell(new Phrase("AMOUNT IN:  ("+amountIn.toUpperCase()+")", cellFont));
            cell1ss1.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            tabless.addCell(cell1ss);
            tabless.addCell(cell1ss1);
            document.add(tabless);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            PdfPCell cell1 = new PdfPCell(new Phrase("REVENUE OBJECT HEAD ", cellFont));
            PdfPCell cell2 = new PdfPCell(new Phrase(type.getAllocDesc().toUpperCase()+" "+findyr.getFinYear()+" "+" Allocation to ICG", cellFont));
            PdfPCell cell3 = new PdfPCell(new Phrase("UNIT", cellFont));
            PdfPCell cell4 = new PdfPCell(new Phrase(type.getAllocDesc().toUpperCase()+": "+findyr.getFinYear()+" "+" Allocation", cellFont));
            PdfPCell cell5 = new PdfPCell(new Phrase("Bill Submission Upto "+" "+formattedDate, cellFont));
            PdfPCell cell6 = new PdfPCell(new Phrase("% Bill Submission w.r.t. "+" "+type.getAllocDesc().toUpperCase()+": "+findyr.getFinYear(), cellFont));
            PdfPCell cell7 = new PdfPCell(new Phrase("CGDA Booking Upto "+" "+formattedDate, cellFont));
            PdfPCell cell8 = new PdfPCell(new Phrase("% Bill Clearance w.r.t."+" "+type.getAllocDesc().toUpperCase()+": "+findyr.getFinYear(), cellFont));

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);
            table.addCell(cell5);
            table.addCell(cell6);
            table.addCell(cell7);
            table.addCell(cell8);

            int i = 1;
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            float grTotalSum = 0;
            String finyear = "";
            String unit = "";

            Double IcgAmount = 0.0;
            for (String val : rowData) {
                String subHeadId = val;
                String hrUnit = hrData.getUnitId();
                System.out.println("Sorting " + subHeadId);
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndAllocationTypeIdAndIsFlagAndIsBudgetRevision(subHeadId, allocationType, "0", "0");
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                List<BudgetAllocation> hrDetails = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndIsBudgetRevision(hrUnit, finYearId, subHeadId, allocationType, "0");
                if (hrDetails.size() > 0) {
                    Double hrAllocAmount = Double.valueOf(hrDetails.get(0).getAllocationAmount());
                    AmountUnit hrAmount = amountUnitRepository.findByAmountTypeId(hrDetails.get(0).getAmountType());
                    Double hrAmountUnit = hrAmount.getAmount();
                    IcgAmount = hrAllocAmount * hrAmountUnit / reqAmount;
                }
                int count = 0;
                float sum = 0;
                float expsum = 0;
                float percentagesum = 0;
                Double amount = Double.valueOf(0);
                Double amountUnit;
                Double finAmount;
                Double eAmount;
                Double expnAmount;
                Double allAmount = 0.0;

                for (BudgetAllocation row : reportDetails) {

                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getToUnit())) {


                            if (row.getToUnit().equalsIgnoreCase(hrData.getUnitId())) {
                                continue;
                            } else {
                                amount = Double.valueOf(row.getAllocationAmount());
                            }
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            String uid = row.getToUnit();
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<ContigentBill> expenditure1 = contigentBillRepository.findByCbUnitIdAndFinYearAndBudgetHeadIDAndIsUpdate(uid, finYearId, subHeadId, "0");

                            List<ContigentBill> expenditure = expenditure1.stream()
                                    .filter(e -> e.getCbDate().after(fromDateFormate) && e.getCbDate().before(toDateFormate)).collect(Collectors.toList());
                            if (expenditure.size() > 0) {
                                double totalAmount = 0.0;
                                for (ContigentBill bill : expenditure) {
                                    totalAmount += Double.parseDouble(bill.getCbAmount());
                                }
                                DecimalFormat decimalFormat = new DecimalFormat("#");
                                String cbAmount = decimalFormat.format(totalAmount);
                                eAmount = Double.parseDouble(cbAmount);
                            } else {
                                eAmount = 0.0;
                            }
                            if (finAmount != 0)
                                expnAmount = eAmount * 100 / (amount * amountUnit);
                            else
                                expnAmount = 0.0;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);
                            CgUnit unitN = cgUnitRepository.findByUnit(row.getToUnit());

                            if (count == 0) {
                                table.addCell(bHead.getSubHeadDescr());
                                table.addCell(String.format("%1$0,1.4f", new BigDecimal(IcgAmount)));
                            } else {
                                table.addCell("");
                                table.addCell("");
                            }
                            table.addCell(unitN.getDescr());
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(eAmount)));
                            table.addCell(String.format("%1$0,1.4f", new BigDecimal(expnAmount)));
                            table.addCell(" ");
                            table.addCell(" ");

                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            expsum += Float.parseFloat(new BigDecimal(eAmount).toPlainString());
                            percentagesum += Float.parseFloat(new BigDecimal(expnAmount).toPlainString());

                            grTotalAlloc += sum;
                            grTotalAddition += expsum;
                            grTotalSum += percentagesum;
                        }
                    }

                }
                if (count != 0) {
                    PdfPCell cell10 = new PdfPCell(new Phrase("TOTAL", cellFont));
                    PdfPCell cell20 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(sum)), cellFont));
                    PdfPCell cell30 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(expsum)), cellFont));
                    PdfPCell cell40 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(percentagesum)), cellFont));

                    table.addCell(" ");
                    table.addCell(" ");
                    table.addCell(cell10);
                    table.addCell(cell20);
                    table.addCell(cell30);
                    table.addCell(cell40);
                    table.addCell(" ");
                    table.addCell(" ");
                    count = 0;
                }

            }
            PdfPCell cell50 = new PdfPCell(new Phrase("GRAND TOTAL", cellFont));
            PdfPCell cell60 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), cellFont));
            PdfPCell cell70 = new PdfPCell(new Phrase( String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), cellFont));
            PdfPCell cell80 = new PdfPCell(new Phrase( String.format("%1$0,1.4f", new BigDecimal(grTotalSum)), cellFont));

            table.addCell(" ");
            table.addCell(" ");
            table.addCell(cell50);
            table.addCell(cell60);
            table.addCell(cell70);
            table.addCell(cell80);
            table.addCell(" ");
            table.addCell(" ");

            document.add(table);

            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + type.getAllocDesc().toUpperCase() + "_FER-budget-report.pdf");
            dto.setFileName(type.getAllocDesc().toUpperCase() + "_FER-budget-report.pdf");
            dtoList.add(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getMainBEAllocationReportDoc(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }

        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (fromDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FROM DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (toDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "TO DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AllocationType type = allocationRepository.findByAllocTypeId(allocationType);
        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());

        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);
        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {
            int i = 1;
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            LocalDate date = LocalDate.parse(toDate, inputFormatter);
            String formattedDate = date.format(outputFormatter);

            LocalDate frmLocal = LocalDate.parse(fromDate);
            LocalDate resultDate = frmLocal.minusDays(1);
            LocalDateTime frmlocalDateTime = LocalDateTime.of(resultDate, LocalTime.MIDNIGHT);
            Timestamp fromDateFormate = Timestamp.valueOf(frmlocalDateTime);

            LocalDate localDa = LocalDate.parse(toDate);
            LocalDate resultDt = localDa.plusDays(1);
            LocalDateTime localDateTime = LocalDateTime.of(resultDt, LocalTime.MIDNIGHT);
            Timestamp toDateFormate = Timestamp.valueOf(localDateTime);

            XWPFDocument document = new XWPFDocument();

            CTDocument1 ctDocument = document.getDocument();
            CTBody ctBody = ctDocument.getBody();
            CTSectPr ctSectPr = (ctBody.isSetSectPr()) ? ctBody.getSectPr() : ctBody.addNewSectPr();
            CTPageSz ctPageSz = (ctSectPr.isSetPgSz()) ? ctSectPr.getPgSz() : ctSectPr.addNewPgSz();
            ctPageSz.setOrient(STPageOrientation.LANDSCAPE);
            ctPageSz.setW(java.math.BigInteger.valueOf(Math.round(120 * 1440))); //11 inches
            ctPageSz.setH(java.math.BigInteger.valueOf(Math.round(8.5 * 1440))); //8.5 inches


            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText(type.getAllocDesc().toUpperCase() + "_FER" + "_ALLOCATION REPORT");
            headingRun.setBold(true);
            headingRun.setFontSize(16);


            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + type.getAllocDesc().toUpperCase() + "_FER" + "_Allocation_Report" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));
            XWPFTable tab = document.createTable(2, 1);
            tab.setWidth("100%");
            XWPFTableRow tabRow = tab.getRow(0);
            XWPFParagraph paragraph1 = tabRow.getCell(0).addParagraph();
            boldText(paragraph1.createRun(), 10, "COAST GUARD BUDUGET: FY: " + findyr.getFinYear(), true);
            paragraph1.setAlignment(ParagraphAlignment.CENTER);

            XWPFTableRow tabRow1 = tab.getRow(1);
            XWPFParagraph paragraph2 = tabRow1.getCell(0).addParagraph();
            boldText(paragraph2.createRun(), 10, "( " + amountIn + " )", true);
            paragraph2.setAlignment(ParagraphAlignment.RIGHT);

            XWPFParagraph sp = document.createParagraph();
            sp.setSpacingAfter(1);

            XWPFTable table = document.createTable(1, 8);
            table.setWidth("100%");
            XWPFTableRow tableRowOne = table.getRow(0);
            XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
            boldText(paragraphtableRowOne.createRun(), 12, "REVENUE OBJECT HEAD ", true);
            XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1.createRun(), 12, type.getAllocDesc().toUpperCase() + " " + findyr.getFinYear() + " " + "ALLOCATION TO ICG", true);
            XWPFParagraph paragraphtableRowOne2 = tableRowOne.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2.createRun(), 12, "UNIT", true);
            XWPFParagraph paragraphtableRowOne3 = tableRowOne.getCell(3).addParagraph();
            boldText(paragraphtableRowOne3.createRun(), 12, type.getAllocDesc().toUpperCase() + ": " + findyr.getFinYear() + " " + "ALLOCATION", true);
            XWPFParagraph paragraphtableRowOne4 = tableRowOne.getCell(4).addParagraph();
            boldText(paragraphtableRowOne4.createRun(), 12, "BILL SUBMISSION UPTO : " + formattedDate, true);
            XWPFParagraph paragraphtableRowOne5 = tableRowOne.getCell(5).addParagraph();
            boldText(paragraphtableRowOne5.createRun(), 12, "% BILL SUBMISSION w.r.t : " + type.getAllocDesc().toUpperCase() + " " + findyr.getFinYear(), true);
            XWPFParagraph paragraphtableRowOne6 = tableRowOne.getCell(6).addParagraph();
            boldText(paragraphtableRowOne6.createRun(), 12, "CGDA BOOKING UPTO : " + formattedDate, true);
            XWPFParagraph paragraphtableRowOne7 = tableRowOne.getCell(7).addParagraph();
            boldText(paragraphtableRowOne7.createRun(), 12, "% BILL CLEARANCE w.r.t : " + type.getAllocDesc().toUpperCase() + " " + findyr.getFinYear(), true);
            Double IcgAmount = 0.0;
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            float grTotalSum = 0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndAllocationTypeIdAndIsFlagAndIsBudgetRevision(subHeadId, allocationType, "0", "0");
                String hrUnit = hrData.getUnitId();
                List<BudgetAllocation> hrDetails = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndIsBudgetRevision(hrUnit, finYearId, subHeadId, allocationType, "0");
                if (hrDetails.size() > 0) {
                    Double hrAllocAmount = Double.valueOf(hrDetails.get(0).getAllocationAmount());
                    AmountUnit hrAmount = amountUnitRepository.findByAmountTypeId(hrDetails.get(0).getAmountType());
                    Double hrAmountUnit = hrAmount.getAmount();
                    IcgAmount = hrAllocAmount * hrAmountUnit / reqAmount;
                }
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                int sz = reportDetails.size();
                if(sz<=0)
                    continue;
                XWPFTable table11 = document.createTable(sz, 8);
                table11.setWidth("100%");
                int count = 0;
                float sum = 0;
                float expsum = 0;
                float percentagesum = 0;
                Double amount = Double.valueOf(0);
                Double amountUnit;
                Double finAmount;
                Double eAmount;
                Double expnAmount;
                Double allAmount = 0.0;

                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {

                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            String uid = reportDetails.get(r).getToUnit();
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<ContigentBill> expenditure1 = contigentBillRepository.findByCbUnitIdAndFinYearAndBudgetHeadIDAndIsUpdate(uid, finYearId, subHeadId, "0");

                            List<ContigentBill> expenditure = expenditure1.stream()
                                    .filter(e -> e.getCbDate().after(fromDateFormate) && e.getCbDate().before(toDateFormate)).collect(Collectors.toList());
                            if (expenditure.size() > 0) {
                                double totalAmount = 0.0;
                                for (ContigentBill bill : expenditure) {
                                    totalAmount += Double.parseDouble(bill.getCbAmount());
                                }
                                DecimalFormat decimalFormat = new DecimalFormat("#");
                                String cbAmount = decimalFormat.format(totalAmount);
                                eAmount = Double.parseDouble(cbAmount);
                            } else {
                                eAmount = 0.0;
                            }
                            if (finAmount != 0)
                                expnAmount = eAmount * 100 / (amount * amountUnit);
                            else
                                expnAmount = 0.0;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);
                            CgUnit unitN = cgUnitRepository.findByUnit(reportDetails.get(r).getToUnit());

                            XWPFTableRow tableRowOne111 = table11.getRow(r);
                            XWPFParagraph paragraphtableRowOne11 = tableRowOne111.getCell(0).addParagraph();
                            if (r == 0) {
                                boldText(paragraphtableRowOne11.createRun(), 10, bHead.getSubHeadDescr(), true);
                            } else {
                                boldText(paragraphtableRowOne11.createRun(), 10, "", true);
                            }
                            XWPFParagraph paragraphtableRow11 = tableRowOne111.getCell(1).addParagraph();
                            if (r == 0) {
                                boldText(paragraphtableRow11.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(IcgAmount)), true);
                            } else {
                                boldText(paragraphtableRow11.createRun(), 10, "", true);
                            }

                            XWPFParagraph paragraphtableRow21 = tableRowOne111.getCell(2).addParagraph();
                            boldText(paragraphtableRow21.createRun(), 10, unitN.getDescr(), true);

                            XWPFParagraph paragraphtableRow31 = tableRowOne111.getCell(3).addParagraph();
                            boldText(paragraphtableRow31.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(finAmount)), true);

                            XWPFParagraph paragraphtableRow41 = tableRowOne111.getCell(4).addParagraph();
                            boldText(paragraphtableRow41.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(eAmount)), true);

                            XWPFParagraph paragraphtableRow51 = tableRowOne111.getCell(5).addParagraph();
                            boldText(paragraphtableRow51.createRun(), 10, String.format("%1$0,1.9f", new BigDecimal(expnAmount)), true);

                            XWPFParagraph paragraphtableRow61 = tableRowOne111.getCell(6).addParagraph();
                            boldText(paragraphtableRow61.createRun(), 10, "", true);

                            XWPFParagraph paragraphtableRow71 = tableRowOne111.getCell(7).addParagraph();
                            boldText(paragraphtableRow71.createRun(), 10, "", true);

                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            expsum += Float.parseFloat(new BigDecimal(eAmount).toPlainString());
                            percentagesum += Float.parseFloat(new BigDecimal(expnAmount).toPlainString());


                            grTotalAlloc += sum;
                            grTotalAddition += expsum;
                            grTotalSum += percentagesum;
                        }
                    }

                }
                if (count != 0) {

                    XWPFTable table222 = document.createTable(1, 8);
                    table222.setWidth("100%");
                    XWPFTableRow tableRowOne222 = table222.getRow(0);
                    XWPFParagraph paragraphtableRowOne222 = tableRowOne222.getCell(0).addParagraph();
                    boldText(paragraphtableRowOne222.createRun(), 12, "", true);
                    XWPFParagraph paragraphtableRowOne1222 = tableRowOne222.getCell(1).addParagraph();
                    boldText(paragraphtableRowOne1222.createRun(), 12, "", true);
                    XWPFParagraph paragraphtableRowOne2222 = tableRowOne222.getCell(2).addParagraph();
                    boldText(paragraphtableRowOne2222.createRun(), 12, "TOTAL", true);
                    XWPFParagraph paragraphtableRowOne2233 = tableRowOne222.getCell(3).addParagraph();
                    boldText(paragraphtableRowOne2233.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(sum)), true);
                    XWPFParagraph paragraphtableRowOne2244 = tableRowOne222.getCell(4).addParagraph();
                    boldText(paragraphtableRowOne2244.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(expsum)), true);
                    XWPFParagraph paragraphtableRowOne2255 = tableRowOne222.getCell(5).addParagraph();
                    boldText(paragraphtableRowOne2255.createRun(), 12, String.format("%1$0,1.9f", new BigDecimal(percentagesum)), true);
                    XWPFParagraph paragraphtableRowOne2266 = tableRowOne222.getCell(6).addParagraph();
                    boldText(paragraphtableRowOne2266.createRun(), 12, "", true);
                    XWPFParagraph paragraphtableRowOne2277 = tableRowOne222.getCell(7).addParagraph();
                    boldText(paragraphtableRowOne2277.createRun(), 12, "", true);
                }

            }
            XWPFTable table220 = document.createTable(1, 8);
            table220.setWidth("100%");
            XWPFTableRow tableRowOne220 = table220.getRow(0);
            XWPFParagraph paragraphtableRowOne220 = tableRowOne220.getCell(0).addParagraph();
            boldText(paragraphtableRowOne220.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne1220 = tableRowOne220.getCell(1).addParagraph();
            boldText(paragraphtableRowOne1220.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne2220 = tableRowOne220.getCell(2).addParagraph();
            boldText(paragraphtableRowOne2220.createRun(), 12, " GRAND TOTAL", true);
            XWPFParagraph paragraphtableRowOne2230 = tableRowOne220.getCell(3).addParagraph();
            boldText(paragraphtableRowOne2230.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), true);
            XWPFParagraph paragraphtableRowOne2200 = tableRowOne220.getCell(4).addParagraph();
            boldText(paragraphtableRowOne2200.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), true);
            XWPFParagraph paragraphtableRowOne2250 = tableRowOne220.getCell(5).addParagraph();
            boldText(paragraphtableRowOne2250.createRun(), 12, String.format("%1$0,1.9f", new BigDecimal(grTotalSum)), true);
            XWPFParagraph paragraphtableRowOne2260 = tableRowOne220.getCell(6).addParagraph();
            boldText(paragraphtableRowOne2260.createRun(), 12, "", true);
            XWPFParagraph paragraphtableRowOne2270 = tableRowOne220.getCell(7).addParagraph();
            boldText(paragraphtableRowOne2270.createRun(), 12, "", true);

            String names = approveName;
            String unitName = hrData.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + type.getAllocDesc().toUpperCase() + "_FER" + "_Allocation_Report" + ".docx");
            dto.setFileName(type.getAllocDesc().toUpperCase() + "_FER" + "_Allocation_Report");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<FerResponse>> getMainBEAllocationReportExcel(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FerResponse> responce = new ArrayList<FerResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if (finYearId == null || finYearId.isEmpty()) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "FINANCIAL YEAR CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (allocationType == null || allocationType.isEmpty()) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "ALLOCATION TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }

        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (fromDate == null) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "FROM DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (toDate == null) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "TO DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }

        AllocationType type = allocationRepository.findByAllocTypeId(allocationType);
        List<String> rowDatas = budgetAllocationRepository.findSubHead(finYearId, allocationType);
        List<String> rowData = rowDatas.stream().sorted(Comparator.comparing(str -> str.substring(str.length() - 2))).collect(Collectors.toList());

        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);
        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<FerResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        try {
            int i = 1;
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            LocalDate date = LocalDate.parse(toDate, inputFormatter);
            String formattedDate = date.format(outputFormatter);

            LocalDate frmLocal = LocalDate.parse(fromDate);
            LocalDate resultDate = frmLocal.minusDays(1);
            LocalDateTime frmlocalDateTime = LocalDateTime.of(resultDate, LocalTime.MIDNIGHT);
            Timestamp fromDateFormate = Timestamp.valueOf(frmlocalDateTime);

            LocalDate localDa = LocalDate.parse(toDate);
            LocalDate resultDt = localDa.plusDays(1);
            LocalDateTime localDateTime = LocalDateTime.of(resultDt, LocalTime.MIDNIGHT);
            Timestamp toDateFormate = Timestamp.valueOf(localDateTime);

            FerResponse res = new FerResponse();
            res.setFinYear(findyr.getFinYear());
            res.setAmountIn(amountIn);
            res.setAllocationType(type.getAllocDesc().toUpperCase());
            res.setUpToDate(formattedDate);
            List<FerSubResponse> addRes = new ArrayList<FerSubResponse>();


            Double IcgAmount = 0.0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocation> reportDetail = budgetAllocationRepository.findBySubHeadAndAllocationTypeIdAndIsFlagAndIsBudgetRevision(subHeadId, allocationType, "0", "0");
                String hrUnit = hrData.getUnitId();
                List<BudgetAllocation> hrDetails = budgetAllocationRepository.findByToUnitAndFinYearAndSubHeadAndAllocationTypeIdAndIsBudgetRevision(hrUnit, finYearId, subHeadId, allocationType, "0");
                if (hrDetails.size() > 0) {
                    Double hrAllocAmount = Double.valueOf(hrDetails.get(0).getAllocationAmount());
                    AmountUnit hrAmount = amountUnitRepository.findByAmountTypeId(hrDetails.get(0).getAmountType());
                    Double hrAmountUnit = hrAmount.getAmount();
                    IcgAmount = hrAllocAmount * hrAmountUnit / reqAmount;
                }
                List<BudgetAllocation> reportDetailss = reportDetail.stream().filter(e -> !e.getToUnit().equalsIgnoreCase(hrData.getUnitId())).collect(Collectors.toList());
                List<BudgetAllocation> reportDetails=reportDetailss.stream().filter(e->Double.valueOf(e.getAllocationAmount())!=0).collect(Collectors.toList());

                int count = 0;
                float sum = 0;
                float expsum = 0;
                float percentagesum = 0;
                Double amount = Double.valueOf(0);
                Double amountUnit;
                Double finAmount;
                Double eAmount;
                Double expnAmount;
                Double allAmount = 0.0;

                for (Integer r = 0; r < reportDetails.size(); r++) {
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(reportDetails.get(r).getToUnit())) {

                            FerSubResponse subResp = new FerSubResponse();


                            amount = Double.valueOf(reportDetails.get(r).getAllocationAmount());
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(reportDetails.get(r).getAmountType());

                            String uid = reportDetails.get(r).getToUnit();
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<ContigentBill> expenditure1 = contigentBillRepository.findByCbUnitIdAndFinYearAndBudgetHeadIDAndIsUpdate(uid, finYearId, subHeadId, "0");

                            List<ContigentBill> expenditure = expenditure1.stream()
                                    .filter(e -> e.getCbDate().after(fromDateFormate) && e.getCbDate().before(toDateFormate)).collect(Collectors.toList());
                            if (expenditure.size() > 0) {
                                double totalAmount = 0.0;
                                for (ContigentBill bill : expenditure) {
                                    totalAmount += Double.parseDouble(bill.getCbAmount());
                                }
                                DecimalFormat decimalFormat = new DecimalFormat("#");
                                String cbAmount = decimalFormat.format(totalAmount);
                                eAmount = Double.parseDouble(cbAmount);
                            } else {
                                eAmount = 0.0;
                            }
                            if (finAmount != 0)
                                expnAmount = eAmount * 100 / (amount * amountUnit);
                            else
                                expnAmount = 0.0;
                            BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);
                            CgUnit unitN = cgUnitRepository.findByUnit(reportDetails.get(r).getToUnit());

                            subResp.setSubHead(bHead.getSubHeadDescr());
                            subResp.setUnitName(unitN.getDescr());
                            subResp.setIcgAllocAmount(String.format("%1$0,1.4f", new BigDecimal(IcgAmount)));
                            subResp.setAllocAmount(String.format("%1$0,1.4f", new BigDecimal(finAmount)));
                            subResp.setBillSubmission(String.format("%1$0,1.4f", new BigDecimal(eAmount)));
                            subResp.setPercentageBill(String.format("%1$0,1.9f", new BigDecimal(expnAmount)));
                            subResp.setCgdaBooking("");
                            subResp.setPercentageBillClearnce("");
                            addRes.add(subResp);

                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            expsum += Float.parseFloat(new BigDecimal(eAmount).toPlainString());
                            percentagesum += Float.parseFloat(new BigDecimal(expnAmount).toPlainString());
                        }
                    }

                }

            }
            res.setFerDetails(addRes);
            responce.add(res);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(responce, new TypeReference<List<FerResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getUnitRebaseReport(String fromDate, String toDate) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        String amountTypeId = "101";

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if (amountTypeId == null || amountTypeId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AMOUNT TYPE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (fromDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FROM DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        if (toDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "TO DATE CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrData.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType().toUpperCase();

        String names = approveName;
        String unitName = hrData.getUnit();
        String rank = approveRank;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        LocalDate date = LocalDate.parse(toDate, inputFormatter);
        String formattedDate = date.format(outputFormatter);

        LocalDate frmLocal = LocalDate.parse(fromDate);
        LocalDate resultDate = frmLocal.minusDays(1);
        LocalDateTime frmlocalDateTime = LocalDateTime.of(resultDate, LocalTime.MIDNIGHT);
        Timestamp fromDateFormate = Timestamp.valueOf(frmlocalDateTime);

        LocalDate localDa = LocalDate.parse(toDate);
        LocalDate resultDt = localDa.plusDays(1);
        LocalDateTime localDateTime = LocalDateTime.of(resultDt, LocalTime.MIDNIGHT);
        Timestamp toDateFormate = Timestamp.valueOf(localDateTime);

        List<String> groupUnitId = budgetRebaseRepository.findGroupRebaseUnit();
        if (groupUnitId.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "DATA NOT FOUND FROM DB", HttpStatus.OK.value());
        }

        try {
            Document document = new Document(PageSize.A4);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + "Rebase_Report.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(new File(path)));

            document.open();
            Paragraph paragraph = new Paragraph();
            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            paragraph.add(new Chunk("UNIT  REBASE  REPORT", boldFont));
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(paragraph);
            document.add(new Paragraph("\n"));

            int i = 1;
            String finyear = "";
            String unit = "";

            String RunitId = "";
            String uName = "";
            String frmStation = "";
            String toStation = "";
            String finYear = "";
            String subHead = "";
            String headCodeId = "";
            Double allocAmount;
            Double expAmount;
            Double balAmount;
            Timestamp LastCbD;
            String val = "";
            int no=1;
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            float grTotalSum = 0;
            Double amountUnit;
            AllocationType allocType;
            if (groupUnitId.size() > 0) {
                int count = 1;
                for (String ids : groupUnitId) {

                    RunitId = ids;
                    List<BudgetRebase> rebaseDatas = budgetRebaseRepository.findByRebaseUnitId(RunitId);
                    List<BudgetRebase> rebaseData1 = rebaseDatas.stream()
                            .filter(e -> e.getOccuranceDate().after(fromDateFormate) && e.getOccuranceDate().before(toDateFormate)).collect(Collectors.toList());
                    List<BudgetRebase> rebaseDatass = rebaseData1.stream().sorted(Comparator.comparing(data -> data.getBudgetHeadId().substring(data.getBudgetHeadId().length() - 2))).collect(Collectors.toList());
                    List<BudgetRebase> rebaseData=rebaseDatass.stream().filter(e->Double.valueOf(e.getAllocAmount())!=0).collect(Collectors.toList());

                    if (rebaseData.size() <= 0) {
                        throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "DATA NOT FOUND IN THIS DATE RANGE");
                    }
                    CgUnit unitN = cgUnitRepository.findByUnit(RunitId);
                    String frmS = rebaseData.get(0).getFrmStationId();
                    CgStation toS = cgStationRepository.findByStationId(rebaseData.get(0).getToStationId());

                    uName = unitN.getDescr();
                    Date rebaseDate = rebaseData.get(0).getOccuranceDate();
                    System.out.println("RBDATE" + rebaseDate);
                    frmStation = frmS;
                    toStation = toS.getStationName();

                    Paragraph paragraph11 = new Paragraph();
                    Font boldFontss = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
                    paragraph11.add(new Chunk("Serial No: "+no, boldFontss));
                    document.add(paragraph11);
                    document.add(new Paragraph("\n"));


                    PdfPTable table1 = new PdfPTable(4);
                    table1.setWidthPercentage(100);

                    Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                    PdfPCell cell1 = new PdfPCell(new Phrase("Unit Name ", cellFont));
                    PdfPCell cell2 = new PdfPCell(new Phrase(uName, cellFont));
                    PdfPCell cell3 = new PdfPCell(new Phrase("From Station", cellFont));
                    PdfPCell cell4 = new PdfPCell(new Phrase(frmStation, cellFont));
                    PdfPCell cell5 = new PdfPCell(new Phrase("Date of Rebase", cellFont));
                    PdfPCell cell6 = new PdfPCell(new Phrase(String.valueOf(rebaseDate), cellFont));
                    PdfPCell cell7 = new PdfPCell(new Phrase("To Station", cellFont));
                    PdfPCell cell8 = new PdfPCell(new Phrase(toStation, cellFont));

                    table1.addCell(cell1);
                    table1.addCell(cell2);
                    table1.addCell(cell3);
                    table1.addCell(cell4);
                    table1.addCell(cell5);
                    table1.addCell(cell6);
                    table1.addCell(cell7);
                    table1.addCell(cell8);

                    document.add(table1);


                    PdfPTable table = new PdfPTable(6);
                    table.setWidthPercentage(100);
                    PdfPCell cell10 = new PdfPCell(new Phrase("FINANCIAL YEAR & ALLOCATION TYPE ", cellFont));
                    PdfPCell cell20 = new PdfPCell(new Phrase("REVENUE OBJECT HEAD", cellFont));
                    PdfPCell cell40 = new PdfPCell(new Phrase("ALLOCATION IN: ( "+amountIn+")", cellFont));
                    PdfPCell cell50 = new PdfPCell(new Phrase("EXPENDITURE IN: (INR)",cellFont));
                    PdfPCell cell60 = new PdfPCell(new Phrase("BALANCE IN : ( "+amountIn+")", cellFont));
                    PdfPCell cell70 = new PdfPCell(new Phrase("LAST CB DATE", cellFont));

                    table.addCell(cell10);
                    table.addCell(cell20);
                    table.addCell(cell40);
                    table.addCell(cell50);
                    table.addCell(cell60);
                    table.addCell(cell70);

                    no++;

                    for (Integer k = 0; k < rebaseData.size(); k++) {
                        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(rebaseData.get(k).getFinYear());
                        String bHeads = rebaseData.get(k).getBudgetHeadId();
                        BudgetHead bHead = subHeadRepository.findByBudgetCodeId(bHeads);
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(rebaseData.get(k).getAmountType());
                        String allc = rebaseData.get(k).getAllocTypeId();
                        allocType = allocationRepository.findByAllocTypeId(allc);
                        if (amountTypeObj == null) {
                            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                            }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                        }
                        amountUnit = amountTypeObj.getAmount();
                        finYear = findyr.getFinYear();
                        subHead = bHead.getSubHeadDescr();
                        headCodeId = bHead.getBudgetHeadId();
                        Double aAmount = Double.valueOf(rebaseData.get(k).getAllocAmount());
                        Double eAmount = Double.valueOf(rebaseData.get(k).getExpAmount());
                        Double bAmount = Double.valueOf(rebaseData.get(k).getBalAmount());
                        String cbD = "";
                        if (rebaseData.get(k).getLastCbDate() != null) {
                            LastCbD = rebaseData.get(k).getLastCbDate();
                            SimpleDateFormat id = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                            SimpleDateFormat od = new SimpleDateFormat("dd-MMMM-yyyy");
                            Date dateC = id.parse(LastCbD.toString());
                            cbD = od.format(dateC);
                        } else
                            cbD = "";
                        allocAmount = aAmount * amountUnit / reqAmount;
                        expAmount = eAmount;
                        balAmount = bAmount * amountUnit / reqAmount;

                        table.addCell(finYear + " " + allocType.getAllocType().toUpperCase());
                        table.addCell(subHead);
                        table.addCell(String.format("%1$0,1.4f", new BigDecimal(allocAmount)));
                        table.addCell(String.format("%1$0,1.4f", new BigDecimal(expAmount)));
                        table.addCell(String.format("%1$0,1.4f", new BigDecimal(balAmount)));
                        table.addCell(cbD);

                        grTotalAlloc += allocAmount;
                        grTotalAddition += expAmount;
                        grTotalSum += balAmount;
                    }
                    PdfPCell cell99 = new PdfPCell(new Phrase("GRAND TOTAL", cellFont));
                    PdfPCell cell88 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), cellFont));
                    PdfPCell cell77 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), cellFont));
                    PdfPCell cell66 = new PdfPCell(new Phrase(String.format("%1$0,1.4f", new BigDecimal(grTotalSum)), cellFont));

                    table.addCell(" ");
                    table.addCell(cell99);
                    table.addCell(cell88);
                    table.addCell(cell77);
                    table.addCell(cell66);
                    table.addCell(" ");
                    document.add(table);

                }
            }


            document.add(new Paragraph("\n"));
            Paragraph heading1 = new Paragraph(formattedDateTime);
            heading1.setAlignment(Paragraph.ALIGN_LEFT);
            document.add(heading1);

            document.add(new Paragraph("\n"));
            Paragraph heading2 = new Paragraph(names+"\n"+unitName+"\n"+rank);
            heading2.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(heading2);

            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + "Rebase_Report.pdf");
            dto.setFileName("Rebase_Report.pdf");
            dtoList.add(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }
    @Override
    public ApiResponse<List<FilePathResponse>> getUnitRebaseReportDoc(String fromDate, String toDate) {
        List<UnitRebaseReportResponce> responce = new ArrayList<UnitRebaseReportResponce>();
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrDataCheck = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        if (hrDataCheck == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "YOU ARE NOT AUTHORIZED TO UPDATE USER STATUS", HttpStatus.OK.value());
        } else {
            if (hrDataCheck.getRoleId().contains(HelperUtils.SYSTEMADMIN)) {
            } else {
                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                }, "YOU ARE NOT AUTHORIZED TO REBASE THE STATION", HttpStatus.OK.value());
            }
        }
        if (fromDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "FROM DATE CAN NOT BE NULL", HttpStatus.OK.value());
        }
        if (toDate == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "TO DATE NOT BE NULL", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrDataCheck.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }
        List<String> groupUnitId = budgetRebaseRepository.findGroupRebaseUnit();
        if (groupUnitId.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "DATA NOT FOUND FROM DB", HttpStatus.OK.value());
        }

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        LocalDate date = LocalDate.parse(toDate, inputFormatter);
        String formattedDate = date.format(outputFormatter);

        LocalDate frmLocal = LocalDate.parse(fromDate);
        LocalDate resultDate = frmLocal.minusDays(1);
        LocalDateTime frmlocalDateTime = LocalDateTime.of(resultDate, LocalTime.MIDNIGHT);
        Timestamp fromDateFormate = Timestamp.valueOf(frmlocalDateTime);

        LocalDate localDa = LocalDate.parse(toDate);
        LocalDate resultDt = localDa.plusDays(1);
        LocalDateTime localDateTime = LocalDateTime.of(resultDt, LocalTime.MIDNIGHT);
        Timestamp toDateFormate = Timestamp.valueOf(localDateTime);

        try {
            XWPFDocument document = new XWPFDocument();
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setAlignment(ParagraphAlignment.CENTER);
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText("UNIT REBASE REPORT");
            headingRun.setBold(true);
            headingRun.setFontSize(16);

            XWPFParagraph spacingParagraphss = document.createParagraph();
            spacingParagraphss.setSpacingAfter(20);

            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String path = folder.getAbsolutePath() + "/" + "UnitRebaseReport" + ".docx";
            FileOutputStream out = new FileOutputStream(new File(path));
            int count = 1;
            String RunitId = "";
            float grTotalAlloc = 0;
            float grTotalAddition = 0;
            float grTotalSum = 0;
            if (groupUnitId.size() > 0) {
                for (String ids : groupUnitId) {
                    RunitId = ids;
                    List<BudgetRebase> rebaseDatas = budgetRebaseRepository.findByRebaseUnitId(RunitId);
                    List<BudgetRebase> rebaseData1 = rebaseDatas.stream()
                            .filter(e -> e.getOccuranceDate().after(fromDateFormate) && e.getOccuranceDate().before(toDateFormate)).collect(Collectors.toList());
                    List<BudgetRebase> rebaseDatass = rebaseData1.stream().sorted(Comparator.comparing(data -> data.getBudgetHeadId().substring(data.getBudgetHeadId().length() - 2))).collect(Collectors.toList());
                    List<BudgetRebase> rebaseData=rebaseDatass.stream().filter(e->Double.valueOf(e.getAllocAmount())!=0).collect(Collectors.toList());

                    if (rebaseData.size() <= 0) {
                        throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "DATA NOT FOUND IN THIS DATE RANGE");
                    }
                    UnitRebaseReportResponce rebase = new UnitRebaseReportResponce();
                    CgUnit unitN = cgUnitRepository.findByUnit(RunitId);
                    CgStation toS = cgStationRepository.findByStationId(rebaseData.get(0).getToStationId());
                    AmountUnit amountTypeObjs = amountUnitRepository.findByAmountTypeId(rebaseData.get(0).getAmountType());

                    rebase.setUnitName(unitN.getDescr());
                    rebase.setDateOfRebase(rebaseData.get(0).getOccuranceDate());
                    rebase.setFromStation(rebaseData.get(0).getFrmStationId());
                    rebase.setToStation(toS.getStationName());
                    int size = rebaseData.size();

                    XWPFParagraph headingParagraph11 = document.createParagraph();
                    headingParagraph11.setAlignment(ParagraphAlignment.LEFT);
                    headingParagraph11.setStyle("Heading1");
                    XWPFRun headingRun11 = headingParagraph11.createRun();
                    headingRun11.setText("Serial No:" + count);
                    headingRun11.setBold(true);
                    headingRun11.setColor("0000FF");
                    headingRun11.setFontSize(12);

                    XWPFTable table = document.createTable(2, 4);
                    table.setWidth("100%");


                    XWPFTableRow tableRowOne = table.getRow(0);
                    XWPFParagraph paragraphtableRowOne = tableRowOne.getCell(0).addParagraph();
                    boldText(paragraphtableRowOne.createRun(), 12, "UNIT NAME", true);

                    XWPFParagraph paragraphtableRowOne1 = tableRowOne.getCell(1).addParagraph();
                    boldText(paragraphtableRowOne1.createRun(), 10, unitN.getDescr(), true);

                    XWPFParagraph paragraphtableRowOne2 = tableRowOne.getCell(2).addParagraph();
                    boldText(paragraphtableRowOne2.createRun(), 12, "FROM STATION", true);

                    XWPFParagraph paragraphtableRowOne3 = tableRowOne.getCell(3).addParagraph();
                    boldText(paragraphtableRowOne3.createRun(), 10, rebaseData.get(0).getFrmStationId(), true);

                    XWPFTableRow tableRowTwo = table.getRow(1);
                    XWPFParagraph paragraphtableRowTwo = tableRowTwo.getCell(0).addParagraph();
                    boldText(paragraphtableRowTwo.createRun(), 12, "REBASE DATE", true);

                    XWPFParagraph paragraphtableRowTwo1 = tableRowTwo.getCell(1).addParagraph();
                    boldText(paragraphtableRowTwo1.createRun(), 10, rebaseData.get(0).getOccuranceDate().toString(), true);

                    XWPFParagraph paragraphtableRowTwo2 = tableRowTwo.getCell(2).addParagraph();
                    boldText(paragraphtableRowTwo2.createRun(), 12, "TO STATION", true);

                    XWPFParagraph paragraphtableRowTwo3 = tableRowTwo.getCell(3).addParagraph();
                    boldText(paragraphtableRowTwo3.createRun(), 10, toS.getStationName(), true);

                    XWPFParagraph spacingParagraph = document.createParagraph();
                    spacingParagraph.setSpacingAfter(20);

                    List<UnitRebaseSubReportResponce> addRes = new ArrayList<UnitRebaseSubReportResponce>();
                    XWPFTable table1 = document.createTable(size + 1, 6);
                    table1.setWidth("100%");

                    XWPFTableRow tableRow = table1.getRow(0);
                    XWPFParagraph paragraphtableRow0 = tableRow.getCell(0).addParagraph();
                    boldText(paragraphtableRow0.createRun(), 12, "FINANCIAL YEAR & ALLOCATION", true);

                    XWPFParagraph paragraphtableRow1 = tableRow.getCell(1).addParagraph();
                    boldText(paragraphtableRow1.createRun(), 12, "SUB HEAD", true);

                    XWPFParagraph paragraphtableRow2 = tableRow.getCell(2).addParagraph();
                    boldText(paragraphtableRow2.createRun(), 12, "ALLOCATION IN: (" + amountTypeObjs.getAmountType() + ")", true);

                    XWPFParagraph paragraphtableRow3 = tableRow.getCell(3).addParagraph();
                    boldText(paragraphtableRow3.createRun(), 12, "EXPENDITURE : (INR)", true);

                    XWPFParagraph paragraphtableRow4 = tableRow.getCell(4).addParagraph();
                    boldText(paragraphtableRow4.createRun(), 12, "BALANCE IN: (" + amountTypeObjs.getAmountType() + ")", true);

                    XWPFParagraph paragraphtableRow5 = tableRow.getCell(5).addParagraph();
                    boldText(paragraphtableRow5.createRun(), 12, "LAST CB DATE:", true);
                    count++;

                    XWPFParagraph spacingParagraph1 = document.createParagraph();
                    spacingParagraph1.setSpacingAfter(20);

                    for (Integer k = 0; k < rebaseData.size(); k++) {
                        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(rebaseData.get(k).getFinYear());
                        BudgetHead bHead = subHeadRepository.findByBudgetCodeId(rebaseData.get(k).getBudgetHeadId());
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(rebaseData.get(k).getAmountType());
                        AllocationType allocType = allocationRepository.findByAllocTypeId(rebaseData.get(k).getAllocTypeId());
                        UnitRebaseSubReportResponce subResp = new UnitRebaseSubReportResponce();
                        subResp.setFinYear(findyr.getFinYear());
                        subResp.setAllocationType(allocType.getAllocDesc().toUpperCase());
                        subResp.setSubHead(bHead.getSubHeadDescr());
                        subResp.setAllocationAmount(rebaseData.get(k).getAllocAmount());
                        subResp.setExpenditureAmount(rebaseData.get(k).getExpAmount());
                        subResp.setBalAmount(rebaseData.get(k).getBalAmount());
                        subResp.setAmountType(amountTypeObj.getAmountType());
                        if (rebaseData.get(k).getLastCbDate() != null)
                            subResp.setLastCbDate(rebaseData.get(k).getLastCbDate());


                        XWPFTableRow tableRows = table1.getRow(k + 1);
                        XWPFParagraph paragraphtableRow01 = tableRows.getCell(0).addParagraph();
                        boldText(paragraphtableRow01.createRun(), 10, findyr.getFinYear() + " " + allocType.getAllocDesc().toUpperCase(), true);

                        XWPFParagraph paragraphtableRow11 = tableRows.getCell(1).addParagraph();
                        boldText(paragraphtableRow11.createRun(), 10, bHead.getSubHeadDescr(), true);

                        XWPFParagraph paragraphtableRow21 = tableRows.getCell(2).addParagraph();
                        boldText(paragraphtableRow21.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(rebaseData.get(k).getAllocAmount())), true);

                        XWPFParagraph paragraphtableRow31 = tableRows.getCell(3).addParagraph();
                        boldText(paragraphtableRow31.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(rebaseData.get(k).getExpAmount())), true);

                        XWPFParagraph paragraphtableRow41 = tableRows.getCell(4).addParagraph();
                        boldText(paragraphtableRow41.createRun(), 10, String.format("%1$0,1.4f", new BigDecimal(rebaseData.get(k).getBalAmount())), true);
                        if (rebaseData.get(k).getLastCbDate() != null) {
                            Date LastCbD = rebaseData.get(k).getLastCbDate();
                            SimpleDateFormat id = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                            SimpleDateFormat od = new SimpleDateFormat("dd-MMMM-yyyy");
                            Date dateC = id.parse(LastCbD.toString());
                            String cbD = od.format(dateC);
                            XWPFParagraph paragraphtableRow51 = tableRows.getCell(5).addParagraph();
                            boldText(paragraphtableRow51.createRun(), 10, cbD, true);
                        } else {
                            XWPFParagraph paragraphtableRow51 = tableRows.getCell(5).addParagraph();
                            boldText(paragraphtableRow51.createRun(), 10, null, true);
                        }
                        Double sumExisting = Double.valueOf(rebaseData.get(k).getAllocAmount());
                        Double sumExp = Double.valueOf(rebaseData.get(k).getExpAmount());
                        Double sumBaal = Double.valueOf(rebaseData.get(k).getBalAmount());

                        grTotalAlloc += sumExisting;
                        grTotalAddition += sumExp;
                        grTotalSum += sumBaal;
                        addRes.add(subResp);
                    }
                    rebase.setList(addRes);
                    responce.add(rebase);

                    XWPFTable table111 = document.createTable(1, 6);
                    table111.setWidth("100%");

                    XWPFTableRow tableRow111 = table111.getRow(0);
                    XWPFParagraph paragraphtableRow01 = tableRow111.getCell(0).addParagraph();
                    boldText(paragraphtableRow01.createRun(), 12, "      ", true);

                    XWPFParagraph paragraphtableRow11 = tableRow111.getCell(1).addParagraph();
                    boldText(paragraphtableRow11.createRun(), 12, "GRAND TOTAL", true);

                    XWPFParagraph paragraphtableRow21 = tableRow111.getCell(2).addParagraph();
                    boldText(paragraphtableRow21.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAlloc)), true);

                    XWPFParagraph paragraphtableRow31 = tableRow111.getCell(3).addParagraph();
                    boldText(paragraphtableRow31.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalAddition)), true);

                    XWPFParagraph paragraphtableRow41 = tableRow111.getCell(4).addParagraph();
                    boldText(paragraphtableRow41.createRun(), 12, String.format("%1$0,1.4f", new BigDecimal(grTotalSum)), true);

                    XWPFParagraph paragraphtableRow51 = tableRow111.getCell(5).addParagraph();
                    boldText(paragraphtableRow51.createRun(), 12, "       ", true);
                }
            }
            String names = approveName;
            String unitName = hrDataCheck.getUnit();
            String rank = approveRank;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            XWPFParagraph mainParagraph = document.createParagraph();
            mainParagraph = document.createParagraph();
            mainParagraph.createRun().addBreak();
            mainParagraph = document.createParagraph();
            boldText(mainParagraph.createRun(), 10, formattedDateTime + "", true);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, names + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, unitName + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            mainParagraph = document.createParagraph();
            normalText(mainParagraph.createRun(), 10, rank + "", true);
            mainParagraph.setAlignment(ParagraphAlignment.RIGHT);
            document.write(out);
            out.close();
            document.close();
            FilePathResponse dto = new FilePathResponse();
            dto.setPath(HelperUtils.FILEPATH + "UnitRebaseReport" + ".docx");
            dto.setFileName("UnitRebaseReport");
            dtoList.add(dto);
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<UnitRebaseReportResponce>> getUnitRebaseReportExcel(String fromDate, String toDate) {
        List<UnitRebaseReportResponce> responce = new ArrayList<UnitRebaseReportResponce>();
        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrDataCheck = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        if (hrDataCheck == null) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<UnitRebaseReportResponce>>() {
            }, "YOU ARE NOT AUTHORIZED TO UPDATE USER STATUS", HttpStatus.OK.value());
        } else {
            if (hrDataCheck.getRoleId().contains(HelperUtils.SYSTEMADMIN)) {
            } else {
                return ResponseUtils.createFailureResponse(responce, new TypeReference<List<UnitRebaseReportResponce>>() {
                }, "YOU ARE NOT AUTHORIZED TO REBASE THE STATION", HttpStatus.OK.value());
            }
        }
        if (fromDate == null) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<UnitRebaseReportResponce>>() {
            }, "FROM DATE CAN NOT BE NULL", HttpStatus.OK.value());
        }
        if (toDate == null) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<UnitRebaseReportResponce>>() {
            }, "TO DATE NOT BE NULL", HttpStatus.OK.value());
        }

        List<String> groupUnitId = budgetRebaseRepository.findGroupRebaseUnit();
        if (groupUnitId.size() <= 0) {
            return ResponseUtils.createFailureResponse(responce, new TypeReference<List<UnitRebaseReportResponce>>() {
            }, "DATA NOT FOUND FROM DB", HttpStatus.OK.value());
        }
        List<HrData> hrDataList = hrDataRepository.findByUnitIdAndIsActive(hrDataCheck.getUnitId(), "1");
        if (hrDataList.size() == 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO ROLE ASSIGN FOR THIS UNIT.");
        }

        String approverPId = "";
        String approveName = "";
        String approveRank = "";

        for (Integer k = 0; k < hrDataList.size(); k++) {
            HrData findHrData = hrDataList.get(k);
            if (findHrData.getRoleId().contains(HelperUtils.BUDGETAPPROVER)) {
                approverPId = findHrData.getPid();
                approveName = findHrData.getFullName();
                approveRank = findHrData.getRank();
            }
        }
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        LocalDate date = LocalDate.parse(toDate, inputFormatter);
        String formattedDate = date.format(outputFormatter);

        LocalDate frmLocal = LocalDate.parse(fromDate);
        LocalDate resultDate = frmLocal.minusDays(1);
        LocalDateTime frmlocalDateTime = LocalDateTime.of(resultDate, LocalTime.MIDNIGHT);
        Timestamp fromDateFormate = Timestamp.valueOf(frmlocalDateTime);

        LocalDate localDa = LocalDate.parse(toDate);
        LocalDate resultDt = localDa.plusDays(1);
        LocalDateTime localDateTime = LocalDateTime.of(resultDt, LocalTime.MIDNIGHT);
        Timestamp toDateFormate = Timestamp.valueOf(localDateTime);

        try {

            int count = 1;
            String RunitId = "";
            if (groupUnitId.size() > 0) {
                for (String ids : groupUnitId) {
                    RunitId = ids;
                    List<BudgetRebase> rebaseDatas = budgetRebaseRepository.findByRebaseUnitId(RunitId);
                    List<BudgetRebase> rebaseData1 = rebaseDatas.stream()
                            .filter(e -> e.getOccuranceDate().after(fromDateFormate) && e.getOccuranceDate().before(toDateFormate)).collect(Collectors.toList());
                    List<BudgetRebase> rebaseDatass = rebaseData1.stream().sorted(Comparator.comparing(data -> data.getBudgetHeadId().substring(data.getBudgetHeadId().length() - 2))).collect(Collectors.toList());
                    List<BudgetRebase> rebaseData=rebaseDatass.stream().filter(e->Double.valueOf(e.getAllocAmount())!=0).collect(Collectors.toList());

                    if (rebaseData.size() <= 0) {
                        throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "DATA NOT FOUND IN THIS DATE RANGE");
                    }
                    UnitRebaseReportResponce rebase = new UnitRebaseReportResponce();
                    CgUnit unitN = cgUnitRepository.findByUnit(RunitId);
                    CgStation toS = cgStationRepository.findByStationId(rebaseData.get(0).getToStationId());
                    AmountUnit amountTypeObjs = amountUnitRepository.findByAmountTypeId(rebaseData.get(0).getAmountType());

                    rebase.setUnitName(unitN.getDescr());
                    rebase.setDateOfRebase(rebaseData.get(0).getOccuranceDate());
                    rebase.setFromStation(rebaseData.get(0).getFrmStationId());
                    rebase.setToStation(toS.getStationName());

                    List<UnitRebaseSubReportResponce> addRes = new ArrayList<UnitRebaseSubReportResponce>();
                    count++;
                    for (Integer k = 0; k < rebaseData.size(); k++) {
                        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(rebaseData.get(k).getFinYear());
                        BudgetHead bHead = subHeadRepository.findByBudgetCodeId(rebaseData.get(k).getBudgetHeadId());
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(rebaseData.get(k).getAmountType());
                        AllocationType allocType = allocationRepository.findByAllocTypeId(rebaseData.get(k).getAllocTypeId());
                        UnitRebaseSubReportResponce subResp = new UnitRebaseSubReportResponce();
                        subResp.setFinYear(findyr.getFinYear());
                        subResp.setAllocationType(allocType.getAllocDesc());
                        subResp.setSubHead(bHead.getSubHeadDescr());
                        subResp.setAllocationAmount(rebaseData.get(k).getAllocAmount());
                        subResp.setExpenditureAmount(rebaseData.get(k).getExpAmount());
                        subResp.setBalAmount(rebaseData.get(k).getBalAmount());
                        subResp.setAmountType(amountTypeObj.getAmountType());
                        if (rebaseData.get(k).getLastCbDate() != null) {
                            subResp.setLastCbDate(rebaseData.get(k).getLastCbDate());
                        }
                        addRes.add(subResp);
                    }
                    rebase.setList(addRes);
                    responce.add(rebase);
                }
            }
        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Error occurred");
        }
        return ResponseUtils.createSuccessResponse(responce, new TypeReference<List<UnitRebaseReportResponce>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getRevisedAllocationReport(String authGroupId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();
        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }
        if (authGroupId == null || authGroupId.isEmpty()) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "AUTHGROUP ID CAN NOT BE NULL OR EMPTY", HttpStatus.OK.value());
        }

/*        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        AllocationType allockData = allocationRepository.findByAllocTypeId(allocationType);
        String allocType = allockData.getAllocType();
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);*/


        List<String> rowData = budgetAllocationDetailsRepository.findSubHead(authGroupId);
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
/*        String amtType = "";
        String names = hrData.getFullName();
        String unitName = hrData.getUnit();
        String rank = hrData.getRank();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        String dBunit = cgUnit.getDescr();
        List<CgUnit> units = new ArrayList<>();
        if (dBunit.equalsIgnoreCase("D(Budget)")) {
            units = cgUnitRepository.findAllByOrderByDescrAsc();
        } else {
            if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
            } else {
                units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
            }
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }*/
        String htmlContent = new String();
        try {
            //htmlContent = FileUtils.readFileToString(new File("src/main/resources/templates/re-allocation-report.html"), "UTF-8");
            //htmlContent = FileUtils.readFileToString(new File(new File(".").getCanonicalPath()+"/webapps/budget/WEB-INF/classes/templates/re-allocation-report.html"), "UTF-8");
            htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "  <title> Revised Report</title>\n" +
                    "  <meta charset=\"utf-8\"></meta>\n" +
                    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></meta>\n" +
                    "  \n" +
                    "<style>\n" +
                    ".bold{\n" +
                    "\tfont-weight: bold !important;\n" +
                    "}\n" +
                    ".bbtm{\n" +
                    "\tborder-bottom: 1px solid transparent !important;\n" +
                    "}\n" +
                    ".brtm{\n" +
                    "\tborder-right: 1px solid transparent !important;\n" +
                    "}\n" +
                    ".bold{\n" +
                    "\tfont-weight: bold;\n" +
                    "}\n" +
                    ".wrapper{\n" +
                    "\twidth: 70%;\n" +
                    "\tmargin: 100px auto;\n" +
                    "}\n" +
                    "\n" +
                    ":root {\n" +
                    "    --bg-table-stripe: #f6f6f5;\n" +
                    "    --b-table: #e3e3e2;\n" +
                    "    --caption: #242423;\n" +
                    "}\n" +
                    "th {\n" +
                    "\ttext-align:left;\n" +
                    "\tborder: 1px solid #242423 ;\n" +
                    "}\n" +
                    "td{\n" +
                    "\tborder: 1px solid #242423 ;\n" +
                    "}\n" +
                    "\n" +
                    "table {\n" +
                    "    background-color: transparent;\n" +
                    "    border-collapse:collapse;\n" +
                    "  \tfont-family: Arial, Helvetica, sans-serif\n" +
                    "}\n" +
                    "\n" +
                    "th {\n" +
                    "    text-align:left\n" +
                    "}\n" +
                    "\n" +
                    ".dcf-txt-center {\n" +
                    "      text-align: center!important\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-txt-left {\n" +
                    "      text-align: left!important\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-txt-right {\n" +
                    "      text-align: right!important\n" +
                    "    }\n" +
                    "    \n" +
                    ".dcf-table caption {\n" +
                    "      color: var(--caption);\n" +
                    "      font-size: 1.13em;\n" +
                    "      font-weight: 700;\n" +
                    "      padding-bottom: .56rem\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table thead {\n" +
                    "      font-size: .84em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table tbody {\n" +
                    "      border-bottom: 1px solid var(--b-table);\n" +
                    "      border-top: 1px solid var(--b-table);\n" +
                    "      font-size: .84em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table tfoot {\n" +
                    "      font-size: .84em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table td, .dcf-table th {\n" +
                    "      padding-right: 1.78em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table-bordered, .dcf-table-bordered td, .dcf-table-bordered th {\n" +
                    "      border: 1px solid var(--b-table)\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table-bordered td, .dcf-table-bordered th, .dcf-table-striped td, .dcf-table-striped th {\n" +
                    "      padding-left: 1em;\n" +
                    "      padding-right: 1em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table-bordered tr:not(:last-child), .dcf-table-striped tr:not(:last-child) {\n" +
                    "      border-bottom: 1px solid var(--b-table)\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table-striped tbody tr:nth-of-type(2n) {\n" +
                    "      background-color: transparent;\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table thead td, .dcf-table thead th {\n" +
                    "      padding-bottom: .75em;\n" +
                    "      vertical-align: bottom\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table tbody td, .dcf-table tbody th, .dcf-table tfoot td, .dcf-table tfoot th {\n" +
                    "      padding-top: .75em;\n" +
                    "      vertical-align: top\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table tbody td, .dcf-table tbody th {\n" +
                    "      padding-bottom: .75em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-table-bordered thead th {\n" +
                    "      padding-top: 1.33em\n" +
                    "    }\n" +
                    "\n" +
                    "    .dcf-wrapper-table-scroll {\n" +
                    "      overflow-x: auto;\n" +
                    "      -webkit-overflow-scrolling: touch;\n" +
                    "      left: 50%;\n" +
                    "      margin-left: -50vw;\n" +
                    "      margin-right: -50vw;\n" +
                    "      padding-bottom: 1em;\n" +
                    "      position: relative;\n" +
                    "      right: 50%;\n" +
                    "      width: 100vw\n" +
                    "    }\n" +
                    "\n" +
                    "    @media only screen and (max-width:42.09em) {\n" +
                    "      .dcf-table-responsive thead {\n" +
                    "        clip: rect(0 0 0 0);\n" +
                    "        -webkit-clip-path: inset(50%);\n" +
                    "        clip-path: inset(50%);\n" +
                    "        height: 1px;\n" +
                    "        overflow: hidden;\n" +
                    "        position: absolute;\n" +
                    "        width: 1px;\n" +
                    "        white-space: nowrap\n" +
                    "      }\n" +
                    "      .dcf-table-responsive tr {\n" +
                    "        display: block\n" +
                    "      }\n" +
                    "      .dcf-table-responsive td {\n" +
                    "        -webkit-column-gap: 3.16vw;\n" +
                    "        -moz-column-gap: 3.16vw;\n" +
                    "        column-gap: 3.16vw;\n" +
                    "        display: grid;\n" +
                    "        grid-template-columns: 1fr 2fr;\n" +
                    "        text-align: left!important\n" +
                    "      }\n" +
                    "      .dcf-table-responsive.dcf-table-bordered, .dcf-table-responsive.dcf-table-bordered thead th {\n" +
                    "        border-width: 0\n" +
                    "      }\n" +
                    "      .dcf-table-responsive.dcf-table-bordered tbody td {\n" +
                    "        border-top-width: 0\n" +
                    "      }\n" +
                    "      .dcf-table-responsive:not(.dcf-table-bordered) tbody tr {\n" +
                    "        padding-bottom: .75em\n" +
                    "      }\n" +
                    "      .dcf-table-responsive:not(.dcf-table-bordered) tbody td {\n" +
                    "        padding-bottom: 0\n" +
                    "      }\n" +
                    "      .dcf-table-responsive:not(.dcf-table-bordered):not(.dcf-table-striped) tbody td {\n" +
                    "        padding-right: 0\n" +
                    "      }\n" +
                    "      .dcf-table-responsive.dcf-table-bordered tbody tr:last-child td:last-child {\n" +
                    "        border-bottom-width: 0\n" +
                    "      }\n" +
                    "      .dcf-table-responsive tbody td:before {\n" +
                    "        content: attr(data-label);\n" +
                    "        float: left;\n" +
                    "        font-weight: 700;\n" +
                    "        padding-right: 1.78em\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    ".dcf-overflow-x-auto {\n" +
                    "      overflow-x: auto!important;\n" +
                    "      -webkit-overflow-scrolling: touch\n" +
                    "    }\n" +
                    ".sign {\n" +
                    "\t\t float: right;\n" +
                    "\t\t padding-right: 20px;\n" +
                    "\t\t padding-top: 20px;\n" +
                    "\t\t width:27%;\n" +
                    "\t }\n" +
                    "\n" +
                    ".count {\n" +
                    "\ttext-align: center;\n" +
                    "\tpadding-top: 200px;\n" +
                    "}\n" +
                    "\n" +
                    ".table3 {\n" +
                    "\tpadding-top: 55px;\n" +
                    "}\n" +
                    "\n" +
                    ".tab {\n" +
                    "\tpadding-left: 85px;\n" +
                    "}\n" +
                    ".table4{\n" +
                    "\tpadding-top: 15px;\n" +
                    "}\n" +
                    ".wrap{\n" +
                    "\twidth:70%;\n" +
                    "\tmargin: 100px auto;\n" +
                    "}\n" +
                    ".date {\n" +
                    "\tfloat: left;\n" +
                    "\tpadding-left: 20px;\n" +
                    "\tpadding-top: 20px;\n" +
                    "\twidth:45%;\n" +
                    "}\n" +
                    "\n" +
                    ".count {\n" +
                    "\ttext-align: center;\n" +
                    "\tpadding-top: 200px;\n" +
                    "}\n" +
                    "\n" +
                    ".table3 {\n" +
                    "\tpadding-top: 55px;\n" +
                    "}\n" +
                    "\n" +
                    ".tab {\n" +
                    "\tpadding-left: 85px;\n" +
                    "}\n" +
                    ".table4{\n" +
                    "\tpadding-top: 15px;\n" +
                    "}\n" +
                    ".wrap{\n" +
                    "\twidth:70%;\n" +
                    "\tmargin: 100px auto;\n" +
                    "}\n" +
                    "    \n" +
                    ".dcf-w-100\\% {\n" +
                    "  width: 100%!important;\n" +
                    "\t\t}\n" +
                    "    \n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"wrapper\"> \n" +
                    "\t<h2 style=\"text-align: center;\">${allocaType_placeholder} ALLOCATION REPORT</h2><br></br>\n" +
                    "\t<table class=\"dcf-table dcf-table-responsive dcf-table-bordered dcf-table-striped dcf-w-100%\">\n" +
                    "\t\t<thead>\n" +
                    "\t\t\t<tr>\n" +
                    "\t\t\t\t<th class=\"dcf-txt-center bbtm\" scope=\"col\">UNIT</th>\n" +
                    "\t\t\t\t<th scope=\"col\"></th>\n" +
                    "\t\t\t\t<th class=\"dcf-txt-center\" scope=\"col\">${allocaType_placeholder} ${finYear_placeholder}  Allocation</th>\n" +
                    "\t\t\t\t<th scope=\"col\"></th>\n" +
                    "\t\t\t</tr>\n" +
                    "\t\t</thead>\n" +
                    "\t\t<tbody>\n" +
                    "\n" +
                    "\t\t\t<tr>\n" +
                    "\t\t\t\t<td data-label=\"CGCs/RHQs/ADG(P) Directorates at CGHQ\"></td>\n" +
                    "\t\t\t\t<td class=\"dcf-txt-center brtm bold\">Allocation Amount (in ${amountType_placeholder})</td>\n" +
                    "\t\t\t\t<td class=\"dcf-txt-center brtm bold\" data-label=\"RE 2022-23 Allocation\">Additional/Withdrawal (in ${amountType_placeholder})</td>\n" +
                    "\t\t\t\t<td class=\"dcf-txt-center bold\">Revised (in ${amountType_placeholder})</td>\n" +
                    "\t\t\t</tr>\n" +
                    "\n" +
                    "\t\t\t${data_placeholder}\n" +
                    "\n" +
                    "\t\t</tbody>\n" +
                    "\t</table>\n" +
                    "\t<div class=\"sign\">\n" +
                    "\t\t<ul style=\"list-style: none; margin-top: 0;\">\n" +
                    "\t\t\t<li>${name_placeholder}</li>\n" +
                    "\t\t\t<li>${unit_placeholder}</li>\n" +
                    "\t\t\t<li>${rank_placeholder}</li>\n" +
                    "\t\t</ul>\n" +
                    "\t</div>\n" +
                    "\t<div class=\"date\">\n" +
                    "\t\tDate-${date_placeholder}\n" +
                    "\t</div>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>\n";

            StringBuilder sb = new StringBuilder();
            int i = 1;

            String names = hrData.getFullName();
            String unitName = hrData.getUnit();
            String rank = hrData.getRank();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            String finyear = "";
            String amountTypeId = "";
            String allocTypes = "";
            Double amount = Double.valueOf(0);
            String amountUnit = "";
            Double finAmount;
            Double revisedAmount;
            Double reAmount;
            Double s2 = 0.0;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocationDetails> reportDetails = budgetAllocationDetailsRepository.findByAuthGroupIdAndSubHeadAndIsDelete(authGroupId, subHeadId, "0");
   /*             if (reportDetails.size() <= 0) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "RECORD NOT FOUND OR EMPTY", HttpStatus.OK.value());
                }*/
                BudgetHead bHead = subHeadRepository.findByBudgetCodeId(subHeadId);

                int count = 0;
                sb.append("<tr>");
                sb.append("<td class=\"the\" ><b>").append(StringEscapeUtils.escapeHtml4(bHead.getSubHeadDescr())).append("</b></td>");
                sb.append("<td class=\"the\"></td>");
                sb.append("<td class=\"the\"></td>");
                sb.append("<td class=\"the\"></td>");
                sb.append("</tr>");
                float sumExisting = 0;
                float sumRE = 0;
                float total = 0;
                for (BudgetAllocationDetails row : reportDetails) {
                    AllocationType allockData = allocationRepository.findByAllocTypeId(row.getAllocTypeId());
                    BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(row.getFinYear());
                    AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                    CgUnit unitN = cgUnitRepository.findByUnit(row.getToUnit());
                    allocTypes = allockData.getAllocType();
                    finyear = findyr.getFinYear();
                    amountUnit = amountTypeObj.getAmountType();

                    amount = Double.valueOf(row.getAllocationAmount());
                    if (row.getRevisedAmount() != null) {
                        revisedAmount = Double.valueOf(row.getRevisedAmount());
                    } else
                        revisedAmount = 0.0;

                    finAmount = amount;
                    reAmount = revisedAmount;
                    String s = reAmount.toString();
                    if (s.contains("-")) {
                        String s1 = s.replace("-", "");
                        s2 = Double.parseDouble(s1);
                    }

                    sb.append("<tr>");
                    sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(unitN.getDescr())).append("</td>");
                    sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(finAmount)))).append("</td>");
                    if (reAmount < 0)
                        sb.append("<td class=\"the\">(-)").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(s2)))).append("</td>");
                    else if (reAmount > 0)
                        sb.append("<td class=\"the\"> (+)").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(reAmount)))).append("</td>");
                    else
                        sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(reAmount)))).append("</td>");
                    sb.append("<td class=\"the\">").append(String.format("%1$0,1.4f", (new BigDecimal((Float.parseFloat(Double.toString(finAmount)) + Float.parseFloat(Double.toString(reAmount))))))).append("</td>");
                    sb.append("</tr>");
                    sumExisting += Float.parseFloat(new BigDecimal(Double.toString(finAmount)).toPlainString());
                    sumRE += Float.parseFloat(new BigDecimal(Double.toString(reAmount)).toPlainString());
                }
                total = sumExisting + sumRE;
                Double ss2 = 0.0;
                String ss = Float.toString(sumRE);
                if (ss.contains("-")) {
                    String ss1 = ss.replace("-", "");
                    ss2 = Double.parseDouble(ss1);
                }
                sb.append("<tr>");
                sb.append("<td class=\"the bold\">").append("Total").append("</td>");
                sb.append("<td class=\"the bold\">").append(String.format("%1$0,1.4f", new BigDecimal(sumExisting))).append("</td>");
                if (sumRE < 0)
                    sb.append("<td class=\"the bold\">(-)").append(String.format("%1$0,1.4f", new BigDecimal(ss2))).append("</td>");
                else if (sumRE > 0)
                    sb.append("<td class=\"the bold\">(+)").append(String.format("%1$0,1.4f", new BigDecimal(sumRE))).append("</td>");
                else
                    sb.append("<td class=\"the bold\">").append(String.format("%1$0,1.4f", new BigDecimal(sumRE))).append("</td>");
                sb.append("<td class=\"the bold\">").append(String.format("%1$0,1.4f", new BigDecimal(total))).append("</td>");
                sb.append("</tr>");
            }
            if (sb.toString().isEmpty()) {
                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                }, "RECORD NOT FOUND", HttpStatus.OK.value());
            }

            htmlContent = htmlContent.replace("${finYear_placeholder}", StringEscapeUtils.escapeHtml4(finyear));
            htmlContent = htmlContent.replace("${amountType_placeholder}", StringEscapeUtils.escapeHtml4(amountUnit));
            htmlContent = htmlContent.replace("${allocaType_placeholder}", StringEscapeUtils.escapeHtml4(allocTypes.toUpperCase()));
            htmlContent = htmlContent.replace("${name_placeholder}", StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}", StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}", StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}", StringEscapeUtils.escapeHtml4(formattedDateTime));


            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());
            String filepath = HelperUtils.FILEPATH + "/" + "Revision_Allocation-Report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "Revision_Allocation-Report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());
            //generatePdf(htmlContent, filepath);
            FilePathResponse response = new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("Revision_Allocation-Report.pdf");

            dtoList.add(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }


    public static void generatePdf(String htmlContent, String outputPdfFile) throws Exception {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(new FileOutputStream(outputPdfFile));
    }


    private String convertToXhtml(String html) throws UnsupportedEncodingException {
        Context ctx = new Context();
        String renderedHtmlContent = templateEngine.process(html, ctx);

        Tidy tidy = new Tidy();
        tidy.setInputEncoding(UTF_8);
        tidy.setOutputEncoding(UTF_8);
        tidy.setXHTML(true);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        tidy.parseDOM(inputStream, outputStream);
        return outputStream.toString(UTF_8);
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