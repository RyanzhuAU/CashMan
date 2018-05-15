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

    /**
     * Used to initialize the cash machine with the given cash supplies.
     */
    public void initializeCashMachine() {
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

    /**
     * Used to get the current stock of cash supplies.
     *
     * @return the list of current cash supplies.
     */
    public List<CashSupply> getCurrentCashSupplies() {
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

    /**
     * Use the recursive method to dispense the cash.
     * This algorithm works as below:
     * - first try to supply the full amount with the highest cash supply only.
     * - if successful, then just return this cash supply.
     * - if not successful, then see if it could get one highest cash
     * - if possible withdraw one highest cash, update the withdraw cash amount and call this method again.
     * - if not possible, remove the highest cash supply from cashSupplyList, and call this method again.
     *
     * @param cashAmount     Required dispense cash amount
     * @param cashSupplyList Cash supplies ordered by cash value desc.
     * @param dispenseResult Used to store the dispense cash result.
     * @return the left cash amount
     * @throws CashSupplyException handles three different scenario
     *                             - There is no cash in the machine
     *                             - The existing cash supply could not meet the required cash amount
     *                             - The required cash amount is over the daily withdraw limitation
     */
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

    /**
     * Check the cash stock after dispensing the cash. If the stock is lower than the specific cash supply standard, then send the notification via email/text or call another endpoint
     *
     * @param cashSupplyList The list of the cash supplies after dispensing the cash.
     */
    private void checkCashStock(List<CashSupply> cashSupplyList) {
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

    /**
     * It should get the bank withdraw limitation from somewhere else
     *
     * @return The current bank account limitation
     */
    //TODO need to get this limitation from somewhere else
    public int getAccountCashWithdrawLimitation() {
        return 1000;
    }

    /**
     * Try to supply with the specific cash supply
     *
     * @param amount         The withdraw cash amount
     * @param cashSupply     The specific cash supply
     * @param dispenseResult The result map which is used to store the dispense cash supply
     * @return
     */
    private boolean supplyWithOneCash(Integer amount, CashSupply cashSupply, Map<CashType, CashSupply> dispenseResult) {
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

    /**
     * Add the cash supply to the dispense result map.
     *
     * @param dispenseCashType     The selected cash type.
     * @param cashWithdrawQuantity The cash withdraw quantity for the selected cash type
     * @param dispenseResult       The result map which is used to store the dispense cash supply
     */
    private void addDispenseCashSupply(CashType dispenseCashType, Integer cashWithdrawQuantity, Map<CashType, CashSupply> dispenseResult) {
        if (dispenseResult.containsKey(dispenseCashType)) {
            CashSupply dispenseCashSupply = dispenseResult.get(dispenseCashType);
            dispenseCashSupply.add(cashWithdrawQuantity);
        } else {
            CashSupply withdrawCashSupply = new CashSupply(dispenseCashType, cashWithdrawQuantity);
            dispenseResult.put(dispenseCashType, withdrawCashSupply);
        }
    }

    /**
     * It should get the initialization cash supplies from somewhere else. Now just use the dummy map.
     *
     * @return A map of the initialized cash supplies.
     */
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

    /**
     * It should get the low stock standard setting somewhere.
     *
     * @return A map of the standard of the low stock cash supplies
     */
    private Map<Integer, Integer> getDummyCashLowStockStandard() {
        Map<Integer, Integer> dummyCashLowSupplyStandard = new HashMap<>();

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
