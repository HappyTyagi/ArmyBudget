package com.sdd.service.Impl;


import com.fasterxml.jackson.core.type.TypeReference;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class MangeReportImpl implements MangeReportService {

    private static final String UTF_8 = "UTF-8";

    @Autowired
    private TemplateEngine templateEngine;

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
    BudgetAllocationReportRepository budgetAllocationReportRepository;

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
    private ContigentBillRepository contigentBillRepository;

    @Override
    public ApiResponse<List<FilePathResponse>> getAllocationReport(String authGroupId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(), "1");

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }


        String fileName = "AllocationReport" + hrData.getUnitId();

        HashMap<String, List<ReportSubModel>> hashMap = new HashMap<>();
        List<BudgetAllocationReport> budgetAllocationReport = budgetAllocationReportRepository.findByAuthGroupId(authGroupId);

        if (budgetAllocationReport.size() <= 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND");
        }

        for (Integer j = 0; j < budgetAllocationReport.size(); j++) {

            if (hashMap.containsKey(budgetAllocationReport.get(j).getSubHeadDes())) {
                List<ReportSubModel> reportMaindata = hashMap.get(budgetAllocationReport.get(j).getSubHeadDes());
                ReportSubModel subModel = new ReportSubModel();
                subModel.setType(budgetAllocationReport.get(j).getAllocationType());
                subModel.setRemark(budgetAllocationReport.get(j).getRemark());
                subModel.setUnit(budgetAllocationReport.get(j).getUnitName());
                subModel.setAmount(budgetAllocationReport.get(j).getTotalAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYearDes());
                reportMaindata.add(subModel);
                hashMap.put(budgetAllocationReport.get(j).getSubHeadDes(), reportMaindata);

            } else {
                List<ReportSubModel> reportMaindata = new ArrayList<ReportSubModel>();
                ReportSubModel subModel = new ReportSubModel();
                subModel.setType(budgetAllocationReport.get(j).getAllocationType());
                subModel.setRemark(budgetAllocationReport.get(j).getRemark());
                subModel.setUnit(budgetAllocationReport.get(j).getUnitName());
                subModel.setAmount(budgetAllocationReport.get(j).getTotalAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYearDes());
                reportMaindata.add(subModel);
                hashMap.put(budgetAllocationReport.get(j).getSubHeadDes(), reportMaindata);
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
                String templateName = "allocation-report.html";
                File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                File file = new File(filePath);
                pdfGenaratorUtil.createPdfAllocation(templateName, hashMap, file);
                filePathResponse.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                filePathResponse.setFileName(fileName);
                dtoList.add(filePathResponse);

            } catch (Exception e) {
                throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INTERNAL SERVER ERROR");
            }
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
        List<BudgetAllocationReport> budgetAllocationReport = budgetAllocationReportRepository.findByUnitIdAndFinYearIdAndAllocationTypeId(reportRequest.getUnitId(), reportRequest.getBudgetFinancialYearId(), "ALL_102");

        if (budgetAllocationReport.size() <= 0) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND");
        }
        String fileName = hrData.getUnitId() + System.currentTimeMillis();


        for (Integer j = 0; j < budgetAllocationReport.size(); j++) {

            if (hashMap.containsKey(budgetAllocationReport.get(j).getSubHeadDes())) {
                List<ReportSubModel> reportMaindata = hashMap.get(budgetAllocationReport.get(j).getSubHeadDes());
                ReportSubModel subModel = new ReportSubModel();
                subModel.setUnit(budgetAllocationReport.get(j).getUnitName());
                subModel.setType(budgetAllocationReport.get(j).getAllocationType());
                subModel.setRemark(budgetAllocationReport.get(j).getRemark());
                subModel.setAmount(budgetAllocationReport.get(j).getTotalAmount());
                subModel.setRevisedAmount(budgetAllocationReport.get(j).getRevisedAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYearDes());
                reportMaindata.add(subModel);
                hashMap.put(budgetAllocationReport.get(j).getSubHeadDes(), reportMaindata);
            } else {
                List<ReportSubModel> reportMaindata = new ArrayList<ReportSubModel>();
                ReportSubModel subModel = new ReportSubModel();
                subModel.setUnit(budgetAllocationReport.get(j).getUnitName());
                subModel.setType(budgetAllocationReport.get(j).getAllocationType());
                subModel.setRemark(budgetAllocationReport.get(j).getRemark());
                subModel.setAmount(budgetAllocationReport.get(j).getTotalAmount());
                subModel.setRevisedAmount(budgetAllocationReport.get(j).getRevisedAmount());
                subModel.setFinYear(budgetAllocationReport.get(j).getFinYearDes());
                reportMaindata.add(subModel);
                hashMap.put(budgetAllocationReport.get(j).getSubHeadDes(), reportMaindata);
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
                throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INTERNAL SERVER ERROR");
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


        ContigentBill cbData = contigentBillRepository.findByCbIdAndIsFlag(reportRequest.getCbId(), "0");

        if (cbData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "NO DATA FOUND.");
        }

        String fileName = "ContingentBill" + hrData.getUnitId() + cbData.getCbId();


        double allocationAmount = 0;
        double balanceAmount = 0;
        List<BudgetAllocation> modBudgetAllocations = budgetAllocationRepository.findByToUnitAndSubHeadAndIsFlag(hrData.getUnitId(), cbData.getBudgetHeadID(), "0");
        if (modBudgetAllocations.size() == 0) {

        } else {
            allocationAmount = 0;
            for (Integer i = 0; i < modBudgetAllocations.size(); i++) {
                AmountUnit amountUnit = amountUnitRepository.findByAmountTypeId(modBudgetAllocations.get(i).getAmountType());
                allocationAmount = allocationAmount + (Double.parseDouble(modBudgetAllocations.get(i).getAllocationAmount()) * amountUnit.getAmount());
                balanceAmount = balanceAmount + (Double.parseDouble(modBudgetAllocations.get(i).getBalanceAmount()) * amountUnit.getAmount());
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
        List<ContigentBill> cbExpendure = contigentBillRepository.findByCbUnitIdAndBudgetHeadIDAndIsFlag(hrData.getUnitId(), cbData.getBudgetHeadID(), "0");
        if (cbExpendure.size() == 0) {

        } else {
            expenditure = 0;
            for (Integer i = 0; i < cbExpendure.size(); i++) {
                expenditure = expenditure + Double.parseDouble(cbExpendure.get(i).getCbAmount());
            }
        }
        Authority authorityDetails= authorityRepository.findByAuthorityId(cbData.getAuthGroupId());
        CgUnit unit = cgUnitRepository.findByUnit(cbData.getCbUnitId());
        BudgetHead budgetHead = subHeadRepository.findByBudgetCodeId(cbData.getBudgetHeadID());
        HrData approverId = hrDataRepository.findByPidAndIsActive(approverCbPId, "1");
        HrData verifer = hrDataRepository.findByPidAndIsActive(veriferCbPId, "1");

        cbReportResponse.setAuthorityDetails(authorityDetails);
        cbReportResponse.setApprover(approverId);
        cbReportResponse.setVerifer(verifer);

        cbReportResponse.setOnAccountData(cbData.getOnAccountOf());
        cbReportResponse.setOnAurthyData(cbData.getAuthorityDetails());
        cbReportResponse.setExpenditureAmount(String.format("%f", expenditure));
        cbReportResponse.setCurrentBillAmount(String.format("%f", Double.parseDouble(cbData.getCbAmount())));
        cbReportResponse.setAllocatedAmount(String.format("%f", allocationAmount));
        cbReportResponse.setCbData(cbData);
        cbReportResponse.setUnitData(unit);
        cbReportResponse.setBudgetHead(budgetHead);
        cbReportResponse.setBalanceAmount(String.format("%f", (balanceAmount - expenditure)));
        cbReportResponse.setRemeningAmount(String.format("%f", ((balanceAmount - Double.parseDouble(cbData.getCbAmount())))));

        String hindiAmount = ConverterUtils.convert(Long.parseLong(cbData.getCbAmount()));
        cbReportResponse.setHindiAmount(hindiAmount);

        HashMap<String, List<ReportSubModel>> hashMap = new HashMap<>();
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        try {
            FilePathResponse dto = new FilePathResponse();
            String templateName = "cb-report.html";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
            File file = new File(filePath);
            pdfGenaratorUtil.createCbReportPdfSample(templateName, cbReportResponse, file);
            dto.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
            dto.setFileName(fileName);
            dtoList.add(dto);

        } catch (Exception e) {
            throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INTERNAL SERVER ERROR");
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


        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID) && cdaReportRequest.getCdaType().contains("All CDA")) {

            List<CDAReportResponse> cdaReportList = new ArrayList<>();
            CDAReportResponse cdaReportResponse = new CDAReportResponse();

            List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadOrderBySerialNumberAsc(cdaReportRequest.getMajorHead());
            List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findAllByOrderByCdaNameAsc();
            for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                cdaReportList.add(cdaReportResponse);
            }
            cdaReportResponse = new CDAReportResponse();
            cdaReportResponse.setName("Total Amount");
            cdaReportList.add(cdaReportResponse);
            allCdaData.put("object", cdaReportList);

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
                            if(cdaData.get(m).getTotalParkingAmount() == null){
                                amount = amount ;
                            }else{
                                amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                            }

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
                cdaReportList.add(cdaReportResponse);
                allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
            }
            try {

                String templateName = "cda-parking-report-revenue.html";
                File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                File file = new File(filePath);
                pdfGenaratorUtil.createCdaAllMainReport(templateName, allCdaData, cadSubReport, file);
                dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                dtoList.setFileName(fileName);


            } catch (Exception e) {
                throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INTERNAL SERVER ERROR");
            }

        }
        else if (cdaReportRequest.getCdaType().contains("All CDA")) {

            List<CDAReportResponse> cdaReportList = new ArrayList<>();
            CDAReportResponse cdaReportResponse = new CDAReportResponse();

            List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadOrderBySerialNumberAsc(cdaReportRequest.getMajorHead());
            List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findAllByOrderByCdaNameAsc();
            for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                cdaReportList.add(cdaReportResponse);
            }
            allCdaData.put("object", cdaReportList);


            for (int i = 0; i < subHeadsData.size(); i++) {
                cdaReportList = new ArrayList<>();
                cdaReportResponse = new CDAReportResponse();

                BudgetHead subHead = subHeadsData.get(i);
                cdaReportResponse.setName(subHead.getSubHeadDescr());


                CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
                List<CgUnit> unitDataList = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
                Float totalAmount = 0f;
                for (int s = 0; s < cdaParkingTotalList.size(); s++) {
                    if (cdaParkingTotalList.size() > 0) {
                        for (int k = 0; k < cdaParkingTotalList.size(); k++) {
                            List<CdaParkingTrans> cdaData = cdaParkingTransRepository.findByFinYearIdAndBudgetHeadIdAndGinNoAndIsFlagAndUnitId(cdaReportRequest.getFinancialYearId(), subHead.getBudgetCodeId(), cdaParkingTotalList.get(k).getGinNo(), "0", unitDataList.get(s).getUnit());
                            Float amount = 0f;
                            for (int m = 0; m < cdaData.size(); m++) {
                                if(cdaData.get(m).getTotalParkingAmount() == null){
                                    amount = amount ;
                                }else{
                                    amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                                }
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
                }
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(totalAmount + "");
                cdaReportList.add(cdaReportResponse);
                allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
            }


            try {

                String templateName = "cda-parking-report-revenue.html";
                File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                File file = new File(filePath);
                pdfGenaratorUtil.createCdaMainReport(templateName, allCdaData, cadSubReport, file);
                dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                dtoList.setFileName(fileName);

            } catch (Exception e) {
                throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INTERNAL SERVER ERROR");
            }

        }
        else if (cdaReportRequest.getCdaType().contains("Mumbai CDA")) {


            List<CDAReportResponse> cdaReportList = new ArrayList<>();
            CDAReportResponse cdaReportResponse = new CDAReportResponse();

            List<BudgetHead> subHeadsData = subHeadRepository.findByMajorHeadOrderBySerialNumberAsc(cdaReportRequest.getMajorHead());
            List<CdaParking> cdaParkingTotalList = cdaParkingRepository.findByCdaGroupCodeOrderByCdaNameAsc("200201");
            for (int i = 0; i < cdaParkingTotalList.size(); i++) {
                cdaReportResponse = new CDAReportResponse();
                cdaReportResponse.setName(cdaParkingTotalList.get(i).getCdaName());
                cdaReportList.add(cdaReportResponse);
            }
            cdaReportResponse = new CDAReportResponse();
            cdaReportResponse.setName("Total Amount");
            cdaReportList.add(cdaReportResponse);
            allCdaData.put("object", cdaReportList);

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
                            if(cdaData.get(m).getTotalParkingAmount() == null){
                                amount = amount ;
                            }else{
                                amount = amount + Float.parseFloat(cdaData.get(m).getTotalParkingAmount());
                            }
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
                cdaReportList.add(cdaReportResponse);
                allCdaData.put(subHead.getSubHeadDescr(), cdaReportList);
            }
            try {

                String templateName = "cda-parking-report-revenue.html";
                File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                String filePath = folder.getAbsolutePath() + "/" + fileName + ".pdf";
                File file = new File(filePath);
                pdfGenaratorUtil.createCdaMainReport(templateName, allCdaData, cadSubReport, file);
                dtoList.setPath(HelperUtils.FILEPATH + fileName + ".pdf");
                dtoList.setFileName(fileName);

            } catch (Exception e) {
                throw new SDDException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INTERNAL SERVER ERROR");
            }


        } else {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID REQUEST.PLEASE CONTACT YOUR ADMINISTRATOR.");
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
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(reportRequest.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();
        List<BudgetAllocationReport> budgetAllocationsDetalis = budgetAllocationReportRepository.findByUnitIdAndFinYearId(reportRequest.getUnitId(), reportRequest.getFinYearId());

        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        String amtType = budgetAllocationsDetalis.get(0).getAmountType();
        String names = hrData.getFullName();
        String unitName = hrData.getUnit();
        String rank = hrData.getRank();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        String htmlContent = new String();
        try {
            //htmlContent = FileUtils.readFileToString(new File("src/main/resources/templates/allocation-unit-wise-report.html"), "UTF-8");
            //htmlContent = FileUtils.readFileToString(new File(new File(".").getCanonicalPath()+"/webapps/budget/WEB-INF/classes/templates/allocation-unit-wise-report.html"), "UTF-8");
            htmlContent = "<!DOCTYPE html>\n" +
                    "<html >\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\"></meta>\n" +
                    "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"></meta>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></meta>\n" +
                    "    <title>Unit Wise Allocation Report</title>\n" +
                    "    <style>\n" +
                    "        .header {\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "            border-bottom: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "        .brtm{\n" +
                    "            border-right: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "        .table {\n" +
                    "            display: flex;\n" +
                    "        }\n" +
                    "\n" +
                    "        .float {\n" +
                    "            float: right;\n" +
                    "\n" +
                    "        }\n" +
                    "\n" +
                    "        .auth {\n" +
                    "            padding-left: 72px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table2,\n" +
                    "        .the,\n" +
                    "        .them.themed {\n" +
                    "            border: 1px solid black;\n" +
                    "            border-collapse: collapse;\n" +
                    "            padding-left: 8px;\n" +
                    "            padding-right: 8px;\n" +
                    "        }\n" +
                    "\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:90%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .sign {\n" +
                    "            float: right;\n" +
                    "            padding-right: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:27%;\n" +
                    "        }\n" +
                    "        .sign ul li{\n" +
                    "            margin-bottom: 10px;\n" +
                    "        }\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .date {\n" +
                    "            float: left;\n" +
                    "            padding-left: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:45%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<!--header-->\n" +
                    "<div class=\"wrap\">\n" +
                    "    <div class=\"header\"> <strong>UNIT WISE ALLOCATION REPORT</strong></div>\n" +
                    "    <br></br>\n" +
                    "    <table style=\"width:100%\" class=\"table2\">\n" +
                    "        <tbody>\n" +
                    "        <tr>\n" +
                    "        <tr>\n" +
                    "            <td colspan=\"2\" rowspan=\"2\" height=\"60\" align=\"center\" valign=\"middle\" class=\"the\"><b>Financial Year : ${finYear_placeholder}</b></td>\n" +
                    "        </tr>\n" +
                    "        <tr>\n" +
                    "            <td colspan=\"2\" align=\"center\" valign=\"middle\" class=\"the\"><b>Unit: ${unitName_placeholder}</b></td>\n" +
                    "        </tr>\n" +
                    "        <tr>\n" +
                    "            <td height=\"41\" align=\"left\" valign=\"middle\" class=\"the\"><b>S.L</b></td>\n" +
                    "            <td class=\"the\"><b>Sub Head</b></td>\n" +
                    "            <td class=\"the\"><b>Allocation Type</b></td>\n" +
                    "            <td class=\"the\"><b>Amount (in ${amountType_placeholder})</b></td>\n" +
                    "        </tr>\n" +
                    "        ${data_placeholder}\n" +
                    "        <tr>\n" +
                    "            <td height=\"41\" align=\"left\" valign=\"middle\" class=\"the brtm\" style=\" border-right: 1px solid transparent !important;\"><b>Total</b></td>\n" +
                    "            <td class=\"the brtm\" style=\" border-right: 1px solid transparent !important;\"></td>\n" +
                    "            <td class=\"the\"></td>\n" +
                    "            <td class=\"the\"><b>${total_placeholder} </b></td>\n" +
                    "        </tr>\n" +
                    "        </tr>\n" +
                    "        </tbody>\n" +
                    "    </table>\n" +
                    "\n" +
                    "    <div class=\"sign\">\n" +
                    "        <ul style=\"list-style: none; margin-top: 0;\">\n" +
                    "            <li>${name_placeholder}</li>\n" +
                    "            <li>${unit_placeholder}</li>\n" +
                    "            <li>${rank_placeholder}</li>\n" +
                    "        </ul>\n" +
                    "\n" +
                    "    </div>\n" +
                    "    <div class=\"date\">\n" +
                    "        Date-${date_placeholder}\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>\n";

            StringBuilder sb = new StringBuilder();
            int i = 1;
            String finyear = "";
            String unit = "";
            Double amount;
            Double amountUnit;
            Double finAmount;
            Double sum = 0.0;
            for (BudgetAllocationReport row : budgetAllocationsDetalis) {
                finyear = row.getFinYearDes();
                unit = row.getUnitName();
                amount = Double.valueOf(row.getTotalAmount());
                AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                if (amountTypeObj == null) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                }
                amountUnit = amountTypeObj.getAmount();
                finAmount = amount * amountUnit / reqAmount;
                sb.append("<tr>");
                sb.append("<td class=\"the\">").append(i).append("</td>");
                sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getSubHeadDes())).append("</td>");
                sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getAllocationType())).append("</td>");
                sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(finAmount)))).append("</td>");

                sb.append("</tr>");
                i++;
