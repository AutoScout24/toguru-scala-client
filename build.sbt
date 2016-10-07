lazy val root = project.in( file(".") ).enablePlugins(GitVersioning)

name := "toguru-scala-client"

git.baseVersion := "0.2.1"

organization in ThisBuild := "com.autoscout24"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("autoscout24")

scalaVersion in ThisBuild := "2.11.8"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings",
  "-Yno-adapted-args", "-Xmax-classfile-name", "130")

val playVersion = "2.5.4"

resolvers += Resolver.jcenterRepo

libraryDependencies in ThisBuild ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.scalactic" %% "scalactic" % "2.2.5",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.typesafe.play" %% "play-json" % playVersion,
  "com.typesafe.play" %% "play" % playVersion % "optional",
  "com.typesafe.play" %% "play-test" % playVersion % "optional",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.hootsuite" %% "scala-circuit-breaker" % "1.0.0",
  "org.mockito" % "mockito-core" % "2.0.8-beta" % "test",
  "org.http4s" %% "http4s-dsl" % "0.14.8a" % "test",
  "org.http4s" %% "http4s-blaze-server" % "0.14.8a" % "test"
)

ScoverageSbtPlugin.ScoverageKeys.coverageMinimum := 80

ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := true

resolvers in ThisBuild ++= Seq(
  Classpaths.sbtPluginReleases,
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)
