package com.suncorp.cashman.domain;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@Data
@Entity
@Table(name = "transaction_log_detail")
public class TransactionLogDetail {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long transactionDetailId;

    @ManyToOne
    @JoinColumn(name="transaction_log_id")
    private TransactionLog transactionLog;

    private String cashDesc;

    private Integer cashValue;

    private Integer quantity;

    public TransactionLogDetail() {

    }

}
