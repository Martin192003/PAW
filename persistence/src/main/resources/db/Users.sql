CREATE TABLE IF NOT EXISTS users (
    userId SERIAL PRIMARY KEY,
    userName VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    cellphone VARCHAR(30) NOT NULL,
    permitions VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'user')),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);