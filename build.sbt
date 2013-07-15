name := "akka_dw"

organization := "io.github.cfchou"

version := "1.0"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-feature", "-deprecation")

resolvers ++= Seq(
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
)

libraryDependencies ++= Seq(
    "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
    "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
    "com.typesafe.akka" %% "akka-actor" % "2.2.0",
    "com.typesafe.akka" %% "akka-testkit" % "2.2.0" % "test",
    "com.typesafe.akka" %% "akka-remote" % "2.2.0",
    "com.typesafe.akka" %% "akka-agent" % "2.2.0"
)

EclipseKeys.withSource := true
