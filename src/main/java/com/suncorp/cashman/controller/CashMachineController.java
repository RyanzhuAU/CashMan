package com.suncorp.cashman.controller;

import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import com.suncorp.cashman.exception.CashSupplyException;
import com.suncorp.cashman.representation.CashSupplyRep;
import com.suncorp.cashman.service.CashService;
import com.suncorp.cashman.service.TransactionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@RestController
@RequestMapping("/cashMachine")
public class CashMachineController {

    @Autowired
    private CashService cashService;

    @Autowired
    private TransactionLogService transactionLogService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Initialize the cash supply
     * @return
     */
    @RequestMapping(value = "/initialize", method = RequestMethod.POST)
    public ResponseEntity initializeCashMachine() {
        try {
            cashService.initializeCashMachine();
            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Dispense the cash
     *
     * @param cashAmount the required cash amount
     * @return list of the cash supply to meet the required cash amount
     * @throws CashSupplyException handles three different scenario
     *                             - There is no cash in the machine
     *                             - The existing cash supply could not meet the required cash amount
     *                             - The required cash amount is over the daily withdraw limitation
     */
    @RequestMapping(value = "/dispenseCash/{cashAmount}", method = RequestMethod.GET)
    public ResponseEntity dispenseCash(@PathVariable("cashAmount") Integer cashAmount) {
        try {
            List<CashSupplyRep> cashSupplyRepList = new ArrayList<>();
            Map<CashType, CashSupply> resultMap = cashService.dispenseCash(cashAmount);

            resultMap.forEach((cashType, cashSupply) -> {
                CashSupplyRep cashSupplyRep = new CashSupplyRep(cashType.getCashValue(), cashType.getCashDesc(), cashSupply.getCashQuantity());
                cashSupplyRepList.add(cashSupplyRep);
            });

            transactionLogService.saveTransactionLog(resultMap, cashAmount);

            return new ResponseEntity(cashSupplyRepList, HttpStatus.OK);
        } catch (CashSupplyException e) {
            logger.error(e.getMessage());
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Check the current cash stock.
     * @return the list of the current cash stock ordered by cash value desc.
     */
    @RequestMapping(value = "/getCashStock", method = RequestMethod.GET)
    public ResponseEntity getCashStock() {
        try {
            List<CashSupplyRep> cashSupplyRepList = new ArrayList<>();

            List<CashSupply> existingCashSupplies = cashService.getCurrentCashSupplies();

            existingCashSupplies.forEach(cashSupply -> {
                CashSupplyRep cashSupplyRep = new CashSupplyRep(cashSupply.getCashType().getCashValue(), cashSupply.getCashType().getCashDesc(), cashSupply.getCashQuantity());
                cashSupplyRepList.add(cashSupplyRep);
            });

            return new ResponseEntity(cashSupplyRepList, HttpStatus.OK);

        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
