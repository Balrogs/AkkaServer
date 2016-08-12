name := "AkkaServer"

version := "1.0"

scalaVersion := "2.11.8"


libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.3.15"

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.11.14"
)

libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"