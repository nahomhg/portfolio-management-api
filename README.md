# Portfolio Management API

A RESTful backend API for tracking digital asset investment portfolios, containing real-time valuations and transaction history.

Solving the problem of fragmented portfolio tracking across multiple exchanges & wallets.
Providing a single 'source-of-truth' for user's holdings, transactions, cost basis, and PnL calculations.


## Tech Stack
- **Backend:** Java 21, Spring Boot 3.5.3
- **Database:** PostgreSQL
- **Caching:** Redis (for 'price' data optimisation)
- **Auth:** JWT-based Authentication
- **Container:** Docker
- **Migration Tool:** Flyway


## Features
- Automated cost-basis calculations using weighted averages.
- Real-time portfolio valuations with external price feeds.
- PnL tracking across all holdings.
- RESTful API design with error handling.

##  Architecture

### System Overview
- User has an account where they can securely note their transactions at certain price points and specific times.
- Application will store transactions and dynamically work out an asset's unrealisedPnL, its weight in the portfolio as well as total portfolio unrealisedPnL.

###  Data Flow
1. Upon application start, we send an API request to CoinGecko's pricing API and receive top 100 assets.
2. Response data is cached in Redis with a 5-minute-TTL. Every 60 seconds endpoint is queried to get up-to-date prices via a scheduled job. The 5-minute TTL acts as a safety net for job failures.
3. User submits transaction (current or back-dated). Example request:
Request A:

```json
{
    "asset":"btc",
    "transactionType":"BUY",
    "units": 0.5
}
```

Request B:
```json
{
    "asset":"btc",
    "transactionType":"BUY",
    "units": 0.5,
    "transactionDate" : "2024-10-26"
}
```
Request A uses current pricing endpoint (cached, fast). Request B uses historical API (not cached, slower) due to CoinGecko's different rate limits on historical data.

5. 'Holdings' table updates with weighted cost basis and units count.
6. Portfolio valuation calculated at request time using cached pricing.


**Portfolio Valuation:** 
_Why compute Portfolio Valuation on request instead of storing and reading from database?_
- 'Holdings' does not store live pricing information. We calculate portfolio valuations on-demand using cached prices to avoid storing stale data, and also avoids time-series recomputation. 

### Key Design Decisions
- **Pricing Caching Strategy:** Redis serves as a distributed cache for pricing data ingestion from CoinGecko's free prices API, preventing API rate limit and dependency on external response time.

- **Concurrency controls:** Optimistic Locking used to prevent race-conditions when transactions are being created/added.

- **Idempotency & DeDupe Detection:** Each transaction requires user to send a unique IDEMPOTENCY_KEY to prevent duplicate submissions from network retries or user error.

- **Database Optimisation:** Database indexing on user_id and asset_name on transactions to allow for efficient transactions search.


##  Current Limitations and Future Planning
- Free API usage limits access to historical data i.e. transactions older than a year cannot have their prices retrieved.
