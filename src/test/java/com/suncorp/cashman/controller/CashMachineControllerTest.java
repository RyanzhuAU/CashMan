package com.suncorp.cashman.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suncorp.cashman.Application;
import com.suncorp.cashman.H2JpaConfig;
import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import com.suncorp.cashman.repository.CashSupplyRepository;
import com.suncorp.cashman.repository.CashTypeRepository;
import com.suncorp.cashman.repository.TransactionLogDetailRepository;
import com.suncorp.cashman.representation.CashSupplyRep;
import com.suncorp.cashman.service.CashService;
import com.suncorp.cashman.service.CashServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by ryan.zhu on 7/4/2018.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, H2JpaConfig.class})
@AutoConfigureMockMvc
public class CashMachineControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CashTypeRepository cashTypeRepository;

    @Autowired
    private CashSupplyRepository cashSupplyRepository;

    @Autowired
    private TransactionLogDetailRepository transactionLogDetailRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CashService cashService;

    @Before
    public void setup() throws Exception {
        cashService = new CashServiceImpl(this.cashSupplyRepository, this.cashTypeRepository);

        transactionLogDetailRepository.deleteAllInBatch();
        cashSupplyRepository.deleteAllInBatch();
        cashTypeRepository.deleteAllInBatch();

        CashType cashType = new CashType("$100", 100);
        cashTypeRepository.save(cashType);
        CashSupply cashSupply = new CashSupply(cashType, 2);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$50", 50);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 2);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$20", 20);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 3);
        cashSupplyRepository.save(cashSupply);

        cashType = new CashType("$10", 10);
        cashTypeRepository.save(cashType);
        cashSupply = new CashSupply(cashType, 4);
        cashSupplyRepository.save(cashSupply);
    }

    @Test
    public void dispenseCashControllerTest() throws Exception {
        // test normal scenario
        MvcResult result = this.mockMvc.perform(get("/cashMachine/dispenseCash/100"))
                            .andExpect(status().isOk())
                            .andReturn();

        String content = result.getResponse().getContentAsString();

        List<CashSupplyRep> cashSupplyRepList = objectMapper.readValue(content, new TypeReference<List<CashSupplyRep>>(){});

        assertThat(cashSupplyRepList.size(), is(1));

        CashSupplyRep cashSupplyRep = cashSupplyRepList.get(0);
        assertThat(cashSupplyRep.getCashDesc(), is("$100"));
        assertThat(cashSupplyRep.getCashQuantity(), is(1));
        assertThat(cashSupplyRep.getCashValue(), is(100));

        //test the over daily withdraw limitation scenario
        int withdrawLimitation = cashService.getAccountCashWithdrawLimitation();
        result = this.mockMvc.perform(get("/cashMachine/dispenseCash/" + (withdrawLimitation + 1)))
                .andExpect(status().isBadRequest())
                .andReturn();

        content = result.getResponse().getContentAsString();
        assertThat(content, is("Sorry, the amount $" + (withdrawLimitation + 1) + " is over your withdraw limitation. The amount you can withdraw is $" + withdrawLimitation + " today."));

        // test the cash out of stock scenario
        result = this.mockMvc.perform(get("/cashMachine/dispenseCash/350"))
                .andExpect(status().isBadRequest())
                .andReturn();

        content = result.getResponse().getContentAsString();
        assertThat(content, is("Sorry, this ATM cannot supply the amountRequired $350 with current stock. The closest amount that can be supplied is $300. Please try again later."));

        // test the check cash stock endpoint
        result = this.mockMvc.perform(get("/cashMachine/getCashStock"))
                .andExpect(status().isOk())
                .andReturn();

        content = result.getResponse().getContentAsString();
        cashSupplyRepList = objectMapper.readValue(content, new TypeReference<List<CashSupplyRep>>(){});

        cashSupplyRepList.forEach(cashRep -> {
            if (cashRep.getCashValue() == 100) {
                assertThat(cashRep.getCashQuantity(), is(1));
            } else if (cashRep.getCashValue() == 50) {
                assertThat(cashRep.getCashQuantity(), is(2));
            } else if (cashRep.getCashValue() == 20) {
                assertThat(cashRep.getCashQuantity(), is(3));
            } else if (cashRep.getCashValue() == 10) {
                assertThat(cashRep.getCashQuantity(), is(4));
            }
        });

        // test the initialization endpoint
        this.mockMvc.perform(post("/cashMachine/initialize"))
                .andExpect(status().isOk())
                .andReturn();

        List<CashSupply> cashSupplyList = cashService.getCurrentCashSupplies();
        cashSupplyList.forEach(cashSupply -> {
            if (cashSupply.getCashType().getCashValue() == 100) {
                assertThat(cashSupply.getCashQuantity(), is(10));
            } else if (cashSupply.getCashType().getCashValue() == 50) {
                assertThat(cashSupply.getCashQuantity(), is(20));
            } else if (cashSupply.getCashType().getCashValue() == 20) {
                assertThat(cashSupply.getCashQuantity(), is(30));
            } else if (cashSupply.getCashType().getCashValue() == 10) {
                assertThat(cashSupply.getCashQuantity(), is(40));
            } else if (cashSupply.getCashType().getCashValue() == 5) {
                assertThat(cashSupply.getCashQuantity(), is(50));
            } else if (cashSupply.getCashType().getCashValue() == 10) {
                assertThat(cashSupply.getCashQuantity(), is(60));
            } else if (cashSupply.getCashType().getCashValue() == 10) {
                assertThat(cashSupply.getCashQuantity(), is(70));
            }
        });
    }
}
