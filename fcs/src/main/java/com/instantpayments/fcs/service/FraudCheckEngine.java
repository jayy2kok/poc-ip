package com.instantpayments.fcs.service;

import com.instantpayments.fcs.config.BlacklistProperties;
import com.instantpayments.fcs.model.FraudCheckRequest;
import com.instantpayments.fcs.model.FraudCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Core fraud check engine. Performs case-insensitive blacklist matching
 * against payer/payee names, countries, banks, and payment instructions.
 */
@Service
public class FraudCheckEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckEngine.class);

    private final BlacklistProperties blacklist;

    public FraudCheckEngine(BlacklistProperties blacklist) {
        this.blacklist = blacklist;
    }

    public FraudCheckResponse check(FraudCheckRequest request) {
        log.info("Checking transaction {} against blacklists", request.getTransactionId());

        // Check names
        if (matchesAny(request.getPayer().getName(), blacklist.getNames()) ||
            matchesAny(request.getPayee().getName(), blacklist.getNames())) {
            return reject(request.getTransactionId(), "Suspicious payment");
        }

        // Check countries
        if (matchesAny(request.getPayer().getCountryCode(), blacklist.getCountries()) ||
            matchesAny(request.getPayee().getCountryCode(), blacklist.getCountries())) {
            return reject(request.getTransactionId(), "Suspicious payment");
        }

        // Check banks
        if (matchesAny(request.getPayer().getBank(), blacklist.getBanks()) ||
            matchesAny(request.getPayee().getBank(), blacklist.getBanks())) {
            return reject(request.getTransactionId(), "Suspicious payment");
        }

        // Check payment instruction
        if (request.getPaymentInstruction() != null &&
            matchesAny(request.getPaymentInstruction(), blacklist.getPaymentInstructions())) {
            return reject(request.getTransactionId(), "Suspicious payment");
        }

        log.info("Transaction {} passed all blacklist checks", request.getTransactionId());
        return approve(request.getTransactionId());
    }

    private boolean matchesAny(String value, List<String> blacklistedValues) {
        if (value == null || blacklistedValues == null) return false;
        return blacklistedValues.stream()
                .anyMatch(bl -> bl.equalsIgnoreCase(value));
    }

    private FraudCheckResponse reject(String transactionId, String message) {
        log.warn("Transaction {} REJECTED: {}", transactionId, message);
        FraudCheckResponse response = new FraudCheckResponse();
        response.setTransactionId(transactionId);
        response.setOutcome("REJECTED");
        response.setMessage(message);
        response.setCheckedAt(Instant.now());
        return response;
    }

    private FraudCheckResponse approve(String transactionId) {
        FraudCheckResponse response = new FraudCheckResponse();
        response.setTransactionId(transactionId);
        response.setOutcome("APPROVED");
        response.setMessage("Nothing found, all okay");
        response.setCheckedAt(Instant.now());
        return response;
    }
}
