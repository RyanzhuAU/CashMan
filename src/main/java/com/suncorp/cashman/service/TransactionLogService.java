package com.suncorp.cashman.service;

import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;

import java.util.Map;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

public interface TransactionLogService {

    void saveTransactionLog(Map<CashType, CashSupply> dispensedCash, Integer cashAmount);

}
