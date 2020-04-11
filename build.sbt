name := "tz-diff"

version := "0.2"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha5",
  "com.github.pathikrit" %% "better-files" % "3.8.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.6",
  "com.lihaoyi" %% "requests" % "0.5.1",
  "io.github.cquiroz" %% "kuyfi" % "1.0.0",
  "org.log4s" %% "log4s" % "1.8.2",
  "org.apache.commons" % "commons-compress" % "1.20",
  "org.scalameta" %% "munit" % "0.7.2" % Test
)

testFrameworks += new TestFramework("munit.Framework")
