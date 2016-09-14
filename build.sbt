import scala.util.Properties

name := "toguru-scala-client"
organization in ThisBuild := "com.autoscout24"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

version in ThisBuild := "1.0." + Properties.envOrElse("TRAVIS_BUILD_NUMBER", "0-SNAPSHOT")

scalaVersion in ThisBuild := "2.11.8"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings",
  "-Yno-adapted-args", "-Xmax-classfile-name", "130")

val playVersion = "2.5.4"

libraryDependencies in ThisBuild ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.scalactic" %% "scalactic" % "2.2.5",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.typesafe.play" %% "play-json" % playVersion,
  "com.typesafe.play" %% "play" % playVersion % "optional",
  "com.typesafe.play" %% "play-test" % playVersion % "optional",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.mockito" % "mockito-core" % "2.0.8-beta" % "test"
)

ScoverageSbtPlugin.ScoverageKeys.coverageMinimum := 80

ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := true

resolvers in ThisBuild ++= Seq(
  Classpaths.sbtPluginReleases,
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

lazy val root = project.in( file(".") )
