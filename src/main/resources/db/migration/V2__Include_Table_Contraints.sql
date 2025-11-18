--- Add min value constraint on Transaction table's 'units' column
ALTER TABLE IF EXISTS transactions
    ADD CONSTRAINT non_zero_transaction_units CHECK (units > 0);

--- Add min value constraint on 'price' column
ALTER TABLE IF EXISTS transactions
    ADD CONSTRAINT non_negative_transactions_price CHECK (total_price = 0 AND txn_type = 'AIRDROP' or total_price > 0 AND txn_type <> 'AIRDROP');


--- Add min value constraint on Holding table's 'units' column
ALTER TABLE IF EXISTS holdings
    ADD CONSTRAINT non_zero_holding_units CHECK (units > 0);


