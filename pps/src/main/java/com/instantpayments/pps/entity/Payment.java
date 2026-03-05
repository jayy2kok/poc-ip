package com.instantpayments.pps.entity;

import com.instantpayments.common.dto.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "payer_name", nullable = false)
    private String payerName;

    @Column(name = "payer_bank", nullable = false)
    private String payerBank;

    @Column(name = "payer_country", nullable = false, length = 3)
    private String payerCountry;

    @Column(name = "payer_account", nullable = false, length = 50)
    private String payerAccount;

    @Column(name = "payee_name", nullable = false)
    private String payeeName;

    @Column(name = "payee_bank", nullable = false)
    private String payeeBank;

    @Column(name = "payee_country", nullable = false, length = 3)
    private String payeeCountry;

    @Column(name = "payee_account", nullable = false, length = 50)
    private String payeeAccount;

    @Column(name = "payment_instruction")
    private String paymentInstruction;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "fraud_message")
    private String fraudMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // --- Getters and Setters ---

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }

    public String getPayerBank() { return payerBank; }
    public void setPayerBank(String payerBank) { this.payerBank = payerBank; }

    public String getPayerCountry() { return payerCountry; }
    public void setPayerCountry(String payerCountry) { this.payerCountry = payerCountry; }

    public String getPayerAccount() { return payerAccount; }
    public void setPayerAccount(String payerAccount) { this.payerAccount = payerAccount; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getPayeeBank() { return payeeBank; }
    public void setPayeeBank(String payeeBank) { this.payeeBank = payeeBank; }

    public String getPayeeCountry() { return payeeCountry; }
    public void setPayeeCountry(String payeeCountry) { this.payeeCountry = payeeCountry; }

    public String getPayeeAccount() { return payeeAccount; }
    public void setPayeeAccount(String payeeAccount) { this.payeeAccount = payeeAccount; }

    public String getPaymentInstruction() { return paymentInstruction; }
    public void setPaymentInstruction(String paymentInstruction) { this.paymentInstruction = paymentInstruction; }

    public LocalDate getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getFraudMessage() { return fraudMessage; }
    public void setFraudMessage(String fraudMessage) { this.fraudMessage = fraudMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
