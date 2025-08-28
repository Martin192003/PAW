CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS plpgsql;

CREATE TABLE IF NOT EXISTS users (
                                     user_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     user_name     VARCHAR(100) NOT NULL,
                                     surname       VARCHAR(100) NOT NULL,
                                     email         TEXT NOT NULL UNIQUE,
                                     password_hash VARCHAR(100) NOT NULL,
                                     cellphone     VARCHAR(30) NOT NULL,
                                     role          VARCHAR(20) NOT NULL CHECK (role IN ('admin','user')),
                                     created_at    TIMESTAMPTZ DEFAULT NOW()
);


CREATE TABLE IF NOT EXISTS clubs (
                                     club_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     club_name VARCHAR(100) NOT NULL,
                                     location VARCHAR(255) NOT NULL,
                                     rating NUMERIC(2,1) CHECK (rating >= 1 AND rating <= 5),
                                     quantity_ratings INT DEFAULT 0,
                                     price_range VARCHAR(20) CHECK (price_range IN ('bajo','medio','alto')),
                                     description TEXT,
                                     contact_info UUID REFERENCES users(user_id) ON DELETE SET NULL,
                                     music_genre VARCHAR(50),
                                     created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS club_tables (
                                           table_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                           club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                           table_number INT NOT NULL,
                                           capacity     INT NOT NULL CHECK (capacity > 0),
                                           table_state  VARCHAR(20) NOT NULL CHECK (table_state IN ('disponible','pendiente','reservada')),
                                           price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                                           discount     NUMERIC(5,2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                                           UNIQUE (club_id, table_number)
);


CREATE TABLE IF NOT EXISTS bookings (
                                        booking_id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                        table_id      UUID NOT NULL REFERENCES club_tables(table_id) ON DELETE CASCADE,
                                        booking_start TIMESTAMPTZ NOT NULL,
                                        booking_end   TIMESTAMPTZ NOT NULL,
                                        status        VARCHAR(20) NOT NULL CHECK (status IN ('confirmada','pendiente','cancelada')),
                                        created_at    TIMESTAMPTZ DEFAULT NOW(),
                                        total_amount  NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
                                        CHECK (booking_end > booking_start)
);



CREATE TABLE IF NOT EXISTS activity (
                                        activity_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        booking_id    UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                        activity_type VARCHAR(50) NOT NULL,
                                        activity_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        details       TEXT
);

CREATE TABLE IF NOT EXISTS reviews (
                                       review_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                       user_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                       club_id     UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                       rating      INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                                       description TEXT,
                                       created_at  TIMESTAMPTZ DEFAULT NOW(),
                                       UNIQUE (user_id, club_id)
);


CREATE TABLE IF NOT EXISTS photos (
                                      photo_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      club_id    UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                      url        TEXT NOT NULL,
                                      posted_by  UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                      created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX ON photos (club_id);

CREATE TABLE IF NOT EXISTS notifications (
                                             notification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                             user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                             type       VARCHAR(30) NOT NULL,
                                             message    TEXT NOT NULL,
                                             is_read    BOOLEAN DEFAULT FALSE,
                                             created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX ON notifications (user_id, is_read);

CREATE TABLE IF NOT EXISTS products (
                                        product_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        club_id      UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                        product_name VARCHAR(100) NOT NULL,
                                        description  TEXT,
                                        base_price   NUMERIC(10,2) NOT NULL,
                                        product_type VARCHAR(50) NOT NULL CHECK (product_type IN ('alcoholicas','noAlcoholicas')),
                                        is_available BOOLEAN DEFAULT TRUE,
                                        created_at   TIMESTAMPTZ DEFAULT NOW()
);



CREATE TABLE IF NOT EXISTS promotions (
                                          promotion_id  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          club_id       UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                          promotion_name VARCHAR(100) NOT NULL,
                                          description   TEXT,
                                          start_date    DATE NOT NULL,
                                          end_date      DATE NOT NULL,
                                          discount      NUMERIC(5,2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                                          CHECK (end_date >= start_date)
);


CREATE TABLE IF NOT EXISTS booking_items (
                                             booking_item_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                             booking_id  UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                             product_id  UUID NOT NULL REFERENCES products(product_id),
                                             quantity    INT NOT NULL CHECK (quantity > 0),
                                             unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
                                             line_total  NUMERIC(10,2) NOT NULL
);


CREATE TABLE IF NOT EXISTS club_admins (
                                           club_id  UUID NOT NULL REFERENCES clubs(club_id) ON DELETE CASCADE,
                                           admin_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                           PRIMARY KEY (club_id, admin_id)
);
