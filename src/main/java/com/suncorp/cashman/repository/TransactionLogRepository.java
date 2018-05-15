package com.suncorp.cashman.repository;

import com.suncorp.cashman.domain.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    TransactionLog findByTransactionLogId(long transactionLogId);

}
