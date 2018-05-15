package com.suncorp.cashman.repository;

import com.suncorp.cashman.domain.TransactionLogDetail;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

public interface TransactionLogDetailRepository extends JpaRepository<TransactionLogDetail, String> {

    TransactionLogDetail findByTransactionDetailId(long transactionDetailId);

}
