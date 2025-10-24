package io.github.nahomgh.portfolio.controller;

import io.github.nahomgh.portfolio.auth.domain.User;
import io.github.nahomgh.portfolio.dto.PortfolioDTO;
import io.github.nahomgh.portfolio.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<PortfolioDTO> getPortfolio(Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(portfolioService.getPortfolio(user.getId()));
    }
}

