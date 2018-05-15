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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
        CashSupply cashSupply = new CashSupply(cashType, 10);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$50", 50);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 10);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$20", 20);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 10);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$10", 10);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 10);
        cashSupplyRepository.save(cashSupply);
    }

    @Test
    public void dispenseCashTest() throws Exception{
        // test the scenario - can withdraw with one note/coin
        Map<CashType, CashSupply> result = cashService.dispenseCash(300);

        result.forEach((cashType, cashSupply) -> {
            assertThat(cashSupply.getCashType(), is(cashType));

            if (StringUtils.equals(cashType.getCashDesc(), "100")) {
                assertThat(cashSupply.getCashQuantity(), is(3));

                CashSupply newCashSupply = cashSupplyRepository.findByCashTypeEquals(cashType);
                assertThat(newCashSupply.getCashQuantity(), is(7));
            }
        });

        // test the scenario - need to withdraw with different note/coin
        result = cashService.dispenseCash(150);

        result.forEach((cashType, cashSupply) -> {
            assertThat(cashSupply.getCashType(), is(cashType));

            if (StringUtils.equals(cashType.getCashDesc(), "100")) {
                assertThat(cashSupply.getCashQuantity(), is(1));

                CashSupply newCashSupply = cashSupplyRepository.findByCashTypeEquals(cashType);
                assertThat(newCashSupply.getCashQuantity(), is(6));
            } else if (StringUtils.equals(cashType.getCashDesc(), "50")) {
                assertThat(cashSupply.getCashQuantity(), is(1));

                CashSupply newCashSupply = cashSupplyRepository.findByCashTypeEquals(cashType);
                assertThat(newCashSupply.getCashQuantity(), is(9));
            }
        });

        // test the scenario - cannot withdraw with existing cash supply
        try {
            cashService.dispenseCash(165);
        } catch (CashSupplyException e) {
            int amountRequired = e.getAmountRequired();
            int amountSupplied = e.getAmountSupplied();

            assertThat(amountRequired, is(165));
            assertThat(amountSupplied, is(160));
            assertThat(e.getMessage(), is("Sorry, this ATM cannot supply the amountRequired $" + amountRequired + " with current stock. " +
                    "The closest amount that can be supplied is $" + amountSupplied + ". Please try again later."));
        }

        int limitation = cashService.getAccountCashWithdrawLimitation();
        try {
            cashService.dispenseCash(limitation + 1);
        } catch (CashSupplyException e) {
            int amountRequired = e.getAmountRequired();
            int amountSupplied = e.getAmountSupplied();

            assertThat(amountRequired, is(limitation + 1));
            assertThat(amountSupplied, is(limitation));
            assertThat(e.getMessage(), is("Sorry, the amount " + amountRequired + " is over your withdraw limitation. The amount you can withdraw is " + amountSupplied + " today."));
        }
    }
}
