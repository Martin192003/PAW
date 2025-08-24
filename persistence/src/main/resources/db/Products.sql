CREATE TABLE IF NOT EXISTS products (
    productId SERIAL PRIMARY KEY,
    productName VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    productType VARCHAR(50) NOT NULL CHECK (productType IN ('alcoholicas', 'noAlcoholicas')),
    isAvailable BOOLEAN DEFAULT TRUE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)