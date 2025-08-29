CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS plpgsql;

CREATE TABLE IF NOT EXISTS users (
                                     "userId" UUID default uuid_generate_v4(),
                                     "userName" VARCHAR(100) NOT NULL,
                                     "surname" VARCHAR(100) NOT NULL,
                                     "email" VARCHAR(100) NOT NULL UNIQUE,
                                     "password" VARCHAR(100) NOT NULL,
                                     "cellphone" VARCHAR(30) NOT NULL,
                                     "role" VARCHAR(20) NOT NULL CHECK ("role" IN ('admin', 'user')),
                                     "createdAt" TIMESTAMPTZ DEFAULT (NOW() AT TIME ZONE 'America/Argentina/Buenos_Aires')
);