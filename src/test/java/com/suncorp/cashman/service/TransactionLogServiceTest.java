package com.suncorp.cashman.service;

import com.suncorp.cashman.Application;
import com.suncorp.cashman.H2JpaConfig;
import com.suncorp.cashman.domain.CashSupply;
import com.suncorp.cashman.domain.CashType;
import com.suncorp.cashman.domain.TransactionLog;
import com.suncorp.cashman.domain.TransactionLogDetail;
import com.suncorp.cashman.exception.CashSupplyException;
import com.suncorp.cashman.repository.CashSupplyRepository;
import com.suncorp.cashman.repository.CashTypeRepository;
import com.suncorp.cashman.repository.TransactionLogDetailRepository;
import com.suncorp.cashman.repository.TransactionLogRepository;
import org.apache.commons.lang.StringUtils;
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
public class TransactionLogServiceTest {
    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private TransactionLogDetailRepository transactionLogDetailRepository;

    @Autowired
    private CashTypeRepository cashTypeRepository;

    private TransactionLogService transactionLogService;

    @Before
    public void setup() {
        transactionLogService = new TransactionLogServiceImpl(transactionLogRepository, transactionLogDetailRepository);
    }

    @Test
    public void saveTransactionTest() throws Exception {
        // test save transaction log and log detail.
        Map<CashType, CashSupply> dispenseCashMap = new HashMap<>();

        CashType cashType = new CashType("$100", 100);
        cashTypeRepository.save(cashType);
        dispenseCashMap.put(cashType, new CashSupply(cashType, 5));

        cashType = new CashType("$50", 50);
        cashTypeRepository.save(cashType);
        dispenseCashMap.put(cashType, new CashSupply(cashType, 6));

        cashType = new CashType("$20", 20);
        cashTypeRepository.save(cashType);
        dispenseCashMap.put(cashType, new CashSupply(cashType, 7));

        transactionLogService.saveTransactionLog(dispenseCashMap, 940);

        List<TransactionLog> transactionLogList = transactionLogRepository.findAll();
        assertThat(transactionLogList.size(), is(1));

        TransactionLog transactionLog = transactionLogList.get(0);
        assertThat(transactionLog.getAccountName(), is("James"));
        assertThat(transactionLog.getAccountNo(), is("12345678"));
        assertThat(transactionLog.getBsb(), is("111111"));
        assertThat(transactionLog.getTotalAmount(), is(940));

        List<TransactionLogDetail> detailList = transactionLogDetailRepository.findAll();
        assertThat(detailList.size(), is(3));

        detailList.forEach(logDetail -> {
            assertThat(logDetail.getTransactionLog().getTransactionId(), is(transactionLog.getTransactionId()));

            if (logDetail.getCashValue() == 100) {
                assertThat(logDetail.getQuality(), is(5));
            } else if (logDetail.getCashValue() == 50) {
                assertThat(logDetail.getQuality(), is(6));
            } else if (logDetail.getCashValue() == 20) {
                assertThat(logDetail.getQuality(), is(7));
            }
        });
    }
}
