-- Hibernate validates String → VARCHAR; CHAR(3) surfaces as bpchar and fails validate.
ALTER TABLE payment
    ALTER COLUMN currency TYPE VARCHAR(3);
