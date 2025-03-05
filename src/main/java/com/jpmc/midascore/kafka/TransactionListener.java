package com.jpmc.midascore.kafka;

import com.jpmc.midascore.model.Transaction;
import com.jpmc.midascore.model.TransactionRecord;
import com.jpmc.midas H2 Integrationcore.model.User;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TransactionListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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

                userRepository.save(sender);
                userRepository.save(recipient);

                TransactionRecord transactionRecord = new TransactionRecord();
                transactionRecord.setSender(sender);
                transactionRecord.setRecipient(recipient);
                transactionRecord.setAmount(amount);
                transactionRecord.setTimestamp(LocalDateTime.now());

                transactionRepository.save(transactionRecord);

                System.out.println("Transaction recorded: " + transaction);
            } else {
                System.out.println("Transaction rejected: Insufficient funds for " + transaction.getSenderId());
            }
        } else {
            System.out.println("Transaction rejected: Invalid sender or recipient");
        }
    }
}