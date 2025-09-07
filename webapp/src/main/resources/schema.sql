CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS plpgsql;

CREATE TABLE IF NOT EXISTS users (
                                     user_id       UUID PRIMARY KEY ,
                                     user_name     VARCHAR(100) NOT NULL,
                                     surname       VARCHAR(100) NOT NULL,
                                     email         TEXT NOT NULL UNIQUE,
                                     password_hash VARCHAR(100) NOT NULL,
                                     cellphone     VARCHAR(30) NOT NULL,
                                     role          VARCHAR(20) NOT NULL CHECK (role IN ('admin','user')),
                                     created_at    TIMESTAMPTZ DEFAULT NOW()
);


CREATE TABLE IF NOT EXISTS clubs (
                                     club_id UUID PRIMARY KEY ,
                                     club_name VARCHAR(100) NOT NULL,
                                     location VARCHAR(255) NOT NULL,
                                     rating NUMERIC(2,1) CHECK (rating >= 1 AND rating <= 5),
                                     quantity_ratings INT DEFAULT 0,
                                     price_range VARCHAR(20) CHECK (price_range IN ('bajo','medio','alto')),
                                     description TEXT,
                                     zone VARCHAR(50) CHECK (zone IS NULL OR zone IN ('ZONA_CABA','ZONA_NORTE','ZONA_OESTE','ZONA_SUR','ZONA_GBA')),
                                     contact_info UUID REFERENCES users(user_id) ON DELETE SET NULL,
                                     music_genre VARCHAR(50),
                                     zone VARCHAR(50) CHECK (zone IN ('ZONA_CABA','ZONA_NORTE', 'ZONA_OESTE', 'ZONA_SUR')),
                                     created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS club_tables (
                                           table_id     UUID PRIMARY KEY ,
                                           club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                           table_number INT NOT NULL,
                                           capacity     INT NOT NULL CHECK (capacity > 0),
                                           table_state  VARCHAR(20) NOT NULL CHECK (table_state IN ('disponible','pendiente','reservada')),
                                           price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                           discount     NUMERIC(5,2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                                           UNIQUE (club_id, table_number)
);


CREATE TABLE IF NOT EXISTS bookings (
                                        booking_id    UUID PRIMARY KEY ,
                                        user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                        table_id      UUID NOT NULL REFERENCES club_tables(table_id) ON DELETE CASCADE,
                                        booking_start TIMESTAMPTZ NOT NULL,
                                        booking_end   TIMESTAMPTZ NOT NULL,
                                        status        VARCHAR(20) NOT NULL CHECK (status IN ('confirmada','pendiente','disponible')),
                                        created_at    TIMESTAMPTZ DEFAULT NOW(),
                                        total_amount  NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
                                        CHECK (booking_end > booking_start)
);



CREATE TABLE IF NOT EXISTS activity (
                                        activity_id   UUID PRIMARY KEY ,
                                        booking_id    UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                        activity_type VARCHAR(50) NOT NULL,
                                        activity_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        details       TEXT
);

CREATE TABLE IF NOT EXISTS reviews (
                                       review_id   UUID PRIMARY KEY ,
                                       user_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                       club_id     UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                       rating      INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                                       description TEXT,
                                       created_at  TIMESTAMPTZ DEFAULT NOW(),
                                       UNIQUE (user_id, club_id)
);

CREATE TABLE IF NOT EXISTS photos (
                                      photo_id     UUID PRIMARY KEY ,
                                      club_id      UUID NOT NULL REFERENCES clubs (club_id) ON DELETE CASCADE,
                                      url          TEXT NOT NULL,
                                      data         BYTEA,
                                      posted_by    UUID NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
                                      created_at   TIMESTAMPTZ DEFAULT NOW(),
                                      filename     TEXT,
                                      content_type TEXT,
                                      size_bytes   BIGINT
);
CREATE INDEX IF NOT EXISTS photos_club_id_idx ON photos (club_id);

CREATE TABLE IF NOT EXISTS notifications (
                                             notification_id UUID PRIMARY KEY ,
                                             user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                             type       VARCHAR(30) NOT NULL,
                                             message    TEXT NOT NULL,
                                             is_read    BOOLEAN DEFAULT FALSE,
                                             created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS notifications_user_read_idx ON notifications (user_id, is_read);

CREATE TABLE IF NOT EXISTS products (
                                        product_id   UUID PRIMARY KEY ,
                                        club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                        product_name VARCHAR(100) NOT NULL,
                                        description  TEXT,
                                        base_price   NUMERIC(10,2) NOT NULL,
                                        product_type VARCHAR(50) NOT NULL CHECK (product_type IN ('alcoholicas','noAlcoholicas','tragos')),
                                        is_available BOOLEAN DEFAULT TRUE,
                                        created_at   TIMESTAMPTZ DEFAULT NOW()
);



CREATE TABLE IF NOT EXISTS promotions (
                                          promotion_id  UUID PRIMARY KEY ,
                                          club_id       UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                          promotion_name VARCHAR(100) NOT NULL,
                                          description   TEXT,
                                          start_date    DATE NOT NULL,
                                          end_date      DATE NOT NULL,
                                          discount      NUMERIC(5,2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                                          CHECK (end_date >= start_date)
);


CREATE TABLE IF NOT EXISTS booking_items (
                                             booking_item_id UUID PRIMARY KEY ,
                                             booking_id  UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                             product_id  UUID NOT NULL REFERENCES products(product_id),
                                             quantity    INT NOT NULL CHECK (quantity > 0),
                                             unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
                                             line_total  NUMERIC(10,2) NOT NULL
);

-- Trigger para line_total
DROP FUNCTION IF EXISTS booking_items_set_line_total() CASCADE;

CREATE OR REPLACE FUNCTION booking_items_set_line_total()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.quantity IS NULL OR NEW.unit_price IS NULL THEN
        RAISE EXCEPTION 'quantity y unit_price no pueden ser NULL';
    END IF;

    IF NEW.quantity <= 0 THEN
        RAISE EXCEPTION 'quantity debe ser > 0';
    END IF;

    IF NEW.unit_price < 0 THEN
        RAISE EXCEPTION 'unit_price debe ser >= 0';
    END IF;

    NEW.line_total := ROUND(NEW.unit_price * NEW.quantity, 2);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_booking_items_line_total ON booking_items;

CREATE TRIGGER trg_booking_items_line_total
    BEFORE INSERT OR UPDATE OF quantity, unit_price ON booking_items
    FOR EACH ROW EXECUTE FUNCTION booking_items_set_line_total();

