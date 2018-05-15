package com.suncorp.cashman.domain;

import com.suncorp.cashman.converter.LocalDateTimeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@Data
@Entity
@Table(name = "transaction_log")
public class TransactionLog {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long transactionLogId;

    private String bsb;

    private String accountNo;

    private String accountName;

    private Integer totalAmount;

    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime createAt;

    public TransactionLog() {

    }

    public TransactionLog(BankAccountDetail bankAccountDetail, Integer totalAmount, LocalDateTime createAt) {
        this.bsb = bankAccountDetail.getBsb();
        this.accountNo = bankAccountDetail.getAccountNo();
        this.accountName = bankAccountDetail.getAccountName();
        this.totalAmount = totalAmount;
        this.createAt = createAt;
    }

}
