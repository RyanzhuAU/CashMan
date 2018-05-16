package com.suncorp.cashman.service;

import com.suncorp.cashman.Application;
import com.suncorp.cashman.H2JpaConfig;
import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import com.suncorp.cashman.exception.CashSupplyException;
import com.suncorp.cashman.repository.CashSupplyRepository;
import com.suncorp.cashman.repository.CashTypeRepository;
import com.suncorp.cashman.repository.TransactionLogDetailRepository;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ryan.zhu on 14/05/2018.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, H2JpaConfig.class})
public class CashServiceTest {
    @Autowired
    private CashSupplyRepository cashSupplyRepository;

    @Autowired
    private CashTypeRepository cashTypeRepository;

    @Autowired
    private TransactionLogDetailRepository transactionLogDetailRepository;

    private CashService cashService;

    @Before
    public void setup() {
        cashService = new CashServiceImpl(cashSupplyRepository, cashTypeRepository);

        transactionLogDetailRepository.deleteAllInBatch();
        cashSupplyRepository.deleteAllInBatch();
        cashTypeRepository.deleteAllInBatch();

        CashType cashType = new CashType("$100", 100);
        cashTypeRepository.save(cashType);
        CashSupply cashSupply = new CashSupply(cashType, 2);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$50", 50);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 3);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$20", 20);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 4);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$10", 10);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 5);
        cashSupplyRepository.save(cashSupply);
    }

    @Test
    public void dispenseCashTest() throws Exception {
        // test the scenario - can withdraw with one note/coin
        Map<CashType, CashSupply> result = cashService.dispenseCash(200);

        result.forEach((cashType, cashSupply) -> {
            assertThat(cashSupply.getCashType(), is(cashType));

            if (StringUtils.equals(cashType.getCashDesc(), "100")) {
                assertThat(cashSupply.getCashQuantity(), is(2));

                CashSupply newCashSupply = cashSupplyRepository.findByCashTypeEquals(cashType);
                assertThat(newCashSupply.getCashQuantity(), is(0));
            }
        });

        // test the scenario - need to withdraw with different note/coin
        result = cashService.dispenseCash(90);

        result.forEach((cashType, cashSupply) -> {
            assertThat(cashSupply.getCashType(), is(cashType));

            if (StringUtils.equals(cashType.getCashDesc(), "50")) {
                assertThat(cashSupply.getCashQuantity(), is(1));

                CashSupply newCashSupply = cashSupplyRepository.findByCashTypeEquals(cashType);
                assertThat(newCashSupply.getCashQuantity(), is(2));
            } else if (StringUtils.equals(cashType.getCashDesc(), "20")) {
                assertThat(cashSupply.getCashQuantity(), is(2));

                CashSupply newCashSupply = cashSupplyRepository.findByCashTypeEquals(cashType);
                assertThat(newCashSupply.getCashQuantity(), is(2));
            }
        });

        // test the scenario - the cash stock cannot meet the required cash amount
        try {
            cashService.dispenseCash(200);
        } catch (CashSupplyException e) {
            int amountRequired = e.getAmountRequired();
            int amountSupplied = e.getAmountSupplied();

            assertThat(amountRequired, is(200));
            assertThat(amountSupplied, is(190));
            assertThat(e.getMessage(), is("Sorry, this ATM cannot supply the amount required $" + amountRequired + " with current stock. " +
                    "The closest amount that can be supplied is $" + amountSupplied + ". Please try again later."));
        }

        // test the scenario - cannot withdraw with existing cash supply
        try {
            cashService.dispenseCash(25);
        } catch (CashSupplyException e) {
            int amountRequired = e.getAmountRequired();
            int amountSupplied = e.getAmountSupplied();

            assertThat(amountRequired, is(25));
            assertThat(amountSupplied, is(20));
            assertThat(e.getMessage(), is("Sorry, this ATM cannot supply the amount required $" + amountRequired + " with current stock. " +
                    "The closest amount that can be supplied is $" + amountSupplied + ". Please try again later."));
        }

        // test the scenario - over the withdraw daily limitation
        int limitation = cashService.getAccountCashWithdrawLimitation();
        try {
            cashService.dispenseCash(limitation + 1);
        } catch (CashSupplyException e) {
            int amountRequired = e.getAmountRequired();
            int amountSupplied = e.getAmountSupplied();

            assertThat(amountRequired, is(limitation + 1));
            assertThat(amountSupplied, is(limitation));
            assertThat(e.getMessage(), is("Sorry, the amount $" + amountRequired + " is over your withdraw limitation. The amount you can withdraw is $" + amountSupplied + " today."));
        }
    }

    @Test
    public void getCurrentCashSuppliesTest() {
        List<CashSupply> currentCashSupplyList = cashService.getCurrentCashSupplies();

        List<CashSupply> cashSupplyListFromDb = cashSupplyRepository.findAll();
        Map<Integer, Integer> cashSupplyMap = new HashMap<>();
        cashSupplyListFromDb.forEach(cashSupply -> {
            cashSupplyMap.put(cashSupply.getCashType().getCashValue(), cashSupply.getCashQuantity());
        });

        currentCashSupplyList.forEach(cashSupply -> {
            assertThat(cashSupply.getCashQuantity(), is(cashSupplyMap.get(cashSupply.getCashType().getCashValue())));
        });
    }

    @Test
    public void initializationTest() {
        cashService.initializeCashMachine();

        List<CashSupply> currentCashSupplyList = cashService.getCurrentCashSupplies();
        currentCashSupplyList.forEach(cashSupply -> {
            if (cashSupply.getCashType().getCashValue() == 100) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(10));
            } else if (cashSupply.getCashType().getCashValue() == 50) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(20));
            } else if (cashSupply.getCashType().getCashValue() == 20) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(30));
            } else if (cashSupply.getCashType().getCashValue() == 10) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(40));
            } else if (cashSupply.getCashType().getCashValue() == 5) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(50));
            } else if (cashSupply.getCashType().getCashValue() == 10) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(60));
            } else if (cashSupply.getCashType().getCashValue() == 10) {
                MatcherAssert.assertThat(cashSupply.getCashQuantity(), is(70));
            }
        });
    }
}
