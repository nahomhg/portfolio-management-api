# Portfolio Management API

A RESTful backend API for tracking digital asset investment portfolios, containing real-time valuations and transaction history.

Solving the problem of fragmented portfolio tracking across multiple exchanges & wallets.
Providing a single 'source-of-truth' for user's holdings, transactions, cost basis, and PnL calculations.


## Tech Stack
- **Backend:** Java 18, Spring Boot
- **Database:** PostgreSQL
- **Caching:** Redis (for 'price' data optimisation)
- **Auth:** JWT-based Authentication
- **Container:** Docker
- **Migration Tool:** Flyway


## Features
- Automated cost-basis calculations using weighted averages
- Real-time portfolio valuations with external price feeds
- PnL tracking across all holdings
- RESTful API design with error handling.

## Architecture

### System Overview
- User has an account where they can securely note their transactions at certain price points and specific times.
- Application will store transactions and dynamically work out an asset's unrealisedPnL, its weight in the portfolio as well as total portfolio unrealisedPnL.

### Data Flow
- Upon application start, we send an API request to CoinGecko's pricing API and receives top 100 assets.
- Response data is cached in Redis with a 5-minute-TTL. Every 15 seconds endpoint is queried to get up-to-date prices.

**Transaction Processing:** [WORK IN PROGRESS WILL UPDATE]
- User creates a transaction, if there's a cache hit, we retrieve asset price otherwise query another CoinGecko API for the particular asset.

**Portfolio Valuation:** [WORK IN PROGRESS WILL UPDATE]
- Every transaction updates the Holdings table which in turn updates the Portfolio table.


### Key Design Decisions
- **Pricing Caching Strategy:** Redis serves as a distributed cache for pricing data ingestion from CoinGecko's free prices API, preventing API rate limit and dependency on external response time.

- **Concurrency controls:** Optimistic Locking used to prevent race-conditions when transactions are being created/added.

- **Idempotency & DeDupe Detection:** Each transaction requires user to send a unique IDEMPOTENCY_KEY to prevent duplicate submissions from network retries or user error.

- **Database Optimisation:** Database indexing on user_id and asset_name on transactions to allow for efficient transactions search.


## Current Limitations and Future Planning:
- Currently, holdings are calculated on-the-fly by aggregating transactions. This is not scalable, therefore, the application is being re-written to store
holdings state at database level.
- Free API usage limits access to historical data i.e. transactions older than a year cannot have their prices retrieved.
