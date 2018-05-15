package com.suncorp.cashman.domain;

import lombok.Data;

/**
 * Create by ryan.zhu on 14/05/2018
 **/

@Data
public class BankAccountDetail {

    private String bsb;

    private String accountNo;

    private String accountName;

    public BankAccountDetail(String bsb, String accountNo, String accountName) {
        this.bsb = bsb;
        this.accountNo = accountNo;
        this.accountName = accountName;
    }
}
