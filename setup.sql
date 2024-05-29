DROP DATABASE IF EXISTS banking;

CREATE DATABASE banking;
USE banking;

CREATE TABLE Users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(64) NOT NULL
);

CREATE TABLE Accounts (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          created_date DATETIME NOT NULL,
                          FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Cards (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       user_id INT NOT NULL,
                       card_number VARCHAR(16) NOT NULL UNIQUE,
                       expiration_date DATE NOT NULL,
                       cvv INT NOT NULL,
                       balance DECIMAL(14,2) DEFAULT 0.00,
                       FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE Deposits (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          account_id INT NOT NULL,
                          amount DECIMAL(14,2) NOT NULL,
                          start_date DATE NOT NULL,
                          end_date DATE NOT NULL,
                          status VARCHAR(50) NOT NULL DEFAULT 'active',
                          FOREIGN KEY (account_id) REFERENCES Accounts(id) ON DELETE CASCADE
);

CREATE TABLE Transactions (
                              id INT AUTO_INCREMENT PRIMARY KEY,
                              from_card VARCHAR(16) NOT NULL,
                              to_card VARCHAR(16) NOT NULL,
                              amount DECIMAL(14,2) NOT NULL,
                              transaction_date DATETIME NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              FOREIGN KEY (from_card) REFERENCES Cards(card_number) ON DELETE CASCADE,
                              FOREIGN KEY (to_card) REFERENCES Cards(card_number) ON DELETE CASCADE
);