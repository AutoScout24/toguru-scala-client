ThisBuild / organization := "com.autoscout24"
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / bintrayOrganization := Some("autoscout24")

ThisBuild / resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("autoscout24", "maven"),
  "Typesafe repository".at("https://repo.typesafe.com/typesafe/releases/")
)

ThisBuild / scoverage.ScoverageKeys.coverageMinimum := 80
ThisBuild / scoverage.ScoverageKeys.coverageFailOnMinimum := true

addCommandAlias("format", "; scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("formatCheck", "; scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck")

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs: _*)
  .settings(publish / skip := true)

lazy val core = projectMatrix
  .in(file("core"))
  .enablePlugins(SemVerPlugin)
  .settings(
    name := "toguru-scala-client",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Yno-adapted-args",
      "-Xmax-classfile-name",
      "130"
    ),
    libraryDependencies ++= Seq(
      "org.scalaj"                 %% "scalaj-http"                 % "2.3.0",
      "com.typesafe.scala-logging" %% "scala-logging"               % "3.5.0",
      "commons-io"                 % "commons-io"                   % "2.4",
      "io.dropwizard.metrics"      % "metrics-core"                 % "3.1.0",
      "org.komamitsu"              % "phi-accural-failure-detector" % "0.0.3",
      "org.scalactic"              %% "scalactic"                   % "3.0.3",
      "com.hootsuite"              %% "scala-circuit-breaker"       % "1.0.2",
      "org.mockito"                % "mockito-core"                 % "2.0.8-beta" % "test",
      "org.scalatest"              %% "scalatest"                   % "3.0.3" % "test",
      "org.http4s"                 %% "http4s-dsl"                  % "0.17.0-M3" % "test",
      "org.http4s"                 %% "http4s-blaze-server"         % "0.17.0-M3" % "test"
    )
  )
  .jvmPlatform(
    scalaVersions = Seq("2.12.3", "2.11.8"),
    settings = Seq(
      libraryDependencies ++= {
        val playVersion = scalaBinaryVersion.value match {
          case "2.12" => "2.6.1"
          case "2.11" => "2.5.4"
        }
        Seq(
          "com.typesafe.play" %% "play-json" % playVersion,
          "com.typesafe.play" %% "play"      % playVersion % "optional",
          "com.typesafe.play" %% "play-test" % playVersion % "optional"
        )
      }
    )
  )
