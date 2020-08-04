CREATE TABLE "users" (
  "id" bigint NOT NULL DEFAULT NULL,
  "username" varchar(50) NOT NULL DEFAULT NULL,
  "password" varchar(500) DEFAULT NULL,
  PRIMARY KEY ("id")
);