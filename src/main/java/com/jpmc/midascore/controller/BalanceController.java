package com.jpmc.midascore.controller;

import com.jpmc.midascore.model.Balance;
import com.jpmc.midascore.model.User;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/balance")
public class BalanceController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public Balance getBalance(@RequestParam String userId) {
        Optional<User> userOpt = userRepository.findByUsername(userId);
        BigDecimal balanceAmount = userOpt.map(User::getBalance).orElse(BigDecimal.ZERO);
        return new Balance(balanceAmount);
    }
}