package com.sdd.controller.WebApi;


import com.sdd.request.CDAReportRequest;
import com.sdd.request.ReportRequest;
import com.sdd.request.SubHeadWiseAllocationReportReq;
import com.sdd.request.UnitWiseAllocationReport;
import com.sdd.response.*;
import com.sdd.service.MangeReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("/reportController")
@Slf4j
public class ReportController {

    @Autowired
    private MangeReportService mangeReportService;


    @GetMapping("/getAllocationReport/{authgroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getAllocationReport(@PathVariable("authgroupId") String authgroupId) {
        return new ResponseEntity<>(mangeReportService.getAllocationReport(authgroupId), HttpStatus.OK);
    }

    @GetMapping("/getAllocationReportDoc/{authgroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getAllocationReportDoc(@PathVariable("authgroupId") String authgroupId) {
        return new ResponseEntity<>(mangeReportService.getAllocationReportDoc(authgroupId), HttpStatus.OK);
    }


    @GetMapping("/getReceiptReport/{authgroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getReceiptReport(@PathVariable("authgroupId") String authgroupId) {
        return new ResponseEntity<>(mangeReportService.getReceiptReport(authgroupId), HttpStatus.OK);
    }

    @GetMapping("/getReceiptReportDoc/{authgroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getReceiptReportDoc(@PathVariable("authgroupId") String authgroupId) {
        return new ResponseEntity<>(mangeReportService.getReceiptReportDoc(authgroupId), HttpStatus.OK);
    }


    @GetMapping("/getReceiptReportRevision/{authgroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getReceiptReportRevision(@PathVariable("authgroupId") String authgroupId) {
        return new ResponseEntity<>(mangeReportService.getReceiptReportRevision(authgroupId), HttpStatus.OK);
    }

    @GetMapping("/getReceiptReportRevisionDoc/{authgroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getReceiptReportRevisionDoc(@PathVariable("authgroupId") String authgroupId) {
        return new ResponseEntity<>(mangeReportService.getReceiptReportRevisionDoc(authgroupId), HttpStatus.OK);
    }


    @GetMapping("/getConsolidateReceiptReport/{finYearId}/{allocationType}/{amountType}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getConsolidateReceiptReport(@PathVariable("finYearId") String finYearId, @PathVariable("allocationType") String allocationType, @PathVariable("amountType") String amountType) {
        return new ResponseEntity<>(mangeReportService.getConsolidateReceiptReport(finYearId, allocationType, amountType), HttpStatus.OK);
    }

    @GetMapping("/getConsolidateReceiptReportDoc/{finYearId}/{allocationType}/{amountType}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getConsolidateReceiptReportDoc(@PathVariable("finYearId") String finYearId, @PathVariable("allocationType") String allocationType, @PathVariable("amountType") String amountType) {
        return new ResponseEntity<>(mangeReportService.getConsolidateReceiptReportDoc(finYearId, allocationType, amountType), HttpStatus.OK);
    }


    @PostMapping("/getAllocationReportRevised")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getAllocationReportRevised(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getAllocationReportRevised(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getContingentBillReport")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getContingentBillReport(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getContingentBillReport(reportRequest), HttpStatus.OK);
    }

    @PostMapping("/getContingentBillAll")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getContingentBillAll(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getContingentBillReport(reportRequest), HttpStatus.OK);
    }

