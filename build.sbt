name := "AkkaServer"

version := "1.0"

scalaVersion := "2.11.8"


libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.11.14",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "io.argonaut" %% "argonaut" % "6.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.15")


resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"

resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"

