package com.suncorp.cashman.service;

import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import com.suncorp.cashman.exception.CashSupplyException;

import java.util.List;
import java.util.Map;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

public interface CashService {

    void initializeCashMachine() throws Exception;

    List<CashSupply> getCurrentCashSupplies() throws Exception;

    Map<CashType, CashSupply> dispenseCash(Integer cashAmount) throws CashSupplyException;

    int getAccountCashWithdrawLimitation();

}
