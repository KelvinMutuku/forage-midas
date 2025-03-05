package com.jpmc.midascore.kafka;

import com.jpmc.midascore.model.*;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TransactionListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    @KafkaListener(topics = "${kafka.topic.transactions}", groupId = "midas-core-group")
    @Transactional
    public void listen(ConsumerRecord<String, Transaction> record) {
        Transaction transaction = record.value();

        Optional<User> senderOpt = userRepository.findByUsername(transaction.getSenderId());
        Optional<User> recipientOpt = userRepository.findByUsername(transaction.getRecipientId());

        if (senderOpt.isPresent() && recipientOpt.isPresent()) {
            User sender = senderOpt.get();
            User recipient = recipientOpt.get();
            BigDecimal amount = transaction.getAmount();

            if (sender.getBalance().compareTo(amount) >= 0) {
                sender.setBalance(sender.getBalance().subtract(amount));
                recipient.setBalance(recipient.getBalance().add(amount));

                // Call Incentive API
                HttpEntity<Transaction> request = new HttpEntity<>(transaction);
                ResponseEntity<Incentive> response = restTemplate.exchange(
                    INCENTIVE_API_URL, HttpMethod.POST, request, Incentive.class);
                
                BigDecimal incentiveAmount = response.getBody().getAmount();
                recipient.setBalance(recipient.getBalance().add(incentiveAmount));

                // Save updated balances
                userRepository.save(sender);
                userRepository.save(recipient);

                // Record transaction with incentive
                TransactionRecord transactionRecord = new TransactionRecord();
                transactionRecord.setSender(sender);
                transactionRecord.setRecipient(recipient);
                transactionRecord.setAmount(amount);
                transactionRecord.setIncentive(incentiveAmount);
                transactionRecord.setTimestamp(LocalDateTime.now());

                transactionRepository.save(transactionRecord);

                System.out.println("Transaction recorded with incentive: " + transaction);
            } else {
                System.out.println("Transaction rejected: Insufficient funds for " + transaction.getSenderId());
            }
        } else {
            System.out.println("Transaction rejected: Invalid sender or recipient");
        }
    }
}