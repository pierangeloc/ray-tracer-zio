import Dependencies._

enablePlugins(JmhPlugin)
enablePlugins(ScalafmtPlugin)


lazy val commonSettings = inThisBuild(
    Seq(
      scalaVersion := "2.13.5",
      scalacOptions ++= Seq("-Ymacro-annotations"),
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      updateOptions := updateOptions.value.withLatestSnapshots(false),
      addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
      addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        newType,
//        zioMacrosCore,
//        zioMacrosTest,
        zioTest % "test",
        zioTestSbt % "test"
      ) ++ logging
    )
)


lazy val `simple-http4s` = project
  .in(file("simple-http4s"))
  .settings(commonSettings)
  .enablePlugins(JavaServerAppPackaging)
  .settings(
    mainClass in Compile := Some("io.tuliplogic.raytracer.http.Main"),
    packageName in Docker := "ray-tracer-zio",
    dockerBaseImage := "openjdk:jre-alpine"
  )
  .settings(
    name := "simple-http4s",
    mainClass in reStart := Some("io.tuliplogic.raytracer.http.Main"),
      libraryDependencies ++= (Seq(
      zio,
      zioMagic,
      zioCats exclude("dev.zio", "zio-test"),
      cats,
      catsEffect,
      http4sServer,
      http4sDsl,
      http4sCirce,
      tapirZio,
      tapirZioHttp4s,
      tapirOpenAPI,
      tapirOpenAPICirce,
      tapirSwagger,
      tapirCirce,
      tapirNewType,
      tapirRefined,
      circeCore,
      circeParser,
      circeGeneric,
      circeGenericX,
      circeRefined,
      log4CatsSlf4j,
      bcrypt
    ) ++ db ++ cfg)
  )
  .dependsOn(commons)
  .dependsOn(`ray-tracer`)


lazy val geometry = project
  .in(file("geometry"))
  .settings(commonSettings)
  .settings(
    name := "geometry",
    libraryDependencies ++= Seq(
      zio,
      zioMagic,
      zioCats exclude("dev.zio", "zio-test"),
      cats,
      catsEffect,
      fs2,
      breeze,
      breezeNative,
      singletonOps,
      log4CatsSlf4j,
      scalaTest % "test",
      scalaCheck % "test"
    )
  )
  .dependsOn(commons)

lazy val commons = project
  .in(file("commons"))
  .settings(commonSettings)
  .settings(
    name := "commons",
    libraryDependencies ++= Seq(
      zio,
      zioMagic,
      zioStreams,
      cats,
      mouse
    )
  )

lazy val `ray-tracer` = project
  .in(file("ray-tracer"))
  .settings(commonSettings)
  .settings(
    name := "ray-tracer",
    libraryDependencies ++= Seq(
      zio,
      zioMagic,
      zioCats exclude("dev.zio", "zio-test"),
      cats,
      catsEffect,
      log4CatsSlf4j,
      scrimageCore,
      scrimageFilters,
      scrimageIOX,
      scalaTest % "test"
    )
  )
  .dependsOn(geometry % "test->test;compile->compile")
