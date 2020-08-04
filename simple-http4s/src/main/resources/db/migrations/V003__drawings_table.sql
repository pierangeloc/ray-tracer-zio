CREATE TABLE "drawings" (
  "id" uuid NOT NULL DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "scene" jsonb DEFAULT NULL,
  "resolution_w" int,
  "resolution_h" int,
  "png" oid,
  PRIMARY KEY ("id"),
  CONSTRAINT fk_user_id
    FOREIGN KEY(user_id)
      REFERENCES users(id)
);