package com.suncorp.cashman.repository;

import com.suncorp.cashman.domain.CashType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

public interface CashTypeRepository extends JpaRepository<CashType, String> {

    CashType findByCashTypeId(long cashTypeId);

}
