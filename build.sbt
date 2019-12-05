lazy val root = project.in(file(".")).enablePlugins(SemVerPlugin)

name := "toguru-scala-client"

organization in ThisBuild := "com.autoscout24"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("autoscout24")

crossScalaVersions in ThisBuild := Seq("2.12.3", "2.11.8")

scalaVersion in ThisBuild := "2.11.8"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings",
  "-Yno-adapted-args", "-Xmax-classfile-name", "130")

resolvers ++= Seq(Resolver.jcenterRepo, Resolver.bintrayRepo("autoscout24", "maven"))

libraryDependencies in ThisBuild ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "commons-io" % "commons-io" % "2.4",
  "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
  "org.komamitsu" % "phi-accural-failure-detector" % "0.0.3",
  "org.scalactic" %% "scalactic" % "3.0.3",
  "com.hootsuite" %% "scala-circuit-breaker" % "1.0.2",
  "org.mockito" % "mockito-core" % "2.0.8-beta" % "test",
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "org.http4s" %% "http4s-dsl" % "0.17.0-M3" % "test",
  "org.http4s" %% "http4s-blaze-server" % "0.17.0-M3" % "test"
)

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) =>
      libraryDependencies.value ++ Seq(
        "com.typesafe.play" %% "play-json" % "2.6.1",
        "com.typesafe.play" %% "play" % "2.6.1" % "optional",
        "com.typesafe.play" %% "play-test" % "2.6.1" % "optional"
      )
    case _ =>
      libraryDependencies.value ++ Seq(
        "com.typesafe.play" %% "play-json" % "2.5.4",
        "com.typesafe.play" %% "play" % "2.5.4" % "optional",
        "com.typesafe.play" %% "play-test" % "2.5.4" % "optional"
      )
  }
}
scoverage.ScoverageKeys.coverageMinimum := 80

scoverage.ScoverageKeys.coverageFailOnMinimum := true

resolvers in ThisBuild ++= Seq(
  Classpaths.sbtPluginReleases,
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)
