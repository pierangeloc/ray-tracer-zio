addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.4")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")

// Allows To Continuously Reload Applications
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.0.3")


// Allows Scala.js Compilation
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.28")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")
