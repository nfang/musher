organization := "me.nfang"

name := "musher"

version := "1.0"

scalaVersion := "2.9.0-1"

seq(webSettings :_*)

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.0.0.RC1",
  "org.scalatra" %% "scalatra-scalate" % "2.0.0.RC1",
  "org.eclipse.jetty" % "jetty-webapp" % "7.4.5.v20110725" % "jetty",
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "redis.clients" % "jedis" % "2.0.0",
  "com.sun.jersey" % "jersey-client" % "1.8",
  "org.specs2" %% "specs2" % "1.5",
  "org.specs2" %% "specs2-scalaz-core" % "6.0.RC2" % "test"
)

jettyPort := 8212

resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

scalacOptions ++= Seq("-unchecked", "-deprecation")