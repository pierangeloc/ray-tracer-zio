import sbt._

object Dependencies {
  val http4sVersion = "0.21.6"
  val tapirZioVersion = "0.15.2"

  val zioVersion       = "1.0.0-RC21-2"
  val zioCatsVersion   = "2.1.4.0-RC17"
  val zioMacrosVersion = "0.6.0"

  val fs2Version        = "2.0.0"
  val catsVersion       = "2.0.0"
  val catsEffectVersion = "2.1.4"
  val circeVersion      = "0.12.1"
  val scrimageVersion   = "3.0.0"

  lazy val scalaTest  = "org.scalatest"  %% "scalatest"  % "3.2.0-M1"
  lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

  lazy val cats       = "org.typelevel" %% "cats-core"   % catsVersion
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  lazy val catsLaws   = "org.typelevel" %% "cats-laws"   % "1.1.0"

  lazy val mouse       = "org.typelevel" %% "mouse"            % "0.23"
  lazy val zio         = "dev.zio"       %% "zio"              % zioVersion
  lazy val zioStreams  = "dev.zio"       %% "zio-streams"      % zioVersion
  lazy val zioCats     = "dev.zio"       %% "zio-interop-cats" % zioCatsVersion

  lazy val fs2 = "co.fs2" %% "fs2-io" % fs2Version

  lazy val http4sServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  lazy val http4sClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
  lazy val http4sDsl    = "org.http4s" %% "http4s-dsl"          % http4sVersion
  lazy val http4sCirce  = "org.http4s" %% "http4s-circe"        % http4sVersion

  lazy val tapirZio           = "com.softwaremill.sttp.tapir" %% "tapir-zio"                % "0.15.3"
  lazy val tapirCirce         = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % "0.15.3"
  lazy val tapirZioHttp4s     = "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server"  % "0.15.3"
  lazy val tapirOpenAPI       = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % "0.16.10"
  lazy val tapirOpenAPICirce  = "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "0.16.10"

  lazy val newType = "io.estatico" %% "newtype" % "0.4.3"

  lazy val http4sAll = Seq(http4sServer, http4sClient, http4sDsl, http4sCirce)

  lazy val circeCore     = "io.circe" %% s"circe-core"           % circeVersion
  lazy val circeGeneric  = "io.circe" %% s"circe-generic"        % circeVersion
  lazy val circeGenericX = "io.circe" %% s"circe-generic-extras" % circeVersion
  lazy val circeParser   = "io.circe" %% "circe-parser"          % circeVersion
  lazy val circeRefined  = "io.circe" %% "circe-refined"         % circeVersion

  val doobieVersion = "0.8.8"

  val doobie: Seq[ModuleID] =
    Seq(
      "org.tpolecat" %% "doobie-core"     % doobieVersion,
      "org.tpolecat" %% "doobie-h2"       % doobieVersion, // H2 driver 1.4.199 + type mappings.
      "org.tpolecat" %% "doobie-hikari"   % doobieVersion, // HikariCP transactor.
      "org.tpolecat" %% "doobie-postgres" % doobieVersion, // Postgres driver 42.2.5 + type mappings.
      "org.tpolecat" %% "doobie-refined"  % doobieVersion  // For refined types
    )

  // type-level restrictions
  lazy val breeze       = "org.scalanlp" %% "breeze"         % "1.0"
  lazy val breezeNative = "org.scalanlp" %% "breeze-natives" % "1.0"
  lazy val singletonOps = "eu.timepit"   %% "singleton-ops"  % "0.4.1"
  lazy val refined      = "eu.timepit"   %% "refined"        % "0.9.7"
  lazy val refinedCats  = "eu.timepit"   %% "refined-cats"   % "0.9.7"

  lazy val circeAll = Seq(circeCore, circeGeneric, circeGenericX)

  lazy val log4CatsCore  = "io.chrisdavenport" %% s"log4cats-core"  % "1.0.1"
  lazy val log4CatsSlf4j = "io.chrisdavenport" %% s"log4cats-slf4j" % "1.0.1"

  lazy val scalafx = "org.scalafx" %% "scalafx" % "8.0.144-R12"

  lazy val scalaTags = "com.lihaoyi" %% "scalatags" % "0.6.8"

  // scrimage
  lazy val scrimageCore    = "com.kyleu" %% "scrimage-core" % scrimageVersion
  lazy val scrimageIOX     = "com.kyleu" %% "scrimage-io-extra" % scrimageVersion
  lazy val scrimageFilters = "com.kyleu" %% "scrimage-filters" % scrimageVersion

  lazy val zioTest         = "dev.zio" %% "zio-test"        % zioVersion
  lazy val zioTestSbt      = "dev.zio" %% "zio-test-sbt"    % zioVersion
//  lazy val zioMacrosCore   = "dev.zio" %% "zio-macros-core" % zioMacrosVersion
//  lazy val zioMacrosTest   = "dev.zio" %% "zio-macros-test" % zioMacrosVersion

}