//                sum=sum+Double.parseDouble(row.getTotalAmount());
                sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
            }

            htmlContent = htmlContent.replace("${total_placeholder}", StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(sum))));
            htmlContent = htmlContent.replace("${name_placeholder}", StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}", StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}", StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}", StringEscapeUtils.escapeHtml4(formattedDateTime));
            htmlContent = htmlContent.replace("${unitName_placeholder}", StringEscapeUtils.escapeHtml4(subUnit.getDescr()));
            htmlContent = htmlContent.replace("${finYear_placeholder}", StringEscapeUtils.escapeHtml4(findyr.getFinYear()));
            htmlContent = htmlContent.replace("${amountType_placeholder}", StringEscapeUtils.escapeHtml4(amountIn));
            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());

            String filepath = HelperUtils.FILEPATH + "/allocation-unit-wise-report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "allocation-unit-wise-report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());

            // generatePdf(htmlContent, filepath);
            FilePathResponse response = new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("AllocationUnitWiseReport.pdf");
            dtoList.add(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
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
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(req.getFinYearId());

        List<BudgetAllocationReport> budgetAllocationsDetalis = budgetAllocationReportRepository.findBySubHeadIdAndFinYearId(req.getSubHeadId(), req.getFinYearId());

        if (budgetAllocationsDetalis.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        List<CgUnit> units = new ArrayList<>();
        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
        } else {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(req.getAmountTypeId());
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        String names = hrData.getFullName();
        String unitName = hrData.getUnit();
        String rank = hrData.getRank();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        String htmlContent = new String();
        try {
            //htmlContent = FileUtils.readFileToString(new File("src\\main\\resources\\templates\\subhead-wise-allocation-report.html"), "UTF-8");
            //htmlContent = FileUtils.readFileToString(new File(new File(".").getCanonicalPath()+"/webapps/budget/WEB-INF/classes/templates/subhead-wise-allocation-report.html"), "UTF-8");
            htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\"></meta>\n" +
                    "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"></meta>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></meta>\n" +
                    "    <title>Sub Head Allocation Report</title>\n" +
                    "    <style>\n" +
                    "        .header {\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "             border-bottom: 1px solid transparent !important;\n" +
                    "         }\n" +
                    "        .brtm{\n" +
                    "            border-right: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "        .table {\n" +
                    "            display: flex;\n" +
                    "        }\n" +
                    "\n" +
                    "        .float {\n" +
                    "            float: right;\n" +
                    "\n" +
                    "        }\n" +
                    "\n" +
                    "        .auth {\n" +
                    "            padding-left: 72px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table2,\n" +
                    "        .the,\n" +
                    "        .them.themed {\n" +
                    "            border: 1px solid black;\n" +
                    "            border-collapse: collapse;\n" +
                    "            padding-left: 8px;\n" +
                    "            padding-right: 8px;\n" +
                    "        }\n" +
                    "\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .sign {\n" +
                    "            float: right;\n" +
                    "            padding-right: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:27%;\n" +
                    "        }\n" +
                    "        .sign ul li{\n" +
                    "            margin-bottom: 10px;\n" +
                    "        }\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .date {\n" +
                    "            float: left;\n" +
                    "            padding-left: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:45%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "\n" +
                    "</head>\n" +
                    "\n" +
                    "\n" +
                    "<body>\n" +
                    "    <!--header-->\n" +
                    "    <div class=\"wrap\">\n" +
                    "        <div class=\"header\"> <strong>SUB HEAD WISE ALLOCATION REPORT</strong></div>\n" +
                    "\n" +
                    "        <table style=\"width:100%\" class=\"table2\">\n" +
                    "            <tbody>\n" +
                    "\n" +
                    "                    <tr>\n" +
                    "                        <td colspan=\"2\" rowspan=\"2\" height=\"60\" align=\"center\" valign=\"middle\" class=\"the\"><b>Financial Year : ${finYear_placeholder}</b></td>\n" +
                    "                        </tr>\n" +
                    "                    <tr>\n" +
                    "                        <td colspan=\"2\" align=\"center\" valign=\"middle\" class=\"the\"><b>Subhead:  ${subHd_placeholder}</b></td>\n" +
                    "                        </tr>\n" +
                    "                    <tr>\n" +
                    "                        <td style=\"border-right: 2px solid #000000\" height=\"41\" align=\"left\" valign=\"middle\" class=\"the\"><b>S.L</b></td>\n" +
                    "                        <td class=\"the\"><b>Unit</b></td>\n" +
                    "                        <td class=\"the\"><b>Allocation Type</b></td>\n" +
                    "                        <td class=\"the\"><b>Amount (in ${amountType_placeholder})</b></td>\n" +
                    "                    </tr>\n" +
                    "                ${data_placeholder}\n" +
                    "                    <tr>\n" +
                    "                        <td height=\"41\" align=\"left\" valign=\"middle\" class=\"the brtm\" style=\" border-right: 1px solid transparent !important;\"><b>Total</b></td>\n" +
                    "                        <td class=\"the brtm\" style=\" border-right: 1px solid transparent !important;\"></td>\n" +
                    "                        <td class=\"the\"></td>\n" +
                    "                        <td class=\"the\"><b> ${total_placeholder}</b></td>\n" +
                    "                    </tr>\n" +
                    "\n" +
                    "\n" +
                    "            </tbody>\n" +
                    "            \n" +
                    "\n" +
                    "        </table>\n" +
                    "        <div class=\"sign\">\n" +
                    "            <ul style=\"list-style: none; margin-top: 0;\">\n" +
                    "                <li>${name_placeholder}</li>\n" +
                    "                <li>${unit_placeholder}</li>\n" +
                    "                <li>${rank_placeholder}</li>\n" +
                    "            </ul>\n" +
                    "\n" +
                    "        </div>\n" +
                    "        <div class=\"date\">\n" +
                    "            Date-${date_placeholder}\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "\n" +
                    "</body>\n" +
                    "\n" +
                    "</html>";

            StringBuilder sb = new StringBuilder();
            int i = 1;
            String finyear = "";
            String unit = "";
            Double amount;
            Double amountUnit;
            Double finAmount;
            float sum = 0;
            for (BudgetAllocationReport row : budgetAllocationsDetalis) {
                for (Integer k = 0; k < units.size(); k++) {
                    if (units.get(k).getUnit().equalsIgnoreCase(row.getUnitId())) {
                        finyear = row.getFinYearDes();
                        unit = row.getSubHeadDes();
                        amount = Double.valueOf(row.getTotalAmount());
                        AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                        if (amountTypeObj == null) {
                            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                            }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                        }
                        amountUnit = amountTypeObj.getAmount();

                        finAmount = amount * amountUnit / reqAmount;

                        sb.append("<tr>");
                        sb.append("<td class=\"the\">").append(i).append("</td>");
                        sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getUnitName())).append("</td>");
                        sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getAllocationType())).append("</td>");
                        sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(finAmount)))).append("</td>");
                        sb.append("</tr>");
                        i++;
                        sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                    }
                }
            }
            htmlContent = htmlContent.replace("${total_placeholder}", StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(sum))));
            htmlContent = htmlContent.replace("${name_placeholder}", StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}", StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}", StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}", StringEscapeUtils.escapeHtml4(formattedDateTime));
            htmlContent = htmlContent.replace("${finYear_placeholder}", StringEscapeUtils.escapeHtml4(findyr.getFinYear()));
            htmlContent = htmlContent.replace("${subHd_placeholder}", StringEscapeUtils.escapeHtml4(budgetAllocationsDetalis.get(0).getSubHeadDes()));
            htmlContent = htmlContent.replace("${amountType_placeholder}", StringEscapeUtils.escapeHtml4(amountIn));
            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());
            String filepath = HelperUtils.FILEPATH + "/subhead-wise-allocation-report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "subhead-wise-allocation-report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());
            // generatePdf(htmlContent, filepath);
            FilePathResponse response = new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("SubheadWiseAllocationReport.pdf");
            dtoList.add(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /*     return null;*/

        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
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
        List<String> rowData = budgetAllocationReportRepository.findSubHead(finYearId, allocationType);
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
        List<CgUnit> units = new ArrayList<>();
        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
        } else {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        String amtType = "";
        String names = hrData.getFullName();
        String unitName = hrData.getUnit();
        String rank = hrData.getRank();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        String htmlContent = new String();
        try {
            //htmlContent = FileUtils.readFileToString(new File("src/main/resources/templates/be-allocation-report.html"), "UTF-8");
            //htmlContent = FileUtils.readFileToString(new File(new File(".").getCanonicalPath()+"/webapps/budget/WEB-INF/classes/templates/be-allocation-report"), "UTF-8");
            htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <title>Report</title>\n" +
                    "    <meta charset=\"utf-8\"></meta>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></meta>\n" +
                    "\n" +
                    "    <style>\n" +
                    "        .header {\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "            border-bottom: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "        .wrapper{\n" +
                    "            width: 70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "\n" +
                    "        :root {\n" +
                    "            --bg-table-stripe: #f6f6f5;\n" +
                    "            --b-table: #e3e3e2;\n" +
                    "            --caption: #242423;\n" +
                    "        }\n" +
                    "\n" +
                    "        table {\n" +
                    "            background-color: transparent;\n" +
                    "            border-collapse:collapse;\n" +
                    "            font-family: Arial, Helvetica, sans-serif\n" +
                    "        }\n" +
                    "\n" +
                    "        th {\n" +
                    "            text-align:left;\n" +
                    "            border: 1px solid #242423 ;\n" +
                    "        }\n" +
                    "        td{\n" +
                    "            border: 1px solid #242423 ;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-center {\n" +
                    "            text-align: center!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-left {\n" +
                    "            text-align: left!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-right {\n" +
                    "            text-align: right!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table caption {\n" +
                    "            color: var(--caption);\n" +
                    "            font-size: 1.13em;\n" +
                    "            font-weight: 700;\n" +
                    "            padding-bottom: .56rem\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table thead {\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody {\n" +
                    "            border-bottom: 1px solid var(--b-table);\n" +
                    "            border-top: 1px solid var(--b-table);\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tfoot {\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table td, .dcf-table th {\n" +
                    "            padding-right: 1.78em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered, .dcf-table-bordered td, .dcf-table-bordered th {\n" +
                    "            border: 1px solid var(--b-table)\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered td, .dcf-table-bordered th, .dcf-table-striped td, .dcf-table-striped th {\n" +
                    "            padding-left: 1em;\n" +
                    "            padding-right: 1em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered tr:not(:last-child), .dcf-table-striped tr:not(:last-child) {\n" +
                    "            border-bottom: 1px solid var(--b-table)\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-striped tbody tr:nth-of-type(2n) {\n" +
                    "            background-color: transparent;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table thead td, .dcf-table thead th {\n" +
                    "            padding-bottom: .75em;\n" +
                    "            vertical-align: bottom\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody td, .dcf-table tbody th, .dcf-table tfoot td, .dcf-table tfoot th {\n" +
                    "            padding-top: .75em;\n" +
                    "            vertical-align: top\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody td, .dcf-table tbody th {\n" +
                    "            padding-bottom: .75em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered thead th {\n" +
                    "            padding-top: 1.33em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-wrapper-table-scroll {\n" +
                    "            overflow-x: auto;\n" +
                    "            -webkit-overflow-scrolling: touch;\n" +
                    "            left: 50%;\n" +
                    "            margin-left: -50vw;\n" +
                    "            margin-right: -50vw;\n" +
                    "            padding-bottom: 1em;\n" +
                    "            position: relative;\n" +
                    "            right: 50%;\n" +
                    "            width: 100vw\n" +
                    "        }\n" +
                    "\n" +
                    "        @media only screen and (max-width:42.09em) {\n" +
                    "            .dcf-table-responsive thead {\n" +
                    "                clip: rect(0 0 0 0);\n" +
                    "                -webkit-clip-path: inset(50%);\n" +
                    "                clip-path: inset(50%);\n" +
                    "                height: 1px;\n" +
                    "                overflow: hidden;\n" +
                    "                position: absolute;\n" +
                    "                width: 1px;\n" +
                    "                white-space: nowrap\n" +
                    "            }\n" +
                    "            .dcf-table-responsive tr {\n" +
                    "                display: block\n" +
                    "            }\n" +
                    "            .dcf-table-responsive td {\n" +
                    "                -webkit-column-gap: 3.16vw;\n" +
                    "                -moz-column-gap: 3.16vw;\n" +
                    "                column-gap: 3.16vw;\n" +
                    "                display: grid;\n" +
                    "                grid-template-columns: 1fr 2fr;\n" +
                    "                text-align: left!important\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered, .dcf-table-responsive.dcf-table-bordered thead th {\n" +
                    "                border-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered tbody td {\n" +
                    "                border-top-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered) tbody tr {\n" +
                    "                padding-bottom: .75em\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered) tbody td {\n" +
                    "                padding-bottom: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered):not(.dcf-table-striped) tbody td {\n" +
                    "                padding-right: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered tbody tr:last-child td:last-child {\n" +
                    "                border-bottom-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive tbody td:before {\n" +
                    "                content: attr(data-label);\n" +
                    "                float: left;\n" +
                    "                font-weight: 700;\n" +
                    "                padding-right: 1.78em\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-overflow-x-auto {\n" +
                    "            overflow-x: auto!important;\n" +
                    "            -webkit-overflow-scrolling: touch\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-w-100\\% {\n" +
                    "            width: 100%!important;\n" +
                    "        }\n" +
                    "        .sign {\n" +
                    "            float: right;\n" +
                    "            padding-right: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:27%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .date {\n" +
                    "            float: left;\n" +
                    "            padding-left: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:45%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .bold{\n" +
                    "            font-weight: bold;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "            border-bottom: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"wrapper\">\n" +
                    "    <div class=\"header\"> <strong>${allocationType_placeholder} ALLOCATION REPORT</strong></div>\n" +
                    "    <br></br>\n" +
                    "    <table class=\"dcf-table dcf-table-responsive dcf-table-bordered dcf-table-striped dcf-w-100%\">\n" +
                    "        <thead>\n" +
                    "        <tr>\n" +
                    "            <th scope=\"col\">REVENUE OBJECT HEAD </th>\n" +
                    "            <th class=\"dcf-txt-center\" scope=\"col\"> UNIT </th>\n" +
                    "            <th class=\"dcf-txt-center\" scope=\"col\">${allocationType_placeholder}  ${finYear_placeholder} ALLOCATION(IN (in ${amountType_placeholder}))</th>\n" +
                    "        </tr>\n" +
                    "        </thead>\n" +
                    "        <tbody>\n" +
                    "        ${data_placeholder}\n" +
                    "\n" +
                    "        </tbody>\n" +
                    "    </table>\n" +
                    "    <div>\n" +
                    "       \n" +
                    "    </div>\n" +
                    "\n" +
                    "    <div class=\"sign\">\n" +
                    "        <ul style=\"list-style: none; margin-top: 0;\">\n" +
                    "            <li>${name_placeholder}</li>\n" +
                    "            <li>${unit_placeholder}</li>\n" +
                    "            <li>${rank_placeholder}</li>\n" +
                    "        </ul>\n" +
                    "    </div>\n" +
                    "    <div class=\"date\">\n" +
                    "        Date-${date_placeholder}\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>\n";

            StringBuilder sb = new StringBuilder();
            int i = 1;
            String finyear = "";
            String unit = "";
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocationReport> reportDetails = budgetAllocationReportRepository.findBySubHeadIdAndAllocationType(subHeadId, allocationType);
                if (reportDetails.size() <= 0) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "RECORD NOT FOUND OR EMPTY", HttpStatus.OK.value());
                }
                int count = 0;
                float sum = 0;
                Double amount;
                Double amountUnit;
                Double finAmount;
                amtType = reportDetails.get(0).getAmountType();

                for (BudgetAllocationReport row : reportDetails) {

                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getUnitId())) {
                            finyear = row.getFinYearDes();
                            amount = Double.valueOf(row.getTotalAmount());
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            sb.append("<tr>");
                            if (count == 0)
                                sb.append("<th scope=\"row\" >").append(StringEscapeUtils.escapeHtml4(row.getSubHeadDes())).append("</th>");
                            else
                                sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getUnitName())).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(finAmount)))).append("</td>");
                            sb.append("</tr>");
                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                        }
                    }

                }
                if (count != 0) {
                    sb.append("<tr>");
                    sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                    sb.append("<td class=\"the bold\">TOTAL</td>");
                    sb.append("<td class=\"the bold\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(sum)))).append("</td>");
                    sb.append("</tr>");
                    count = 0;
                    //print sum
                }

            }
            if (sb.toString().isEmpty()) {
                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                }, "RECORD NOT FOUND", HttpStatus.OK.value());
            }

            htmlContent = htmlContent.replace("${name_placeholder}", StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}", StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}", StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}", StringEscapeUtils.escapeHtml4(formattedDateTime));
            htmlContent = htmlContent.replace("${finYear_placeholder}", StringEscapeUtils.escapeHtml4(findyr.getFinYear()));
            htmlContent = htmlContent.replace("${amountType_placeholder}", StringEscapeUtils.escapeHtml4(amountIn));
            htmlContent = htmlContent.replace("${allocationType_placeholder}", StringEscapeUtils.escapeHtml4(allocationType));

            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());
            String filepath = HelperUtils.FILEPATH + "/be-allocation-report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "be-allocation-report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());
            //generatePdf(htmlContent, filepath);
            FilePathResponse response = new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("BeAllocationReport.pdf");
            dtoList.add(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
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
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        AllocationType allockData = allocationRepository.findByAllocTypeId(allocationType);
        String allocType = allockData.getAllocType();
        BudgetFinancialYear findyr = budgetFinancialYearRepository.findBySerialNo(finYearId);


        List<String> rowData = budgetAllocationReportRepository.findSubHead(finYearId, allocType);
        if (rowData.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "RECORD NOT FOUND", HttpStatus.OK.value());
        }
        String amtType = "";
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
        List<CgUnit> units = new ArrayList<>();
        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
        } else {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }

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
                    "\t\t\t\t<td class=\"dcf-txt-center brtm bold\">Existing Amount (in ${amountType_placeholder})</td>\n" +
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
            String finyear = "";
            String unit = "";
            Double amount;
            Double amountUnit;
            Double finAmount;
            Double revisedAmount;
            Double reAmount;
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocationReport> reportDetails = budgetAllocationReportRepository.findBySubHeadIdAndAllocationType(subHeadId, allocType);
                if (reportDetails.size() <= 0) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "RECORD NOT FOUND OR EMPTY", HttpStatus.OK.value());
                }

                String subHead = reportDetails.get(0).getSubHeadDes();
                int count = 0;
                sb.append("<tr>");
                sb.append("<td class=\"the\" ><b>").append(StringEscapeUtils.escapeHtml4(subHead)).append("</b></td>");
                sb.append("<td class=\"the\"></td>");
                sb.append("<td class=\"the\"></td>");
                sb.append("<td class=\"the\"></td>");
                sb.append("</tr>");
                float sumExisting = 0;
                float sumRE = 0;
                float total = 0;
                for (BudgetAllocationReport row : reportDetails) {
                    finyear = row.getFinYearDes();
                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getUnitId())) {
                            amount = Double.valueOf(row.getTotalAmount());
                            revisedAmount = Double.valueOf(row.getRevisedAmount());
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            reAmount = revisedAmount * amountUnit / reqAmount;
                            sb.append("<tr>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getUnitName())).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(finAmount)))).append("</td>");
                            if (reAmount <= 0)
                                sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(reAmount)))).append("</td>");
                            else
                                sb.append("<td class=\"the\"> (+)").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(reAmount)))).append("</td>");
                            sb.append("<td class=\"the\">").append(String.format("%1$0,1.4f", (new BigDecimal((Float.parseFloat(Double.toString(finAmount)) + Float.parseFloat(Double.toString(reAmount))))))).append("</td>");
                            sb.append("</tr>");
                            sumExisting += Float.parseFloat(new BigDecimal(Double.toString(finAmount)).toPlainString());
                            sumRE += Float.parseFloat(new BigDecimal(Double.toString(reAmount)).toPlainString());
                        }
                    }
                }
                total = sumExisting + sumRE;
                sb.append("<tr>");
                sb.append("<td class=\"the bold\">").append("Total").append("</td>");
                sb.append("<td class=\"the bold\">").append(String.format("%1$0,1.4f", new BigDecimal(sumExisting))).append("</td>");
                sb.append("<td class=\"the bold\">").append(String.format("%1$0,1.4f", new BigDecimal(sumRE))).append("</td>");
                sb.append("<td class=\"the bold\">").append(String.format("%1$0,1.4f", new BigDecimal(total))).append("</td>");
                sb.append("</tr>");
            }
            if (sb.toString().isEmpty()) {
                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                }, "RECORD NOT FOUND", HttpStatus.OK.value());
            }
            htmlContent = htmlContent.replace("${name_placeholder}", StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}", StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}", StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}", StringEscapeUtils.escapeHtml4(formattedDateTime));
            htmlContent = htmlContent.replace("${finYear_placeholder}", StringEscapeUtils.escapeHtml4(findyr.getFinYear()));
            htmlContent = htmlContent.replace("${amountType_placeholder}", StringEscapeUtils.escapeHtml4(amountIn));
            htmlContent = htmlContent.replace("${allocaType_placeholder}", StringEscapeUtils.escapeHtml4(allocType));


            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());
            String filepath = HelperUtils.FILEPATH + "/re-allocation-report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "re-allocation-report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());
            //generatePdf(htmlContent, filepath);
            FilePathResponse response = new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("ReAllocation-report.pdf");

            dtoList.add(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }

    @Override
    public ApiResponse<List<FilePathResponse>> getBEREAllocationReport(String finYearId, String allocationType, String amountTypeId) {

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
        List<String> rowData = budgetAllocationReportRepository.findSubHead(finYearId, allocationType);
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
        List<CgUnit> units = new ArrayList<>();
        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
        } else {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
        }
        if (units.size() <= 0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj = amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount = amountObj.getAmount();
        String amountIn = amountObj.getAmountType();

        String amtType = "";
        String names = hrData.getFullName();
        String unitName = hrData.getUnit();
        String rank = hrData.getRank();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        String htmlContent = new String();
        try {
            //htmlContent = FileUtils.readFileToString(new File("src/main/resources/templates/bere-allocation-report.html"), "UTF-8");
            //htmlContent = FileUtils.readFileToString(new File(new File(".").getCanonicalPath()+"/webapps/budget/WEB-INF/classes/templates/bere-allocation-report"), "UTF-8");
            htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <title>Report</title>\n" +
                    "    <meta charset=\"utf-8\"></meta>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></meta>\n" +
                    "\n" +
                    "    <style>\n" +
                    "        .header {\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "            border-bottom: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "        .wrapper{\n" +
                    "            width: 70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "\n" +
                    "        :root {\n" +
                    "            --bg-table-stripe: #f6f6f5;\n" +
                    "            --b-table: #e3e3e2;\n" +
                    "            --caption: #242423;\n" +
                    "        }\n" +
                    "\n" +
                    "        table {\n" +
                    "            background-color: transparent;\n" +
                    "            border-collapse:collapse;\n" +
                    "            font-family: Arial, Helvetica, sans-serif\n" +
                    "        }\n" +
                    "\n" +
                    "        th {\n" +
                    "            text-align:left;\n" +
                    "            border: 1px solid #242423 ;\n" +
                    "        }\n" +
                    "        td{\n" +
                    "            border: 1px solid #242423 ;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-center {\n" +
                    "            text-align: center!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-left {\n" +
                    "            text-align: left!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-right {\n" +
                    "            text-align: right!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table caption {\n" +
                    "            color: var(--caption);\n" +
                    "            font-size: 1.13em;\n" +
                    "            font-weight: 700;\n" +
                    "            padding-bottom: .56rem\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table thead {\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody {\n" +
                    "            border-bottom: 1px solid var(--b-table);\n" +
                    "            border-top: 1px solid var(--b-table);\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tfoot {\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table td, .dcf-table th {\n" +
                    "            padding-right: 1.78em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered, .dcf-table-bordered td, .dcf-table-bordered th {\n" +
                    "            border: 1px solid var(--b-table)\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered td, .dcf-table-bordered th, .dcf-table-striped td, .dcf-table-striped th {\n" +
                    "            padding-left: 1em;\n" +
                    "            padding-right: 1em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered tr:not(:last-child), .dcf-table-striped tr:not(:last-child) {\n" +
                    "            border-bottom: 1px solid var(--b-table)\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-striped tbody tr:nth-of-type(2n) {\n" +
                    "            background-color: transparent;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table thead td, .dcf-table thead th {\n" +
                    "            padding-bottom: .75em;\n" +
                    "            vertical-align: bottom\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody td, .dcf-table tbody th, .dcf-table tfoot td, .dcf-table tfoot th {\n" +
                    "            padding-top: .75em;\n" +
                    "            vertical-align: top\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody td, .dcf-table tbody th {\n" +
                    "            padding-bottom: .75em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered thead th {\n" +
                    "            padding-top: 1.33em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-wrapper-table-scroll {\n" +
                    "            overflow-x: auto;\n" +
                    "            -webkit-overflow-scrolling: touch;\n" +
                    "            left: 50%;\n" +
                    "            margin-left: -50vw;\n" +
                    "            margin-right: -50vw;\n" +
                    "            padding-bottom: 1em;\n" +
                    "            position: relative;\n" +
                    "            right: 50%;\n" +
                    "            width: 100vw\n" +
                    "        }\n" +
                    "\n" +
                    "        @media only screen and (max-width:42.09em) {\n" +
                    "            .dcf-table-responsive thead {\n" +
                    "                clip: rect(0 0 0 0);\n" +
                    "                -webkit-clip-path: inset(50%);\n" +
                    "                clip-path: inset(50%);\n" +
                    "                height: 1px;\n" +
                    "                overflow: hidden;\n" +
                    "                position: absolute;\n" +
                    "                width: 1px;\n" +
                    "                white-space: nowrap\n" +
                    "            }\n" +
                    "            .dcf-table-responsive tr {\n" +
                    "                display: block\n" +
                    "            }\n" +
                    "            .dcf-table-responsive td {\n" +
                    "                -webkit-column-gap: 3.16vw;\n" +
                    "                -moz-column-gap: 3.16vw;\n" +
                    "                column-gap: 3.16vw;\n" +
                    "                display: grid;\n" +
                    "                grid-template-columns: 1fr 2fr;\n" +
                    "                text-align: left!important\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered, .dcf-table-responsive.dcf-table-bordered thead th {\n" +
                    "                border-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered tbody td {\n" +
                    "                border-top-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered) tbody tr {\n" +
                    "                padding-bottom: .75em\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered) tbody td {\n" +
                    "                padding-bottom: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered):not(.dcf-table-striped) tbody td {\n" +
                    "                padding-right: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered tbody tr:last-child td:last-child {\n" +
                    "                border-bottom-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive tbody td:before {\n" +
                    "                content: attr(data-label);\n" +
                    "                float: left;\n" +
                    "                font-weight: 700;\n" +
                    "                padding-right: 1.78em\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-overflow-x-auto {\n" +
                    "            overflow-x: auto!important;\n" +
                    "            -webkit-overflow-scrolling: touch\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-w-100\\% {\n" +
                    "            width: 100%!important;\n" +
                    "        }\n" +
                    "        .sign {\n" +
                    "            float: right;\n" +
                    "            padding-right: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:27%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .date {\n" +
                    "            float: left;\n" +
                    "            padding-left: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:45%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .count {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 200px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .table3 {\n" +
                    "            padding-top: 55px;\n" +
                    "        }\n" +
                    "\n" +
                    "        .tab {\n" +
                    "            padding-left: 85px;\n" +
                    "        }\n" +
                    "        .table4{\n" +
                    "            padding-top: 15px;\n" +
                    "        }\n" +
                    "        .wrap{\n" +
                    "            width:70%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "        .bold{\n" +
                    "            font-weight: bold;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "            border-bottom: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"wrapper\">\n" +
                    "    <div class=\"header\"> <strong>BE-RE ALLOCATION REPORT</strong></div>\n" +
                    "    <br></br>\n" +
                    "    <table class=\"dcf-table dcf-table-responsive dcf-table-bordered dcf-table-striped dcf-w-100%\">\n" +
                    "        <thead>\n" +
                    "        <tr>\n" +
                    "            <th scope=\"col\">REVENUE OBJECT HEAD </th>\n" +
                    "            <th class=\"dcf-txt-center\" scope=\"col\"> UNIT </th>\n" +
                    "            <th class=\"dcf-txt-center\" scope=\"col\">BE ${finYear_placeholder} ALLOCATION(IN (in ${amountType_placeholder}))</th>\n" +
                    "            <th class=\"dcf-txt-center\" scope=\"col\">RE ${finYear_placeholder} ALLOCATION(IN (in ${amountType_placeholder}))</th>\n" +
                    "\n" +
                    "        </tr>\n" +
                    "        </thead>\n" +
                    "        <tbody>\n" +
                    "        ${data_placeholder}\n" +
                    "\n" +
                    "        </tbody>\n" +
                    "    </table>\n" +
                    "    <div>\n" +
                    "\n" +
                    "    </div>\n" +
                    "\n" +
                    "    <div class=\"sign\">\n" +
                    "        <ul style=\"list-style: none; margin-top: 0;\">\n" +
                    "            <li>${name_placeholder}</li>\n" +
                    "            <li>${unit_placeholder}</li>\n" +
                    "            <li>${rank_placeholder}</li>\n" +
                    "        </ul>\n" +
                    "    </div>\n" +
                    "    <div class=\"date\">\n" +
                    "        Date-${date_placeholder}\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>\n";

            StringBuilder sb = new StringBuilder();
            int i = 1;
            String finyear = "";
            String unit = "";
            for (String val : rowData) {
                String subHeadId = val;
                List<BudgetAllocationReport> reportDetails = budgetAllocationReportRepository.findBySubHeadIdAndAllocationType(subHeadId, allocationType);
                if (reportDetails.size() <= 0) {
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                    }, "RECORD NOT FOUND OR EMPTY", HttpStatus.OK.value());
                }
                int count = 0;
                float sum = 0;
                Double amount;
                Double amountUnit;
                Double finAmount;
                float reSum = 0;
                Double reAmountUnit = 0.0;
                Double reFinalAmount;

                for (BudgetAllocationReport row : reportDetails) {

                    for (Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getUnitId())) {
                            finyear = row.getFinYearDes();
                            amount = Double.valueOf(row.getTotalAmount());
                            String unitIds = row.getUnitId();
                            String allocType = "RE";
                            AmountUnit amountTypeObj = amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if (amountTypeObj == null) {
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                                }, "AMOUNT TYPE NOT FOUND FROM DB", HttpStatus.OK.value());
                            }
                            amountUnit = amountTypeObj.getAmount();
                            finAmount = amount * amountUnit / reqAmount;
                            List<BudgetAllocationReport> reData = budgetAllocationReportRepository.findByUnitIdAndFinYearIdAndAllocationTypeAndSubHeadId(unitIds, finYearId, allocType, subHeadId);
                            if (reData.size() <= 0) {
                                reFinalAmount = 0.0000;
                            } else {
                                Double reTotalAmount = Double.valueOf(reData.get(0).getTotalAmount());
                                AmountUnit amountTypeRe = amountUnitRepository.findByAmountTypeId(reData.get(0).getAmountType());
                                reAmountUnit = amountTypeRe.getAmount();
                                reFinalAmount = reTotalAmount * reAmountUnit / reqAmount;
                            }
                            sb.append("<tr>");
                            if (count == 0)
                                sb.append("<th scope=\"row\" >").append(StringEscapeUtils.escapeHtml4(row.getSubHeadDes())).append("</th>");
                            else
                                sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getUnitName())).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(finAmount)))).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(reFinalAmount)))).append("</td>");
                            sb.append("</tr>");
                            count++;
                            sum += Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            reSum += Float.parseFloat(new BigDecimal(reFinalAmount).toPlainString());

                        }
                    }

                }
                if (count != 0) {
                    sb.append("<tr>");
                    sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                    sb.append("<td class=\"the bold\">TOTAL</td>");
                    sb.append("<td class=\"the bold\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(sum)))).append("</td>");
                    sb.append("<td class=\"the bold\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f", new BigDecimal(reSum)))).append("</td>");
                    sb.append("</tr>");
                    count = 0;
                    //print sum
                }

            }
            if (sb.toString().isEmpty()) {
                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
                }, "RECORD NOT FOUND", HttpStatus.OK.value());
            }

            htmlContent = htmlContent.replace("${name_placeholder}", StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}", StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}", StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}", StringEscapeUtils.escapeHtml4(formattedDateTime));
            htmlContent = htmlContent.replace("${finYear_placeholder}", StringEscapeUtils.escapeHtml4(findyr.getFinYear()));
            htmlContent = htmlContent.replace("${amountType_placeholder}", StringEscapeUtils.escapeHtml4(amountIn));

            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());
            String filepath = HelperUtils.FILEPATH + "/be-allocation-report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "bere-allocation-report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());
            //generatePdf(htmlContent, filepath);
            FilePathResponse response = new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("BeReAllocationReport.pdf");
            dtoList.add(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtils.createSuccessResponse(dtoList, new TypeReference<List<FilePathResponse>>() {
        });
    }


    @Override
    public ApiResponse<List<FilePathResponse>> getMainBEAllocationReport(String finYearId,String allocationType, String amountTypeId) {

        String token = headerUtils.getTokeFromHeader();
        TokenParseData currentLoggedInUser = headerUtils.getUserCurrentDetails(token);
        HrData hrData = hrDataRepository.findByUserNameAndIsActive(currentLoggedInUser.getPreferred_username(),"1");
        List<FilePathResponse> dtoList = new ArrayList<FilePathResponse>();

        if (hrData == null) {
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "INVALID TOKEN.LOGIN AGAIN");
        }

        if(finYearId==null || finYearId.isEmpty() ){
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
            },"FINANCIAL YEAR CAN NOT BE NULL OR EMPTY",HttpStatus.OK.value());
        }
        if(allocationType==null || allocationType.isEmpty()){
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
            },"ALLOCATION TYPE CAN NOT BE NULL OR EMPTY",HttpStatus.OK.value());
        }

        if(amountTypeId==null || amountTypeId.isEmpty()){
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
            },"AMOUNT TYPE CAN NOT BE NULL OR EMPTY",HttpStatus.OK.value());
        }
        List<String> rowData = budgetAllocationReportRepository.findSubHead(finYearId, allocationType);
        if(rowData.size()<=0){
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
            },"RECORD NOT FOUND",HttpStatus.OK.value());
        }
        BudgetFinancialYear findyr=budgetFinancialYearRepository.findBySerialNo(finYearId);

        CgUnit cgUnit = cgUnitRepository.findByUnit(hrData.getUnitId());
        if (cgUnit == null) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
            }, "USER UNIT IS INVALID.PLEASE CHECK", HttpStatus.OK.value());
        }
        List<CgUnit> units = new ArrayList<>();
        if (hrData.getUnitId().equalsIgnoreCase(HelperUtils.HEADUNITID)) {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getSubUnit());
        } else {
            units = cgUnitRepository.findBySubUnitOrderByDescrAsc(cgUnit.getUnit());
        }
        if (units.size() <=0) {
            return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
            }, "UNIT NOT FOUND", HttpStatus.OK.value());
        }
        AmountUnit amountObj=amountUnitRepository.findByAmountTypeId(amountTypeId);
        Double reqAmount=amountObj.getAmount();
        String amountIn=amountObj.getAmountType();

        String amtType= "";
        String names = hrData.getFullName();
        String unitName =hrData.getUnit();
        String rank =hrData.getRank();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        String htmlContent=new String();
        try {
            //htmlContent = FileUtils.readFileToString(new File("src/main/resources/templates/be-allocation-report.html"), "UTF-8");
            //htmlContent = FileUtils.readFileToString(new File(new File(".").getCanonicalPath()+"/webapps/budget/WEB-INF/classes/templates/be-allocation-report"), "UTF-8");
            htmlContent="\n" +
                    "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <title>Coast Guard Budget</title>\n" +
                    "    <meta charset=\"utf-8\"></meta>\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></meta>\n" +
                    "\n" +
                    "    <style>\n" +
                    "        @page  {\n" +
                    "            size: A4 landscape;\n" +
                    "        }\n" +
                    "        .bbtm{\n" +
                    "            border-bottom: 1px solid transparent !important;\n" +
                    "        }\n" +
                    "        .wrapper{\n" +
                    "            width: 95%;\n" +
                    "            margin: 100px auto;\n" +
                    "        }\n" +
                    "\n" +
                    "        :root {\n" +
                    "            --bg-table-stripe: #f6f6f5;\n" +
                    "            --b-table: #e3e3e2;\n" +
                    "            --caption: #242423;\n" +
                    "        }\n" +
                    "\n" +
                    "        table {\n" +
                    "            background-color: transparent;\n" +
                    "            border-collapse:collapse;\n" +
                    "            font-family: Arial, Helvetica, sans-serif\n" +
                    "        }\n" +
                    "        .bold{\n" +
                    "            font-weight: 900;\n" +
                    "        }\n" +
                    "        th {\n" +
                    "            text-align:left;\n" +
                    "        }\n" +
                    "\n" +
                    "        th {\n" +
                    "            text-align:left;\n" +
                    "            border: 1px solid #242423 ;\n" +
                    "        }\n" +
                    "        td{\n" +
                    "            border: 1px solid #242423 ;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-center {\n" +
                    "            text-align: center!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-left {\n" +
                    "            text-align: left!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-txt-right {\n" +
                    "            text-align: right!important\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table caption {\n" +
                    "            color: var(--caption);\n" +
                    "            font-size: 1.13em;\n" +
                    "            font-weight: 700;\n" +
                    "            padding-bottom: .56rem\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table thead {\n" +
                    "            font-size: .75em;\n" +
                    "            text-decoration: underline;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody {\n" +
                    "            border-bottom: 1px solid var(--b-table);\n" +
                    "            border-top: 1px solid var(--b-table);\n" +
                    "            font-size: .75em;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tfoot {\n" +
                    "            font-size: .84em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table td, .dcf-table th {\n" +
                    "            padding-right: 1.78em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered, .dcf-table-bordered td, .dcf-table-bordered th {\n" +
                    "            border: 1px solid var(--b-table)\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered td, .dcf-table-bordered th, .dcf-table-striped td, .dcf-table-striped th {\n" +
                    "            padding-left: 1em;\n" +
                    "            padding-right: 1em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered tr:not(:last-child), .dcf-table-striped tr:not(:last-child) {\n" +
                    "            border-bottom: 1px solid var(--b-table)\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-striped tbody tr:nth-of-type(2n) {\n" +
                    "            background-color: transparent;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table thead td, .dcf-table thead th {\n" +
                    "            padding-bottom: .75em;\n" +
                    "            vertical-align: middle;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody td, .dcf-table tbody th, .dcf-table tfoot td, .dcf-table tfoot th {\n" +
                    "            padding-top: .75em;\n" +
                    "            vertical-align: middle;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table tbody td, .dcf-table tbody th {\n" +
                    "            padding-bottom: .75em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-table-bordered thead th {\n" +
                    "            padding-top: 0.75em\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-wrapper-table-scroll {\n" +
                    "            overflow-x: auto;\n" +
                    "            -webkit-overflow-scrolling: touch;\n" +
                    "            left: 50%;\n" +
                    "            margin-left: -50vw;\n" +
                    "            margin-right: -50vw;\n" +
                    "            padding-bottom: 1em;\n" +
                    "            position: relative;\n" +
                    "            right: 50%;\n" +
                    "            width: 100vw\n" +
                    "        }\n" +
                    "\n" +
                    "        @media only screen and (max-width:42.09em) {\n" +
                    "            .dcf-table-responsive thead {\n" +
                    "                clip: rect(0 0 0 0);\n" +
                    "                -webkit-clip-path: inset(50%);\n" +
                    "                clip-path: inset(50%);\n" +
                    "                height: 1px;\n" +
                    "                overflow: hidden;\n" +
                    "                position: absolute;\n" +
                    "                width: 1px;\n" +
                    "                white-space: nowrap\n" +
                    "            }\n" +
                    "            .dcf-table-responsive tr {\n" +
                    "                display: block\n" +
                    "            }\n" +
                    "            .dcf-table-responsive td {\n" +
                    "                -webkit-column-gap: 3.16vw;\n" +
                    "                -moz-column-gap: 3.16vw;\n" +
                    "                column-gap: 3.16vw;\n" +
                    "                display: grid;\n" +
                    "                grid-template-columns: 1fr 2fr;\n" +
                    "                text-align: left!important\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered, .dcf-table-responsive.dcf-table-bordered thead th {\n" +
                    "                border-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered tbody td {\n" +
                    "                border-top-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered) tbody tr {\n" +
                    "                padding-bottom: .75em\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered) tbody td {\n" +
                    "                padding-bottom: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive:not(.dcf-table-bordered):not(.dcf-table-striped) tbody td {\n" +
                    "                padding-right: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive.dcf-table-bordered tbody tr:last-child td:last-child {\n" +
                    "                border-bottom-width: 0\n" +
                    "            }\n" +
                    "            .dcf-table-responsive tbody td:before {\n" +
                    "                content: attr(data-label);\n" +
                    "                float: left;\n" +
                    "                font-weight: 700;\n" +
                    "                padding-right: 1.78em\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-overflow-x-auto {\n" +
                    "            overflow-x: auto!important;\n" +
                    "            -webkit-overflow-scrolling: touch\n" +
                    "        }\n" +
                    "        .sign {\n" +
                    "            float: right;\n" +
                    "            padding-right: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:27%;\n" +
                    "        }\n" +
                    "        .date {\n" +
                    "            float: left;\n" +
                    "            padding-left: 20px;\n" +
                    "            padding-top: 20px;\n" +
                    "            width:45%;\n" +
                    "        }\n" +
                    "\n" +
                    "        .dcf-w-100\\% {\n" +
                    "            width: 100%!important;\n" +
                    "        }\n" +
                    "\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"wrapper\">\n" +
                    "    <table class=\"dcf-table dcf-table-responsive dcf-table-bordered dcf-table-striped dcf-w-100%\">\n" +
                    "        <thead>\n" +
                    "        <tr>\n" +
                    "            <th class=\"bold dcf-txt-center\" colspan=\"8\">COAST GUARD BUDGET : FY : ${finYear_placeholder}</th>\n" +
                    "        </tr>\n" +
                    "        <tr>\n" +
                    "            <th class=\"bold\" colspan=\"7\"></th>\n" +
                    "            <th class=\"bold dcf-txt-right\" colspan=\"1\">(In ${amountType_placeholder})</th>\n" +
                    "        </tr>\n" +
                    "        <tr>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">REVENUE OBJECT HEAD </th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">${allocationType_placeholder} ${finYear_placeholder} Allocation to ICG</th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">UNIT</th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">${allocationType_placeholder} : ${finYear_placeholder} Allocation</th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">Bill Submission Upto 15 Apr 23</th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">% Bill Submission w.r.t. ${allocationType_placeholder} ${finYear_placeholder}</th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">CGDA Booking Upto 15 Apr 23</th>\n" +
                    "            <th class=\"dcf-txt-center bold\" scope=\"col\">% Bill Clearance w.r.t. ${allocationType_placeholder} ${finYear_placeholder}</th>\n" +
                    "        </tr>\n" +
                    "        </thead>\n" +
                    "        <tbody>\n" +
                    "        ${data_placeholder}\n" +
                    "\n" +
                    "        </tbody>\n" +
                    "    </table>\n" +
                    "    <div class=\"sign\">\n" +
                    "        <ul style=\"list-style: none; margin-top: 0;\">\n" +
                    "            <li>${name_placeholder}</li>\n" +
                    "            <li>${unit_placeholder}</li>\n" +
                    "            <li>${rank_placeholder}</li>\n" +
                    "        </ul>\n" +
                    "    </div>\n" +
                    "    <div class=\"date\">\n" +
                    "        Date-${date_placeholder}\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>\n";
            StringBuilder sb = new StringBuilder();
            int i=1;
            String finyear = "";
            String unit = "";
            for(String val : rowData){
                String subHeadId=val;
                List<BudgetAllocationReport> reportDetails = budgetAllocationReportRepository.findBySubHeadIdAndAllocationType(subHeadId,allocationType);
                if(reportDetails.size()<=0){
                    return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
                    },"RECORD NOT FOUND OR EMPTY",HttpStatus.OK.value());
                }
                int count=0;
                float sum=0;
                float expsum=0;
                float percentagesum=0;
                Double amount;
                Double amountUnit;
                Double finAmount;
                Double eAmount;
                Double expnAmount;

                for (BudgetAllocationReport row : reportDetails){

                    for(Integer k = 0; k < units.size(); k++) {
                        if (units.get(k).getUnit().equalsIgnoreCase(row.getUnitId())){
                            finyear=row.getFinYearDes();
                            amount = Double.valueOf(row.getTotalAmount());
                            AmountUnit amountTypeObj=amountUnitRepository.findByAmountTypeId(row.getAmountType());
                            if(amountTypeObj==null){
                                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
                                },"AMOUNT TYPE NOT FOUND FROM DB",HttpStatus.OK.value());
                            }
                            amountUnit=amountTypeObj.getAmount();
                            finAmount = amount*amountUnit/reqAmount;

                            List<ContigentBill> expenditure = contigentBillRepository.findByCbUnitIdAndFinYearAndBudgetHeadID(row.getUnitId(), finYearId, subHeadId);
                            if(expenditure.size()<=0){
                                eAmount=0.0;
                            }else {
                                eAmount = Double.parseDouble(expenditure.get(0).getProgressiveAmount());
                            }
                            expnAmount=eAmount*100/finAmount;

                            sb.append("<tr>");
                            if(count==0) {
                                sb.append("<th scope=\"row\" >").append(StringEscapeUtils.escapeHtml4(row.getSubHeadDes())).append("</th>");
                                sb.append("<th scope=\"row bold\" >${ALLOCATION_placeholder}</th>");
                            } else
                            {
                                sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                                sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                            }
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(row.getUnitName())).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(finAmount)))).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(eAmount)))).append("</td>");
                            sb.append("<td class=\"the\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(expnAmount)))).append("</td>");
                            sb.append("<td class=\"the\"></td>");
                            sb.append("<td class=\"the\"></td>");
                            sb.append("</tr>");
                            count++;
                            sum+=Float.parseFloat(new BigDecimal(finAmount).toPlainString());
                            expsum+=Float.parseFloat(new BigDecimal(eAmount).toPlainString());
                            percentagesum+=Float.parseFloat(new BigDecimal(expnAmount).toPlainString());
                        }
                    }

                }
                if(count!=0) {

                    sb.append("<tr>");
                    sb.append("<td class=\"the bold\"></td>");
                    sb.append("<th scope=\"row\" class=\"bbtm\"></th>");
                    sb.append("<td class=\"the bold\">TOTAL</td>");
                    sb.append("<td class=\"the bold\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(sum)))).append("</td>");
                    sb.append("<td class=\"the bold\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(expsum)))).append("</td>");
                    sb.append("<td class=\"the bold\">").append(StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(percentagesum)))).append("</td>");
                    sb.append("<td class=\"the bold\"></td>");
                    sb.append("<td class=\"the bold\"></td>");

                    sb.append("</tr>");
                    count=0;
                    //print sum
                    String text=sb.toString().replace("${ALLOCATION_placeholder}",StringEscapeUtils.escapeHtml4(String.format("%1$0,1.4f",new BigDecimal(sum))));
                    sb.setLength(0);
                    sb.append(text);
                }

            }
            if(sb.toString().isEmpty()){
                return ResponseUtils.createFailureResponse(dtoList, new TypeReference<List<FilePathResponse>> () {
                }, "RECORD NOT FOUND", HttpStatus.OK.value());
            }

            htmlContent = htmlContent.replace("${name_placeholder}",StringEscapeUtils.escapeHtml4(names));
            htmlContent = htmlContent.replace("${unit_placeholder}",StringEscapeUtils.escapeHtml4(unitName));
            htmlContent = htmlContent.replace("${rank_placeholder}",StringEscapeUtils.escapeHtml4(rank));
            htmlContent = htmlContent.replace("${date_placeholder}",StringEscapeUtils.escapeHtml4(formattedDateTime));
            htmlContent = htmlContent.replace("${finYear_placeholder}",StringEscapeUtils.escapeHtml4(findyr.getFinYear()));
            htmlContent = htmlContent.replace("${amountType_placeholder}",StringEscapeUtils.escapeHtml4(amountIn));
            htmlContent = htmlContent.replace("${allocationType_placeholder}",StringEscapeUtils.escapeHtml4(allocationType));

            htmlContent = htmlContent.replace("${data_placeholder}", sb.toString());
            String filepath=HelperUtils.FILEPATH+"/main-allocation-report.pdf";
            File folder = new File(new File(".").getCanonicalPath() + HelperUtils.LASTFOLDERPATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = folder.getAbsolutePath() + "/" + "be-allocation-report.pdf";
            File file = new File(filePath);
            generatePdf(htmlContent, file.getAbsolutePath());
            //generatePdf(htmlContent, filepath);
            FilePathResponse response=new FilePathResponse();
            response.setPath(filepath);
            response.setFileName("BeAllocationReport.pdf");
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

}
