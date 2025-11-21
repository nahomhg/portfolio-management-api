-- 20/11/25 Add 'airdrop' into valid transaction types by dropping constraint and adding back with amendment.
-- 20/11/25 Change name of 'total_price' to 'total_cost' in Transactions table.

ALTER TABLE IF EXISTS transactions
DROP CONSTRAINT transactions_txn_type_ck;

ALTER TABLE IF EXISTS transactions
ADD CONSTRAINT transactions_txn_type_ck CHECK (txn_type IN ('BUY', 'SELL', 'AIRDROP'));

ALTER TABLE IF EXISTS transactions
    RENAME COLUMN total_price TO total_cost;
