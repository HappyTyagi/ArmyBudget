package com.sdd.service;

import com.sdd.request.CDAReportRequest;
import com.sdd.request.ReportRequest;
import com.sdd.request.SubHeadWiseAllocationReportReq;
import com.sdd.request.UnitWiseAllocationReport;
import com.sdd.response.*;

import java.util.Date;
import java.util.List;


public interface MangeReportService {


    ApiResponse<List<FilePathResponse>> getAllocationReport(String reportRequest);

    ApiResponse<List<FilePathResponse>> getAllocationReportDoc(String reportRequest);

    ApiResponse<List<FilePathResponse>> getReceiptReport(String reportRequest);

    ApiResponse<List<FilePathResponse>> getReceiptReportDoc(String reportRequest);


    ApiResponse<List<FilePathResponse>> getReceiptReportRevision(String reportRequest);

    ApiResponse<List<FilePathResponse>> getReceiptReportRevisionDoc(String reportRequest);


    ApiResponse<List<FilePathResponse>> getConsolidateReceiptReport(String finYearId,String allocationType,String amountType);

    ApiResponse<List<FilePathResponse>> getConsolidateReceiptReportDoc(String finYearId,String allocationType,String amountType);


    ApiResponse<List<FilePathResponse>> getAllocationReportRevised(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getContingentBillReport(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getContingentBillAll(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getContingentBillReportDoc(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getAllocationReportUnitWise(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getAllocationReportSubHead(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getReAllocationReport(ReportRequest reportRequest);

//    ApiResponse<DefaultResponse> getConicalPath();

    ApiResponse<FilePathResponse> getCdaParkingReport(CDAReportRequest reportRequest);

    ApiResponse<FilePathResponse> getCdaParkingReportDoc(CDAReportRequest reportRequest);


    ApiResponse<FilePathResponse> getReservedFund(CDAReportRequest reportRequest);

    ApiResponse<FilePathResponse> getReservedFundDoc(CDAReportRequest reportRequest);



//  UNIT WISE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getUnitWiseAllocationReport(UnitWiseAllocationReport reportRequest);
    ApiResponse<List<FilePathResponse>> getUnitWiseAllocationReportDoc(UnitWiseAllocationReport reportRequest);
    ApiResponse<List<BeReportResp>> getUnitWiseAllocationReportExcel(UnitWiseAllocationReport reportRequest);

//  SUBHEAD WISE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getSubHeadWiseAllocationReport(SubHeadWiseAllocationReportReq req);
    ApiResponse<List<FilePathResponse>> getSubHeadWiseAllocationReportDoc(SubHeadWiseAllocationReportReq req);
    ApiResponse<List<BeReportResp>> getSubHeadWiseAllocationReportExcel(SubHeadWiseAllocationReportReq req);

//  BE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getBEAllocationReport(String finYearId, String allocationType, String amountTypeId,String status, String majorHd);
    ApiResponse<List<FilePathResponse>> getBEAllocationReportDoc(String finYearId, String allocationType, String amountTypeId,String status,String majorHd);
    ApiResponse<List<BeReportResp>> getBEAllocationReportExcel(String finYearId, String allocationType, String amountTypeId,String status,String majorHd);

//  RE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getREAllocationReport(String finYearId, String allocationType, String amountTypeId,String majorHd);
    ApiResponse<List<FilePathResponse>> getREAllocationReportDoc(String finYearId, String allocationType, String amountTypeId,String majorHd);
    ApiResponse<List<RivisionReportResp>> getREAllocationReportExcel(String finYearId, String allocationType, String amountTypeId,String majorHd);

//  BEandRE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getBEREAllocationReport(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId,String majorHd);
    ApiResponse<List<FilePathResponse>> getBEREAllocationReportDoc(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId,String majorHd);
    ApiResponse<List<BEREResponce>> getBEREAllocationReportExcel(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId,String majorHd);


//  FER ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getMainBEAllocationReport(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate,String majorHd);
    ApiResponse<List<FilePathResponse>> getMainBEAllocationReportDoc(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate,String majorHd);
    ApiResponse<List<FerResponse>> getMainBEAllocationReportExcel(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate,String majorHd);



    //   MA ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getMAAllocationReport(String finYearId, String allocationTypeBE, String allocationTypeRE,String allocationTypeMA, String amountTypeId,String majorHd);
    ApiResponse<List<FilePathResponse>> getMAAllocationReportDoc(String finYearId, String allocationTypeBE, String allocationTypeRE,String allocationTypeMA, String amountTypeId,String majorHd);
    ApiResponse<List<MAResponceReport>> getMAAllocationReportExcel(String finYearId, String allocationTypeBE, String allocationTypeRE,String allocationTypeMA, String amountTypeId,String majorHd);


    //  REVISED ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getRevisedAllocationReport(String authGroupId);
    ApiResponse<List<FilePathResponse>> getRevisedAllocationAprReport(String authGroupId);

    ApiResponse<List<FilePathResponse>> getRevisedAllocationReportDoc(String authGroupId);
    ApiResponse<List<FilePathResponse>> getRevisedAllocationAprReportDoc(String authGroupId);

}


