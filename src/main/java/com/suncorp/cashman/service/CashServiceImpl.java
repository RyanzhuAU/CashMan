package com.suncorp.cashman.service;

import com.suncorp.cashman.MessageConstants;
import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import com.suncorp.cashman.exception.CashSupplyException;
import com.suncorp.cashman.repository.CashSupplyRepository;
import com.suncorp.cashman.repository.CashTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ryan.zhu on 13/05/2018.
 */

@Service
public class CashServiceImpl implements CashService {
    @Autowired
    private CashSupplyRepository cashSupplyRepository;

    @Autowired
    private CashTypeRepository cashTypeRepository;

    public CashServiceImpl(CashSupplyRepository cashSupplyRepository, CashTypeRepository cashTypeRepository) {
        this.cashSupplyRepository = cashSupplyRepository;
        this.cashTypeRepository = cashTypeRepository;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void initializeCashMachine() throws Exception {
        Map<Integer, Integer> initializedCashSupplies = getDummyCashSuppliesForInitialize();

        cashSupplyRepository.deleteAllInBatch();
        cashTypeRepository.deleteAllInBatch();

        initializedCashSupplies.forEach((cashAmount, cashQuantity) -> {
            CashType cashType = new CashType("$" + cashAmount, cashAmount);
            cashTypeRepository.save(cashType);

            CashSupply cashSupply = new CashSupply(cashType, cashQuantity);
            cashSupplyRepository.save(cashSupply);
        });
    }

    public List<CashSupply> getCurrentCashSupplies() throws Exception {
        List<CashSupply> cashSupplyList = cashSupplyRepository.findAllByOrderByCashAmountDesc();

        return cashSupplyList;
    }

    public Map<CashType, CashSupply> dispenseCash(Integer cashAmount) throws CashSupplyException {
        Map<CashType, CashSupply> resultMap = new HashMap<>();

        logger.info("Get the existing cash supplies.");
        List<CashSupply> cashSupplyList = cashSupplyRepository.findAllByOrderByCashAmountDesc();

        if (cashSupplyList.isEmpty()) {
            logger.error(MessageConstants.NO_CASH_SUPPLY_ERROR_MESSAGE);
            throw new CashSupplyException(MessageConstants.NO_CASH_SUPPLY_ERROR_MESSAGE);
        }

        logger.info("Dispense the cash.");
        int amountLeft = withdraw(cashAmount, cashSupplyList, resultMap);

        if (amountLeft != 0) {
            throw new CashSupplyException(cashAmount, cashAmount - amountLeft, false);
        } else {
            cashSupplyRepository.save(cashSupplyList);
            checkCashStock(cashSupplyList);
        }

        return resultMap;
    }

    private int withdraw(Integer cashAmount, List<CashSupply> cashSupplyList, Map<CashType, CashSupply> dispenseResult) throws CashSupplyException {

        if (cashAmount == 0) {
            return cashAmount;
        }

        if (cashAmount < 0) {
            throw new IllegalArgumentException(MessageConstants.CASH_SUPPLY_NEGATIVE_AMOUNT_ERROR_MESSAGE);
        }

        // Check the account daily withdraw limitation
        int cashLimitation = getAccountCashWithdrawLimitation();
        if (cashAmount > cashLimitation) {
            throw new CashSupplyException(cashAmount, cashLimitation, true);
        }

        if (cashSupplyList.size() == 0) {
            return cashAmount;
        }

        CashSupply highestCash = cashSupplyList.get(0);

        if (supplyWithOneCash(cashAmount, highestCash, dispenseResult)) {
            return 0;
        }

        if (highestCash.getCashQuantity() > 0 && cashAmount >= highestCash.getCashType().getCashValue()) {
            highestCash.withdraw(1);

            CashType cashType = highestCash.getCashType();
            cashAmount -= cashType.getCashValue();

            addDispenseCashSupply(cashType, 1, dispenseResult);

            return withdraw(cashAmount, cashSupplyList, dispenseResult);
        } else {
            List<CashSupply> cashSupplyListWithoutHighestCash = cashSupplyList.subList(1, cashSupplyList.size()); // Remove the highest cash
            return withdraw(cashAmount, cashSupplyListWithoutHighestCash, dispenseResult);
        }

    }

    private void checkCashStock(List<CashSupply> cashSupplyList) throws CashSupplyException {
        Map<Integer, Integer> cashLowStockStandard = getDummyCashLowStockStandard();
        List<CashSupply> lowStockCashList = new ArrayList<>();

        cashSupplyList.forEach(cashSupply -> {
            Integer cashValue = cashSupply.getCashType().getCashValue();
            if (cashSupply.getCashQuantity() <= cashLowStockStandard.get(cashValue)) {
                lowStockCashList.add(cashSupply);
            }
        });

        //TODO stock notification. Send the lowStockCashList to the endpoint or via email/message.

    }

    //TODO need to get this limitation from somewhere else
    public int getAccountCashWithdrawLimitation() {
        return 1000;
    }

    private boolean supplyWithOneCash(Integer amount, CashSupply cashSupply, Map<CashType, CashSupply> dispenseResult) throws CashSupplyException {
        CashType cashType = cashSupply.getCashType();
        Integer cashValue = cashType.getCashValue();

        int leftAmount = amount % cashValue;
        int cashWithdrawQuantity = amount / cashValue;

        if (leftAmount == 0 && cashWithdrawQuantity < cashSupply.getCashQuantity()) {
            addDispenseCashSupply(cashType, cashWithdrawQuantity, dispenseResult);
            cashSupply.withdraw(cashWithdrawQuantity);

            return true;
        } else {
            return false;
        }
    }

    private void addDispenseCashSupply(CashType dispenseCashType, Integer cashWithdrawQuantity, Map<CashType, CashSupply> dispenseResult) {
        if (dispenseResult.containsKey(dispenseCashType)) {
            CashSupply dispenseCashSupply = dispenseResult.get(dispenseCashType);
            dispenseCashSupply.add(cashWithdrawQuantity);
        } else {
            CashSupply withdrawCashSupply = new CashSupply(dispenseCashType, cashWithdrawQuantity);
            dispenseResult.put(dispenseCashType, withdrawCashSupply);
        }
    }

    //TODO Should get this initialized cash supplies somewhere.
    private Map<Integer, Integer> getDummyCashSuppliesForInitialize() {
        Map<Integer, Integer> dummyCashSupplies = new HashMap<>();

        dummyCashSupplies.put(100, 10);
        dummyCashSupplies.put(50, 20);
        dummyCashSupplies.put(20, 30);
        dummyCashSupplies.put(10, 40);
        dummyCashSupplies.put(5, 50);
        dummyCashSupplies.put(2, 60);
        dummyCashSupplies.put(1, 70);

        return dummyCashSupplies;
    }

    //TODO Should get this low stock standard setting somewhere.
    private Map<Integer, Integer> getDummyCashLowStockStandard() {
        Map<Integer, Integer> dummyCashLowSupplyStandard= new HashMap<>();

        dummyCashLowSupplyStandard.put(100, 2);
        dummyCashLowSupplyStandard.put(50, 3);
        dummyCashLowSupplyStandard.put(20, 4);
        dummyCashLowSupplyStandard.put(10, 5);
        dummyCashLowSupplyStandard.put(5, 6);
        dummyCashLowSupplyStandard.put(2, 7);
        dummyCashLowSupplyStandard.put(1, 8);

        return dummyCashLowSupplyStandard;
    }
}
