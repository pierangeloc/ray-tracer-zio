package io.tuliplogic.raytracer.http.model.attapirato

case class Config(
    db: Database
)

case class Database(
    url: String,
    user: String,
    password: String,
)

object Config {}
