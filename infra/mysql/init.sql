CREATE DATABASE identity_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE wallet_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE booking_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON identity_db.* TO 'cosync'@'%';
GRANT ALL PRIVILEGES ON wallet_db.*   TO 'cosync'@'%';
GRANT ALL PRIVILEGES ON booking_db.*  TO 'cosync'@'%';