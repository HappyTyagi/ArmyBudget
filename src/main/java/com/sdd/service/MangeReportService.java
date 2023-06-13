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

    ApiResponse<List<FilePathResponse>> getAllocationReportRevised(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getContingentBillReport(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getContingentBillReportDoc(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getAllocationReportUnitWise(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getAllocationReportSubHead(ReportRequest reportRequest);

    ApiResponse<List<FilePathResponse>> getReAllocationReport(ReportRequest reportRequest);

//    ApiResponse<DefaultResponse> getConicalPath();

    ApiResponse<FilePathResponse> getCdaParkingReport(CDAReportRequest reportRequest);

    ApiResponse<FilePathResponse> getCdaParkingReportDoc(CDAReportRequest reportRequest);



//  UNIT WISE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getUnitWiseAllocationReport(UnitWiseAllocationReport reportRequest);
    ApiResponse<List<FilePathResponse>> getUnitWiseAllocationReportDoc(UnitWiseAllocationReport reportRequest);
    ApiResponse<List<BeReportResp>> getUnitWiseAllocationReportExcel(UnitWiseAllocationReport reportRequest);

//  SUBHEAD WISE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getSubHeadWiseAllocationReport(SubHeadWiseAllocationReportReq req);
    ApiResponse<List<FilePathResponse>> getSubHeadWiseAllocationReportDoc(SubHeadWiseAllocationReportReq req);
    ApiResponse<List<BeReportResp>> getSubHeadWiseAllocationReportExcel(SubHeadWiseAllocationReportReq req);

//  BE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getBEAllocationReport(String finYearId, String allocationType, String amountTypeId);
    ApiResponse<List<FilePathResponse>> getBEAllocationReportDoc(String finYearId,String allocationType, String amountTypeId);
    ApiResponse<List<BeReportResp>> getBEAllocationReportExcel(String finYearId,String allocationType, String amountTypeId);

//  RE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getREAllocationReport(String finYearId, String allocationType, String amountTypeId);
    ApiResponse<List<FilePathResponse>> getREAllocationReportDoc(String finYearId, String allocationType, String amountTypeId);
    ApiResponse<List<RivisionReportResp>> getREAllocationReportExcel(String finYearId, String allocationType, String amountTypeId);

//  BEandRE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getBEREAllocationReport(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId);
    ApiResponse<List<FilePathResponse>> getBEREAllocationReportDoc(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId);
    ApiResponse<List<BEREResponce>> getBEREAllocationReportExcel(String finYearId, String allocationTypeBE, String allocationTypeRE, String amountTypeId);


//  FER ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getMainBEAllocationReport(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate);
    ApiResponse<List<FilePathResponse>> getMainBEAllocationReportDoc(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate);
    ApiResponse<List<FerResponse>> getMainBEAllocationReportExcel(String finYearId, String allocationType, String amountTypeId, String fromDate, String toDate);


 //  UNIT REBASE ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getUnitRebaseReport(String fromDate, String toDate);
    ApiResponse<List<FilePathResponse>> getUnitRebaseReportDoc(String fromDate, String toDate);
    ApiResponse<List<UnitRebaseReportResponce>> getUnitRebaseReportExcel(String fromDate, String toDate);

 //  REVISED ALLOCATION REPORT
    ApiResponse<List<FilePathResponse>> getRevisedAllocationReport(String authGroupId);

}


