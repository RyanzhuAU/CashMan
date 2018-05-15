package com.suncorp.cashman.domain;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@Data
@Entity
@Table(name = "cash_type")
public class CashType {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long cashTypeId;

    private String cashDesc;

    private Integer cashValue;

    public CashType() {

    }

    public CashType(String cashDesc, Integer cashValue) {
        this.cashValue = cashValue;
        this.cashDesc = cashDesc;
    }

}
