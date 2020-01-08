package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static com.pluralsight.pension.setup.AccountOpeningService.UNACCEPTABLE_RISK_PROFILE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Account Opening service should ....")
class AccountOpeningServiceTest {

    public static final String FIRST_NAME = "Rupesh";
    public static final String LAST_NAME = "KUmar";
    public static final String TAX_ID = "123";
    public static final LocalDate LOCAL_DATE = LocalDate.of(1977, 04, 11);
    public static final String ACCT_ID = "1234";

    AccountOpeningService accountOpeningService;
    BackgroundCheckService backgroundCheckService = mock(BackgroundCheckService.class);
    ReferenceIdsManager referenceIdsManager = mock(ReferenceIdsManager.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    AccountOpeningEventPublisher accountOpeningEventPublisher = mock(AccountOpeningEventPublisher.class);

    @BeforeEach
    void setUp() {
        accountOpeningService = new AccountOpeningService(backgroundCheckService, referenceIdsManager, accountRepository, accountOpeningEventPublisher);
    }

    @Test
    @DisplayName("Open Accounts.should not be succesfull..")
    void openAccountTest() throws Exception {
        AccountOpeningStatus accountOpeningStatus = accountOpeningService.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE);
        assertNotNull(accountOpeningStatus);
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);

    }

    @Test
    @DisplayName("Should Decline Account opening when background check returns unacceptable..")
    void shouldDeclineAccount() throws Exception {
        when(backgroundCheckService.
                confirm(FIRST_NAME, "Kumar", TAX_ID, LOCAL_DATE))
                .thenReturn(new BackgroundCheckResults(UNACCEPTABLE_RISK_PROFILE, 100));

        AccountOpeningStatus accountOpeningStatus = accountOpeningService.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE);
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);

    }

    @Test
    @DisplayName("Should Open Account..")
    void shouldOpenAccount() throws Exception {
        BackgroundCheckResults backgroundCheckResults = new BackgroundCheckResults("SUCCESS", 100);
        when(backgroundCheckService.
                confirm(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE))
                .thenReturn(backgroundCheckResults);

        when(referenceIdsManager.obtainId(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE)).thenReturn(ACCT_ID);
        AccountOpeningStatus accountOpeningStatus = accountOpeningService.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE);
        assertEquals(AccountOpeningStatus.OPENED, accountOpeningStatus);
        verify(accountRepository).save(ACCT_ID, FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE,backgroundCheckResults );
        verify(accountOpeningEventPublisher).publishEvent(ACCT_ID);

    }

    @Test
    @DisplayName("Should not open account when Background check returns nulll")
    void shouldNotOpenAccountWhenBackGroundCheckReturnNull() throws Exception {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE))
                .thenReturn(null);
        assertEquals(AccountOpeningStatus.DECLINED,
                accountOpeningService.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE));
    }

    @Test
    void shouldNotOpenAccountWhenBackgroundCheckServiceThrowsException() throws Exception {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE))
                .thenThrow(new IOException());
        assertThrows(IOException.class, () -> accountOpeningService.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE));
    }

    @Test
    void shouldNotOpenAccountWhenReferenceServiceThrowsError() throws Exception {

        when(backgroundCheckService.
                confirm(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE))
                .thenReturn(new BackgroundCheckResults("SUCCESS", 100));

        when(referenceIdsManager.obtainId(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE))
                .thenThrow(new RuntimeException());


        assertThrows(RuntimeException.class, () -> accountOpeningService.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, LOCAL_DATE));

    }


}