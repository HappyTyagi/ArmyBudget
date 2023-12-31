package com.sdd.request;

import com.sdd.entities.AllocationType;
import com.sdd.entities.AmountUnit;
import com.sdd.entities.BudgetHead;
import com.sdd.entities.CgUnit;
import com.sdd.response.CdaDetailsForRebaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class RebaseBudgetHistory {
    private String unit;
    private CgUnit fromUnit;
    private String finYear;
    private String authGrupId;
    private BudgetHead subHead;
    private String allocatedAmount;
    private String expenditureAmount;
    private String remBal;
    private String remCdaBal;
    private String status;
    private AmountUnit amountType;
    private AllocationType allocationType;
    private Date lastCbDate;
    private List<CdaDetailsForRebaseResponse> cdaData;
}
