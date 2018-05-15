package com.suncorp.cashman.service;

import com.suncorp.cashman.domain.*;
import com.suncorp.cashman.repository.TransactionLogDetailRepository;
import com.suncorp.cashman.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@Service
public class TransactionLogServiceImpl implements TransactionLogService {

    @Autowired
    TransactionLogRepository transactionLogRepository;

    @Autowired
    TransactionLogDetailRepository transactionLogDetailRepository;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public TransactionLogServiceImpl(TransactionLogRepository transactionLogRepository, TransactionLogDetailRepository transactionLogDetailRepository) {
        this.transactionLogDetailRepository = transactionLogDetailRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    public void saveTransactionLog(Map<CashType, CashSupply> dispensedCash, Integer cashAmount) {
        logger.info("Get the bank account info.");
        BankAccountDetail bankAccountDetail = getDummyBankAccountDetail();

        logger.info("Create the transaction log.");
        TransactionLog transactionLog = new TransactionLog(bankAccountDetail, cashAmount, LocalDateTime.now());
        this.transactionLogRepository.save(transactionLog);

        logger.info("Create the transaction detail log.");
        dispensedCash.forEach((cashType, cashSupply) -> {
            TransactionLogDetail transactionLogDetail = new TransactionLogDetail();
            transactionLogDetail.setCashDesc(cashType.getCashDesc());
            transactionLogDetail.setCashValue(cashType.getCashValue());
            transactionLogDetail.setQuantity(cashSupply.getCashQuantity());
            transactionLogDetail.setTransactionLog(transactionLog);

            this.transactionLogDetailRepository.save(transactionLogDetail);
        });
    }

    private BankAccountDetail getDummyBankAccountDetail() {
        return new BankAccountDetail("111111", "12345678", "James");
    }
}
