name := "The Akka Lab"

version := "0.1"

scalaVersion := "2.10.3"

// Should not be necessary - Kamon libs are in Maven central
//resolvers += "Kamon Repository" at "http://repo.kamon.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.2",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.2",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "io.kamon" % "kamon-core" % "0.3.0",
  "io.kamon" % "kamon-spray" % "0.3.0",
  "io.kamon" % "kamon-statsd" % "0.3.0"
)