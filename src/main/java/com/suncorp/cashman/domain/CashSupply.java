package com.suncorp.cashman.domain;

import com.suncorp.cashman.MessageConstants;
import lombok.Data;

import javax.persistence.*;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@Data
@Entity
@Table(name = "cash_supply")
public class CashSupply {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long cashSupplyId;

    @OneToOne
    @JoinColumn(name="cash_type_id")
    private CashType cashType;

    private Integer cashQuantity;

    public CashSupply() {

    }

    public CashSupply(CashType cashType, Integer cashQuantity) {
        this.cashType = cashType;
        this.cashQuantity = cashQuantity;
    }

    public void withdraw (Integer withdrawQuantity) {
        if (withdrawQuantity < 0) {
            throw new IllegalArgumentException(MessageConstants.CASH_SUPPLY_NEGATIVE_CASH_QUANTITY_ERROR_MESSAGE);
        }

        this.cashQuantity -= withdrawQuantity;
    }

    public void add (Integer cashQuantity) {
        if (cashQuantity < 0) {
            throw new IllegalArgumentException(MessageConstants.CASH_SUPPLY_NEGATIVE_CASH_QUANTITY_ERROR_MESSAGE);
        }

        this.cashQuantity += cashQuantity;
    }


}
