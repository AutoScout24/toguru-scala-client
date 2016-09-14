lazy val root = project.in( file(".") ).enablePlugins(GitVersioning)

name := "toguru-scala-client"

val playVersion = sys.env.getOrElse("PLAY_VERSION", "2.5.7")

git.baseVersion := s"0.1.0-Play-$playVersion"

organization in ThisBuild := "com.autoscout24"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion in ThisBuild := "2.11.8"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings",
  "-Yno-adapted-args", "-Xmax-classfile-name", "130")


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
