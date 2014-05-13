name := "The Akka Lab"

version := "0.1"

scalaVersion := "2.10.3"

resolvers += "Kamon Repository" at "http://repo.kamon.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.2",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "io.kamon" % "kamon-core" % "0.3.0",
  "io.kamon" % "kamon-spray" % "0.3.0",
  "io.kamon" % "kamon-dashboard" % "0.3.0",
  "io.kamon" % "kamon-statsd" % "0.3.0"
)