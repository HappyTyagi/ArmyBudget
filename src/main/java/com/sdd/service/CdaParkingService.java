package com.sdd.service;

import com.sdd.entities.CdaParking;
import com.sdd.request.CDARequest;
import com.sdd.request.CDARequestReBase;
import com.sdd.response.ApiResponse;
import com.sdd.response.CdaParkingTransResponse;
import com.sdd.response.DefaultResponse;
import com.sdd.response.ReabseCdaParkingResponse;

import java.util.List;

public interface CdaParkingService {



	ApiResponse<DefaultResponse> saveCdaParkingData(CDARequest cdaRequest);

	ApiResponse<DefaultResponse> saveCdaParkingDataForRebase(CDARequest cdaRequest);

	ApiResponse<CdaParkingTransResponse> getCdaData(String groupId);

	ApiResponse<CdaParkingTransResponse> getAllCdaData(CDARequest cdaRequest);

	ApiResponse<ReabseCdaParkingResponse> getOldCdaDataForRebase(CDARequestReBase cdaRequest);

	ApiResponse<ReabseCdaParkingResponse> getCheckExpForUnit(CDARequestReBase cdaRequest);

	ApiResponse<List<CdaParking>> getCdaUnitList();

	ApiResponse<DefaultResponse> updateCdaParkingData(CDARequest cdaRequest);

}
