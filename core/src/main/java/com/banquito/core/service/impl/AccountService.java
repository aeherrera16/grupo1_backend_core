package com.banquito.core.service.impl;

import com.banquito.core.dto.AccountRequestDTO;
import com.banquito.core.dto.AccountResponseDTO;
import com.banquito.core.dto.BalanceDTO;
import com.banquito.core.dto.TransactionResponseDTO;
import com.banquito.core.enums.AccountStatusEnum;
import com.banquito.core.exception.AccountNotFoundException;
import com.banquito.core.exception.DuplicateTransactionException;
import com.banquito.core.model.Account;
import com.banquito.core.model.Branch;
import com.banquito.core.model.Customer;
import com.banquito.core.repository.AccountRepository;
import com.banquito.core.repository.AccountSubtypeRepository;
import com.banquito.core.repository.AccountTransactionRepository;
import com.banquito.core.repository.BranchRepository;
import com.banquito.core.repository.CustomerRepository;
import com.banquito.core.service.IAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements IAccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final AccountSubtypeRepository accountSubtypeRepository;
    private final AccountTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    @Override
    public List<AccountResponseDTO> findAll(Integer coreUserId) {
        return accountRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public AccountResponseDTO findByAccountNumber(String accountNumber, Integer coreUserId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AccountResponseDTO> findByCustomerId(Integer customerId, Integer coreUserId) {
        return accountRepository.findByCustomer_Id(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public AccountResponseDTO create(AccountRequestDTO request, Integer coreUserId) {
        com.banquito.core.model.Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + request.getCustomerId()));
        com.banquito.core.model.Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + request.getBranchId()));
        com.banquito.core.model.AccountSubtype subtype = accountSubtypeRepository.findById(request.getAccountSubtypeId())
                .orElseThrow(() -> new RuntimeException("Subtipo no encontrado: " + request.getAccountSubtypeId()));

        BigDecimal initialBalance = request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO;
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }

        LocalDateTime now = LocalDateTime.now();
        Account account = new Account();
        
        String accountNumber = resolveAccountNumber(request.getAccountNumber(), branch);
        account.setAccountNumber(accountNumber);
        
        account.setCustomer(customer);
        account.setBranch(branch);
        account.setAccountSubtype(subtype);
        account.setStatus(AccountStatusEnum.ACTIVO);
        account.setAccountingBalance(initialBalance);
        account.setAvailableBalance(initialBalance);
        account.setIsFavorite(Boolean.TRUE.equals(request.getIsFavorite()));
        account.setOpeningDate(now);
        account.setLastUpdate(now);

        return toResponse(accountRepository.save(account));
    }

    @Transactional
    @Override
    public AccountResponseDTO activate(String accountNumber, Integer coreUserId) {
        return changeStatus(accountNumber, AccountStatusEnum.ACTIVO, coreUserId);
    }

    @Transactional
    @Override
    public AccountResponseDTO inactivate(String accountNumber, Integer coreUserId) {
        return changeStatus(accountNumber, AccountStatusEnum.INACTIVO, coreUserId);
    }

    @Transactional
    @Override
    public AccountResponseDTO block(String accountNumber, Integer coreUserId) {
        return changeStatus(accountNumber, AccountStatusEnum.BLOQUEADO, coreUserId);
    }

    @Transactional
    @Override
    public AccountResponseDTO suspend(String accountNumber, Integer coreUserId) {
        return changeStatus(accountNumber, AccountStatusEnum.SUSPENDIDO, coreUserId);
    }

    private AccountResponseDTO changeStatus(String accountNumber, AccountStatusEnum status, Integer coreUserId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        account.setStatus(status);
        account.setLastUpdate(LocalDateTime.now());
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    @Override
    public BalanceDTO getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return new BalanceDTO(
                account.getAccountNumber(),
                account.getAccountingBalance(),
                account.getAvailableBalance(),
                account.getStatus());
    }

    @Transactional(readOnly = true)
    @Override
    public List<TransactionResponseDTO> getTransactions(String accountNumber, Integer limit) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return transactionRepository.findTop10ByAccount_IdOrderByTransactionDateDesc(account.getId())
                .stream()
                .map(t -> new TransactionResponseDTO(
                        t.getId(),
                        t.getAccount().getAccountNumber(),
                        t.getMovementType(),
                        t.getAmount(),
                        t.getResultingBalance(),
                        t.getTransactionDate(),
                        t.getTransactionUuid(),
                        t.getStatus(),
                        t.getDescription()
                ))
                .collect(Collectors.toList());
    }

    private String resolveAccountNumber(String requestedAccountNumber, com.banquito.core.model.Branch branch) {
        if (requestedAccountNumber != null && !requestedAccountNumber.isBlank()) {
            if (!requestedAccountNumber.startsWith(branch.getBranchCode() + "-")) {
                throw new IllegalArgumentException("El numero de cuenta debe iniciar con el codigo de la sucursal");
            }
            return requestedAccountNumber;
        }
        return branch.getBranchCode() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    @Transactional
    @Override
    public AccountResponseDTO setFavorite(String accountNumber) {
        Account accountToFavorite = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        accountRepository.findByIsFavoriteTrue()
                .ifPresent(currentFavorite -> {
                    currentFavorite.setIsFavorite(false);
                    accountRepository.save(currentFavorite);
                });

        accountToFavorite.setIsFavorite(true);
        Account saved = accountRepository.save(accountToFavorite);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public AccountResponseDTO getFavoriteAccount() {
        Account account = accountRepository.findByIsFavoriteTrue()
                .orElseThrow(() -> new AccountNotFoundException("No existe cuenta favorita configurada"));
        return toResponse(account);
    }

    private AccountResponseDTO toResponse(Account account) {
        String customerName = resolveCustomerName(account.getCustomer());
        return new AccountResponseDTO(
                account.getId(),
                account.getAccountNumber(),
                customerName,
                account.getBranch().getName(),
                account.getAccountSubtype().getDescription(),
                account.getStatus(),
                account.getAccountingBalance(),
                account.getAvailableBalance(),
                account.getIsFavorite(),
                account.getOpeningDate());
    }

    private String resolveCustomerName(com.banquito.core.model.Customer customer) {
        if (customer.getLegalName() != null && !customer.getLegalName().isBlank()) {
            return customer.getLegalName();
        }
        return ((customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                (customer.getLastName() != null ? customer.getLastName() : "")).trim();
    }
}
