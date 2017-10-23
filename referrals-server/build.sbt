name := """referrals"""
organization := "com.soc"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.2"

autoScalaLibrary := true

resolvers += "public-jboss" at "http://repository.jboss.org/nexus/content/groups/public-jbos"

libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value

libraryDependencies ++= Seq(
  "org.drools" % "drools-core" % "7.3.0.Final",
  "org.drools" % "drools-compiler" % "7.3.0.Final"
)

libraryDependencies += guice

libraryDependencies += "com.google.code.gson" % "gson" % "2.2.4"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.4"


herokuAppName in Compile := "referral-server-9x"