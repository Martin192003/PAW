-- =====================================================================
--  ESQUEMA HSQL PARA TESTS (adaptado desde PostgreSQL)
--  Base: esquema PSQL que mandaste
--  Motor: HSQLDB con sql.syntax_pgs=true
-- =====================================================================

-- IMPORTANTE:
-- - No hay CREATE EXTENSION ni plpgsql
-- - Los DEFAULT de UUID usan UUID()
-- - Trigger de booking_items está escrito en SQL/PSM HSQL

-----------------------------------------------------------------------
-- USERS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                                     user_id       UUID PRIMARY KEY,
                                     user_name     VARCHAR(100) NOT NULL,
                                     name          VARCHAR(100) NOT NULL DEFAULT '',
                                     surname       VARCHAR(100) NOT NULL,
                                     email         TEXT NOT NULL UNIQUE,
                                     password_hash VARCHAR(100) NOT NULL,
                                     cellphone     VARCHAR(30) NOT NULL,
                                     role          VARCHAR(20) NOT NULL CHECK (role IN ('admin','user')),
                                     created_at    TIMESTAMPTZ DEFAULT NOW(),
                                     is_verified   BOOLEAN DEFAULT FALSE,
                                     verification_token    VARCHAR(100),
                                     reset_password_token  VARCHAR(255),
                                     locale       VARCHAR(255)
);

-----------------------------------------------------------------------
-- CLUBS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clubs (
                                     club_id UUID PRIMARY KEY,
                                     club_name VARCHAR(100) NOT NULL,
                                     location VARCHAR(255) NOT NULL,
                                     rating NUMERIC(2,1) CHECK (rating >= 1 AND rating <= 5),
                                     quantity_ratings INT DEFAULT 0,
                                     price_range VARCHAR(20) CHECK (price_range IN ('bajo','medio','alto')),
                                     description TEXT,
                                     contact_info UUID REFERENCES users(user_id) ON DELETE SET NULL,
                                     music_genre VARCHAR(50),
                                     zone VARCHAR(50) CHECK (zone IN ('ZONA_CABA','ZONA_NORTE', 'ZONA_OESTE', 'ZONA_SUR')),
                                     is_bar BOOLEAN NOT NULL DEFAULT FALSE,
                                     cbu VARCHAR(50),
                                     created_at TIMESTAMPTZ DEFAULT NOW()
);

-----------------------------------------------------------------------
-- CLUB TABLES
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS club_tables (
                                           table_id     UUID PRIMARY KEY,
                                           club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                           table_number INT NOT NULL,
                                           capacity     INT NOT NULL CHECK (capacity > 0),
                                           table_state  VARCHAR(20) NOT NULL CHECK (table_state IN ('disponible','pendiente','reservada')),
                                           price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                           discount     NUMERIC(5,2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                                           UNIQUE (club_id, table_number)
);

-----------------------------------------------------------------------
-- BOOKINGS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
                                        booking_id    UUID PRIMARY KEY,
                                        user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                        table_id      UUID NOT NULL REFERENCES club_tables(table_id) ON DELETE CASCADE,
                                        booking_start TIMESTAMPTZ NOT NULL,
                                        booking_end   TIMESTAMPTZ NOT NULL,
                                        status        VARCHAR(20) NOT NULL CHECK (status IN ('confirmada','pendiente','disponible', 'cancelada')),
                                        created_at    TIMESTAMPTZ DEFAULT NOW(),
                                        total_amount  NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
                                        CHECK (booking_end > booking_start)
);

-----------------------------------------------------------------------
-- ACTIVITY
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity (
                                        activity_id   UUID PRIMARY KEY,
                                        booking_id    UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                        activity_type VARCHAR(50) NOT NULL,
                                        activity_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        details       TEXT
);

-----------------------------------------------------------------------
-- PHOTOS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS photos (
                                      photo_id   UUID PRIMARY KEY DEFAULT UUID(),
                                      club_id    UUID NOT NULL REFERENCES clubs (club_id) ON DELETE CASCADE,
                                      url        TEXT NOT NULL,
                                      data       BYTEA,
                                      posted_by  UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
                                      created_at TIMESTAMPTZ DEFAULT NOW(),
                                      filename   TEXT,
                                      content_type TEXT,
                                      size_bytes BIGINT,
                                      photo_type VARCHAR(20) DEFAULT 'CAROUSEL'
                                          CHECK (photo_type IN ('COVER', 'CAROUSEL', 'REVIEW', 'EVENT'))
);

