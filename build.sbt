name := "tz-diff"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.7.1",
  "com.github.tototoshi" %% "scala-csv" % "1.3.5",
  "com.lihaoyi" %% "requests" % "0.1.7",
  "io.github.cquiroz" %% "kuyfi" % "0.9.3",
  "io.verizon.journal" %% "core" % "3.0.19",
  "org.apache.commons" % "commons-compress" % "1.18"
)
