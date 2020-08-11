CREATE TABLE "users" (
  "id" uuid NOT NULL DEFAULT NULL,
  "email" varchar(50) NOT NULL DEFAULT NULL,
  "password_hash" varchar(500) DEFAULT NULL,
  "access_token" varchar(500) DEFAULT NULL,
  "access_token_expires_at" timestamptz DEFAULT NULL,
  PRIMARY KEY ("id")
);