CREATE INDEX IF NOT EXISTS idx_photos_club_id ON photos (club_id);

-----------------------------------------------------------------------
-- REVIEWS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
                                       review_id   UUID PRIMARY KEY,
                                       user_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                       club_id     UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                       rating      INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                                       description TEXT,
                                       created_at  TIMESTAMPTZ DEFAULT NOW(),
                                       photo_id    UUID REFERENCES photos(photo_id) ON DELETE SET NULL,
                                       from_table_guest BOOLEAN NOT NULL DEFAULT FALSE,
                                       UNIQUE (user_id, club_id)
);

-----------------------------------------------------------------------
-- NOTIFICATIONS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
                                             notification_id UUID PRIMARY KEY DEFAULT UUID(),
                                             user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                             type       VARCHAR(30) NOT NULL,
                                             message    TEXT NOT NULL,
                                             is_read    BOOLEAN DEFAULT FALSE,
                                             created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS notifications_user_read_idx
    ON notifications (user_id, is_read);

-----------------------------------------------------------------------
-- PRODUCTS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
                                        product_id   UUID PRIMARY KEY DEFAULT UUID(),
                                        club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                        product_name VARCHAR(100) NOT NULL,
                                        description  TEXT,
                                        base_price   NUMERIC(10,2) NOT NULL,
                                        product_type VARCHAR(50) NOT NULL CHECK (product_type IN ('alcoholicas','noAlcoholicas')),
                                        is_available BOOLEAN DEFAULT TRUE,
                                        created_at   TIMESTAMPTZ DEFAULT NOW()
);

-----------------------------------------------------------------------
-- PROMOTIONS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS promotions (
                                          promotion_id   UUID PRIMARY KEY DEFAULT UUID(),
                                          club_id        UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                          promotion_name VARCHAR(100) NOT NULL,
                                          description    TEXT,
                                          start_date     DATE NOT NULL,
                                          end_date       DATE NOT NULL,
                                          discount       NUMERIC(5,2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                                          CHECK (end_date >= start_date)
);

-----------------------------------------------------------------------
-- BOOKING ITEMS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking_items (
                                             booking_item_id UUID PRIMARY KEY,
                                             booking_id  UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                             product_id  UUID NOT NULL REFERENCES products(product_id),
                                             quantity    INT NOT NULL CHECK (quantity > 0),
                                             unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
                                             line_total  NUMERIC(10,2) NOT NULL
);

-----------------------------------------------------------------------
-- RECEIPTS (una sola definición, sin duplicados)
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS receipts (
                                        receipt_id   UUID PRIMARY KEY DEFAULT UUID(),
                                        booking_id   UUID NOT NULL REFERENCES bookings (booking_id) ON DELETE CASCADE,
                                        uploaded_by  UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
                                        url          TEXT NOT NULL,
                                        data         BYTEA,
                                        filename     TEXT,
                                        content_type TEXT,
                                        size_bytes   BIGINT,
                                        created_at   TIMESTAMPTZ DEFAULT NOW(),
                                        UNIQUE (booking_id)
);

CREATE INDEX IF NOT EXISTS receipts_booking_id_idx
    ON receipts (booking_id);

-----------------------------------------------------------------------
-- CLUB_ADMINS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS club_admins (
                                           club_id  UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                           admin_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                           PRIMARY KEY (club_id, admin_id)
);

-----------------------------------------------------------------------
-- CLUB_LAYOUTS
-----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS club_layouts (
                                            layout_id    UUID PRIMARY KEY DEFAULT UUID(),
                                            club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                            uploaded_by  UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                            url          TEXT NOT NULL,
                                            data         BYTEA,
                                            filename     TEXT,
                                            content_type TEXT,
                                            size_bytes   BIGINT,
                                            created_at   TIMESTAMPTZ DEFAULT NOW(),
                                            UNIQUE (club_id)
);

CREATE INDEX IF NOT EXISTS idx_club_layouts_club_id
    ON club_layouts (club_id);


-----------------------------------------------------------------------
-- EVENT
-----------------------------------------------------------------------

DROP TABLE IF EXISTS event CASCADE;

CREATE TABLE event (
                       eventid    UUID PRIMARY KEY,
                       clubid     UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                       eventname  VARCHAR(120) NOT NULL,
                       description TEXT,
                       date       DATE NOT NULL,
                       photoid    UUID REFERENCES photos(photo_id)
);

CREATE INDEX IF NOT EXISTS idx_events_club_id ON event (clubid);
CREATE INDEX IF NOT EXISTS idx_events_date    ON event (date);