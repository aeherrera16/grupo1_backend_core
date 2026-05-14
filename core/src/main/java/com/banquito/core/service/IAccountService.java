package com.banquito.core.service;

import java.util.List;
import com.banquito.core.dto.AccountRequestDTO;
import com.banquito.core.dto.AccountResponseDTO;
import com.banquito.core.dto.BalanceDTO;
import com.banquito.core.dto.TransactionResponseDTO;

public interface IAccountService {

    List<AccountResponseDTO> findAll(Integer coreUserId);

    AccountResponseDTO findByAccountNumber(String accountNumber, Integer coreUserId);

    List<AccountResponseDTO> findByCustomerId(Integer customerId, Integer coreUserId);

    AccountResponseDTO create(AccountRequestDTO request, Integer coreUserId);

    AccountResponseDTO activate(String accountNumber, Integer coreUserId);

    AccountResponseDTO inactivate(String accountNumber, Integer coreUserId);

    AccountResponseDTO block(String accountNumber, Integer coreUserId);

    AccountResponseDTO suspend(String accountNumber, Integer coreUserId);

    BalanceDTO getBalance(String accountNumber);

    List<TransactionResponseDTO> getTransactions(String accountNumber, Integer limit);

    AccountResponseDTO getFavoriteAccount();

    AccountResponseDTO setFavorite(String accountNumber);
}
