package com.sdd.response;

import com.sdd.entities.AllocationType;
import com.sdd.entities.BudgetFinancialYear;
import com.sdd.entities.BudgetHead;
import com.sdd.entities.CgUnit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DashBoardExprnditureResponse {
  private List<GrTotalObjResp> grTotalObjResp;

  String sumAlloc;
  String sumExp;
  String sumBal;
  String perBal;


}
