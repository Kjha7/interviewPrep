package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.dto.request.LoginRequest;
import com.tekion.interview.stockTradingPlatform.dto.request.RegisterUserRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.LoginResponse;
import com.tekion.interview.stockTradingPlatform.dto.response.RegisterUserResponse;
import com.tekion.interview.stockTradingPlatform.entity.UserAccount;
import com.tekion.interview.stockTradingPlatform.entity.UserCredentials;
import com.tekion.interview.stockTradingPlatform.entity.Wallet;
import com.tekion.interview.stockTradingPlatform.entity.embeddable.KycDetails;
import com.tekion.interview.stockTradingPlatform.entity.enums.UserStatus;
import com.tekion.interview.stockTradingPlatform.exception.InvalidCredentialsException;
import com.tekion.interview.stockTradingPlatform.exception.UsernameTakenException;
import com.tekion.interview.stockTradingPlatform.repository.UserAccountRepository;
import com.tekion.interview.stockTradingPlatform.repository.UserCredentialsRepository;
import com.tekion.interview.stockTradingPlatform.repository.WalletRepository;
import com.tekion.interview.stockTradingPlatform.service.TokenService;
import com.tekion.interview.stockTradingPlatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userAccountRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new UsernameTakenException(request.getUsername());
        }
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new UsernameTakenException(request.getEmail()); // reuse: still a uniqueness conflict
        }

        UserAccount user = UserAccount.builder()
                .username(request.getUsername())
                .name(request.getName())
                .dob(request.getDob())
                .email(request.getEmail())
                .address(request.getAddress())
                .contactDetails(request.getContactDetails())
                .kycInfo(new KycDetails(
                        request.getKycDetails().getPanNumber(),
                        request.getKycDetails().getAadhaarLast4(),
                        "PENDING_VERIFICATION"))
                .status(UserStatus.PENDING_KYC_VERIFICATION)
                .build();
        user = userAccountRepository.save(user);

        UserCredentials credentials = UserCredentials.builder()
                .userId(user.getId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .failedLoginAttempts(0)
                .build();
        userCredentialsRepository.save(credentials);

        // Every new user gets a zero-balance wallet up front - buy orders fail on insufficient
        // funds naturally rather than needing a null-wallet special case later.
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balanceMinorUnits(0L)
                .reservedMinorUnits(0L)
                .currency("INR")
                .build();
        walletRepository.save(wallet);

        return RegisterUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        UserCredentials credentials = userCredentialsRepository.findById(user.getId())
                .orElseThrow(InvalidCredentialsException::new);

        if (credentials.getLockedUntil() != null && credentials.getLockedUntil().isAfter(Instant.now())) {
            throw new InvalidCredentialsException(); // TODO: distinct 423 ACCOUNT_LOCKED response
        }

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            credentials.setFailedLoginAttempts(credentials.getFailedLoginAttempts() + 1);
            // TODO: after N attempts, set lockedUntil = now + backoff window
            userCredentialsRepository.save(credentials);
            throw new InvalidCredentialsException();
        }

        credentials.setFailedLoginAttempts(0);
        userCredentialsRepository.save(credentials);

        return tokenService.issueTokenPair(user.getId(), user.getUsername());
    }
}
