package com.suncorp.cashman.representation;

import lombok.Data;

/**
 * Create by ryan.zhu on 14/05/2018
 **/

@Data
public class CashSupplyRep {

    private Integer cashValue;

    private String cashDesc;

    private Integer cashQuantity;

    public CashSupplyRep() {}

    public CashSupplyRep(Integer cashValue, String cashDesc, Integer cashQuantity) {
        this.cashValue = cashValue;
        this.cashDesc = cashDesc;
        this.cashQuantity = cashQuantity;
    }

}
