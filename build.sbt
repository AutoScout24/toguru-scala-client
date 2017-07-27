lazy val root = project.in(file(".")).enablePlugins(GitVersioning)

name := "toguru-scala-client"

git.baseVersion := "1.1.1"

organization in ThisBuild := "com.autoscout24"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("autoscout24")

crossScalaVersions in ThisBuild := Seq("2.12.1", "2.11.8")

scalaVersion in ThisBuild := "2.12.2"

scalacOptions in ThisBuild ++= Seq("-unchecked",
                                   "-deprecation",
                                   "-feature",
                                   "-Xfatal-warnings",
                                   "-Yno-adapted-args",
                                   "-Xmax-classfile-name",
                                   "130")

val playVersion = "2.6.1"

resolvers += Resolver.jcenterRepo

libraryDependencies in ThisBuild ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.scalactic" %% "scalactic" % "3.0.3",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
  "org.komamitsu" % "phi-accural-failure-detector" % "0.0.3",
  "com.typesafe.play" %% "play-json" % playVersion,
  "com.typesafe.play" %% "play" % playVersion % "optional",
  "com.typesafe.play" %% "play-test" % playVersion % "optional",
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "com.hootsuite" %% "scala-circuit-breaker" % "1.0.1",
  "org.mockito" % "mockito-core" % "2.0.8-beta" % "test",
  "org.http4s" %% "http4s-dsl" % "0.17.0-M3" % "test",
  "org.http4s" %% "http4s-blaze-server" % "0.17.0-M3" % "test"
)

scoverage.ScoverageKeys.coverageMinimum := 80

scoverage.ScoverageKeys.coverageFailOnMinimum := true

resolvers in ThisBuild ++= Seq(
  Classpaths.sbtPluginReleases,
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  Resolver.url("scoverage-bintray",
               url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(
    Resolver.ivyStylePatterns)
)