    @PostMapping("/getContingentBillReportDoc")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getContingentBillReportDoc(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getContingentBillReportDoc(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getAllocationReportUnitWise")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getAllocationReportUnitWise(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getAllocationReportUnitWise(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getAllocationReportSubHead")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getAllocationReportSubHead(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getAllocationReportSubHead(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getReAllocationReport")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getReAllocationReport(@RequestBody ReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getReAllocationReport(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getReservedFund")
    public ResponseEntity<ApiResponse<FilePathResponse>> getReservedFund(@RequestBody CDAReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getReservedFund(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getReservedFundDoc")
    public ResponseEntity<ApiResponse<FilePathResponse>> getReservedFundDoc(@RequestBody CDAReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getReservedFundDoc(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getCdaParkingReportDoc")
    public ResponseEntity<ApiResponse<FilePathResponse>> getCdaParkingReportDoc(@RequestBody CDAReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getCdaParkingReportDoc(reportRequest), HttpStatus.OK);
    }


    @PostMapping("/getCdaParkingReport")
    public ResponseEntity<ApiResponse<FilePathResponse>> getCdaParkingReport(@RequestBody CDAReportRequest reportRequest) {
        return new ResponseEntity<>(mangeReportService.getCdaParkingReport(reportRequest), HttpStatus.OK);
    }



    //  UNIT WISE ALLOCATION REPORT
    @PostMapping("/getUnitWiseAllocationReport")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getUnitWiseAllocationReport(@RequestBody UnitWiseAllocationReport reportRequest) {
        return new ResponseEntity<>(mangeReportService.getUnitWiseAllocationReport(reportRequest), HttpStatus.OK);
    }

    @PostMapping("/getUnitWiseAllocationReportDoc")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getUnitWiseAllocationReportDoc(@RequestBody UnitWiseAllocationReport reportRequest) {
        return new ResponseEntity<>(mangeReportService.getUnitWiseAllocationReportDoc(reportRequest), HttpStatus.OK);
    }

    @PostMapping("/getUnitWiseAllocationReportExcel")
    public ResponseEntity<ApiResponse<List<BeReportResp>>> getUnitWiseAllocationReportExcel(@RequestBody UnitWiseAllocationReport reportRequest) {
        return new ResponseEntity<>(mangeReportService.getUnitWiseAllocationReportExcel(reportRequest), HttpStatus.OK);
    }

    //  SUBHEAD WISE ALLOCATION REPORT
    @PostMapping("/getSubHeadWiseAllocationReport")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getSubHeadWiseAllocationReport(@RequestBody SubHeadWiseAllocationReportReq req) {
        return new ResponseEntity<>(mangeReportService.getSubHeadWiseAllocationReport(req), HttpStatus.OK);
    }

    @PostMapping("/getSubHeadWiseAllocationReportDoc")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getSubHeadWiseAllocationReportDoc(@RequestBody SubHeadWiseAllocationReportReq req) {
        return new ResponseEntity<>(mangeReportService.getSubHeadWiseAllocationReportDoc(req), HttpStatus.OK);
    }

    @PostMapping("/getSubHeadWiseAllocationReportExcel")
    public ResponseEntity<ApiResponse<List<BeReportResp>>> getSubHeadWiseAllocationReportExcel(@RequestBody SubHeadWiseAllocationReportReq req) {
        return new ResponseEntity<>(mangeReportService.getSubHeadWiseAllocationReportExcel(req), HttpStatus.OK);
    }

    //  BE ALLOCATION REPORT
    @GetMapping("/getBEAllocationReport/{finYearId}/{allocationType}/{amountTypeId}/{status}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getBEAllocationReport(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "status") String status , @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getBEAllocationReport(finYearId, allocationType, amountTypeId, status,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getBEAllocationReportDoc/{finYearId}/{allocationType}/{amountTypeId}/{status}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getBEAllocationReportDoc(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "status") String status, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getBEAllocationReportDoc(finYearId, allocationType, amountTypeId, status,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getBEAllocationReportExcel/{finYearId}/{allocationType}/{amountTypeId}/{status}/{majorHd}")
    public ResponseEntity<ApiResponse<List<BeReportResp>>> getBEAllocationReportExcel(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "status") String status, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getBEAllocationReportExcel(finYearId, allocationType, amountTypeId, status,majorHd), HttpStatus.OK);
    }

    //  RE ALLOCATION REPORT
    @GetMapping("/getREAllocationReport/{finYearId}/{allocationType}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getREAllocationReport(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getREAllocationReport(finYearId, allocationType, amountTypeId,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getREAllocationReportDoc/{finYearId}/{allocationType}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getREAllocationReportDoc(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getREAllocationReportDoc(finYearId, allocationType, amountTypeId,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getREAllocationReportExcel/{finYearId}/{allocationType}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<RivisionReportResp>>> getREAllocationReportExcel(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getREAllocationReportExcel(finYearId, allocationType, amountTypeId,majorHd), HttpStatus.OK);
    }

    //  BEandRE ALLOCATION REPORT
    @GetMapping("/getBEREAllocationReport/{finYearId}/{allocationTypeBE}/{allocationTypeRE}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getBEREAllocationReport(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationTypeBE") String allocationTypeBE, @PathVariable(value = "allocationTypeRE") String allocationTypeRE, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getBEREAllocationReport(finYearId, allocationTypeBE, allocationTypeRE, amountTypeId,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getBEREAllocationReportDoc/{finYearId}/{allocationTypeBE}/{allocationTypeRE}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getBEREAllocationReportDoc(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationTypeBE") String allocationTypeBE, @PathVariable(value = "allocationTypeRE") String allocationTypeRE, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getBEREAllocationReportDoc(finYearId, allocationTypeBE, allocationTypeRE, amountTypeId,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getBEREAllocationReportExcel/{finYearId}/{allocationTypeBE}/{allocationTypeRE}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<BEREResponce>>> getBEREAllocationReportExcel(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationTypeBE") String allocationTypeBE, @PathVariable(value = "allocationTypeRE") String allocationTypeRE, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getBEREAllocationReportExcel(finYearId, allocationTypeBE, allocationTypeRE, amountTypeId,majorHd), HttpStatus.OK);
    }

    //  FER ALLOCATION REPORT
    @GetMapping("/getMainBEAllocationReport/{finYearId}/{allocationType}/{amountTypeId}/{fromDate}/{toDate}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getMainBEAllocationReport(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "fromDate") String fromDate, @PathVariable(value = "toDate") String toDate, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getMainBEAllocationReport(finYearId, allocationType, amountTypeId, fromDate, toDate,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getMainBEAllocationReportDoc/{finYearId}/{allocationType}/{amountTypeId}/{fromDate}/{toDate}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getMainBEAllocationReportDoc(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "fromDate") String fromDate, @PathVariable(value = "toDate") String toDate, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getMainBEAllocationReportDoc(finYearId, allocationType, amountTypeId, fromDate, toDate,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getMainBEAllocationReportExcel/{finYearId}/{allocationType}/{amountTypeId}/{fromDate}/{toDate}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FerResponse>>> getMainBEAllocationReportExcel(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationType") String allocationType, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "fromDate") String fromDate, @PathVariable(value = "toDate") String toDate, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getMainBEAllocationReportExcel(finYearId, allocationType, amountTypeId, fromDate, toDate,majorHd), HttpStatus.OK);
    }



    //  MA ALLOCATION REPORT
    @GetMapping("/getMAAllocationReport/{finYearId}/{allocationTypeBE}/{allocationTypeRE}/{allocationTypeMA}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getMAAllocationReport(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationTypeBE") String allocationTypeBE, @PathVariable(value = "allocationTypeRE") String allocationTypeRE,@PathVariable(value = "allocationTypeMA") String allocationTypeMA, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getMAAllocationReport(finYearId, allocationTypeBE, allocationTypeRE,allocationTypeMA, amountTypeId,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getMAAllocationReportDoc/{finYearId}/{allocationTypeBE}/{allocationTypeRE}/{allocationTypeMA}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getMAAllocationReportDoc(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationTypeBE") String allocationTypeBE, @PathVariable(value = "allocationTypeRE") String allocationTypeRE,@PathVariable(value = "allocationTypeMA") String allocationTypeMA, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getMAAllocationReportDoc(finYearId, allocationTypeBE, allocationTypeRE,allocationTypeMA, amountTypeId,majorHd), HttpStatus.OK);
    }

    @GetMapping("/getMAAllocationReportExcel/{finYearId}/{allocationTypeBE}/{allocationTypeRE}/{allocationTypeMA}/{amountTypeId}/{majorHd}")
    public ResponseEntity<ApiResponse<List<MAResponceReport>>> getMAAllocationReportExcel(@PathVariable(value = "finYearId") String finYearId, @PathVariable(value = "allocationTypeBE") String allocationTypeBE, @PathVariable(value = "allocationTypeRE") String allocationTypeRE,@PathVariable(value = "allocationTypeMA") String allocationTypeMA, @PathVariable(value = "amountTypeId") String amountTypeId, @PathVariable(value = "majorHd") String majorHd) {
        return new ResponseEntity<>(mangeReportService.getMAAllocationReportExcel(finYearId, allocationTypeBE, allocationTypeRE,allocationTypeMA, amountTypeId,majorHd), HttpStatus.OK);
    }


    //  REVISED ALLOCATION REPORT
    @GetMapping("/getRevisedAllocationReport/{authGroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getRevisedAllocationReport(@PathVariable(value = "authGroupId") String authGroupId) {
        return new ResponseEntity<>(mangeReportService.getRevisedAllocationReport(authGroupId), HttpStatus.OK);
    }

    @GetMapping("/getRevisedAllocationAprReport/{authGroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getRevisedAllocationAprReport(@PathVariable(value = "authGroupId") String authGroupId) {
        return new ResponseEntity<>(mangeReportService.getRevisedAllocationAprReport(authGroupId), HttpStatus.OK);
    }

    @GetMapping("/getRevisedAllocationReportDoc/{authGroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getRevisedAllocationReportDoc(@PathVariable(value = "authGroupId") String authGroupId) {
        return new ResponseEntity<>(mangeReportService.getRevisedAllocationReportDoc(authGroupId), HttpStatus.OK);
    }

    @GetMapping("/getRevisedAllocationAprReportDoc/{authGroupId}")
    public ResponseEntity<ApiResponse<List<FilePathResponse>>> getRevisedAllocationAprReportDoc(@PathVariable(value = "authGroupId") String authGroupId) {
        return new ResponseEntity<>(mangeReportService.getRevisedAllocationAprReportDoc(authGroupId), HttpStatus.OK);
    }



}
