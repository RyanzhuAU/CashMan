package com.suncorp.cashman.repository;

import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

public interface CashSupplyRepository extends JpaRepository<CashSupply, String> {
    String cash_supply_ordered_query = "Select s from CashSupply s join s.cashType t order by t.cashValue desc";

    @Query(cash_supply_ordered_query)
    List<CashSupply> findAllByOrderByCashAmountDesc();

    CashSupply findByCashTypeEquals(CashType cashType);

}
