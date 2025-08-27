CREATE TABLE IF NOT EXISTS clubs(
    clubId SERIAL PRIMARY KEY,
    clubName VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    musicGenre VARCHAR(50) NOT NULL CHECK (musicGenre IN ('rock', 'pop', 'cachengue', 'techno', 'reggaeton', 'cumbia', 'trap', 'jazz', 'otro')),
    locationType VARCHAR(30) NOT NULL CHECK (locationType IN ('boliche', 'bar')),
    rating FLOAT CHECK (rating >= 1 AND rating <= 5),
    quantityRatings INT DEFAULT 0,
    priceRange VARCHAR(20) CHECK (priceRange IN ('bajo', 'medio', 'alto')),
    description TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clubAdmins (
    clubId INT REFERENCES clubs(clubId) ON DELETE CASCADE,
    adminId INT REFERENCES users(userId) ON DELETE CASCADE,
    PRIMARY KEY (clubId, adminId)
);