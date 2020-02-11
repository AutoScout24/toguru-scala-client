ThisBuild / organization := "com.autoscout24"
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / bintrayOrganization := Some("autoscout24")

ThisBuild / gitVersioningSnapshotLowerBound := "3.0.0"
// Snapshots cannot be published on BinTray
ThisBuild / version := (ThisBuild / version).value.replaceAll("\\-SNAPSHOT$", "")

ThisBuild / resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("autoscout24", "maven"),
  "Typesafe repository".at("https://repo.typesafe.com/typesafe/releases/")
)

ThisBuild / scoverage.ScoverageKeys.coverageMinimum := 80
ThisBuild / scoverage.ScoverageKeys.coverageFailOnMinimum := true

addCommandAlias("format", "; scalafmt; test:scalafmt; scalafmtSbt")
addCommandAlias("formatCheck", "; scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck")

val versions = new {
  val scala212                  = "2.12.10"
  val scala213                  = "2.13.1"
  val play26                    = "2.6.25"
  val play27                    = "2.7.4"
  val play28                    = "2.8.0"
  val circe                     = "0.12.3"
  val sttp                      = "2.0.0-RC6"
  val slf4j                     = "1.7.30"
  val phiAccuralFailureDetector = "0.0.5"
  val failsafe                  = "2.3.1"
  val scalatest                 = "3.1.0"
  val mockito                   = "1.10.4"
}

val dependencies = new {
  def play(version: String) = Seq(
    "com.typesafe.play" %% "play"      % version % Provided,
    "com.typesafe.play" %% "play-test" % version % Test,
  )
}

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs: _*)
  .aggregate(play.projectRefs: _*)
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
    ),
    libraryDependencies ++= Seq(
      "io.circe"                     %% "circe-core"                  % versions.circe,
      "io.circe"                     %% "circe-parser"                % versions.circe,
      "com.softwaremill.sttp.client" %% "core"                        % versions.sttp,
      "org.slf4j"                    % "slf4j-api"                    % versions.slf4j,
      "org.komamitsu"                % "phi-accural-failure-detector" % versions.phiAccuralFailureDetector,
      "net.jodah"                    % "failsafe"                     % versions.failsafe,
      "org.mockito"                  %% "mockito-scala-scalatest"     % versions.mockito % "test",
      "org.scalatest"                %% "scalatest"                   % versions.scalatest % "test",
      "org.slf4j"                    % "slf4j-nop"                    % versions.slf4j % "test",
    )
  )
  .jvmPlatform(
    scalaVersions = Seq(versions.scala213, versions.scala212),
  )

lazy val play = (projectMatrix in file("play"))
  .enablePlugins(SemVerPlugin)
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % versions.scalatest % "test",
      "org.slf4j"     % "slf4j-nop"  % versions.slf4j     % "test",
    )
  )
  .customRow(
    scalaVersions = Seq(versions.scala213, versions.scala212),
    axisValues = Seq(PlayAxis.play28, VirtualAxis.jvm),
    _.settings(
      name := "toguru-scala-client-play28",
      libraryDependencies ++= dependencies.play(versions.play28),
    )
  )
  .customRow(
    scalaVersions = Seq(versions.scala213, versions.scala212),
    axisValues = Seq(PlayAxis.play27, VirtualAxis.jvm),
    _.settings(
      name := "toguru-scala-client-play27",
      libraryDependencies ++= dependencies.play(versions.play27),
    )
  )
  .customRow(
    scalaVersions = Seq(versions.scala212),
    axisValues = Seq(PlayAxis.play26, VirtualAxis.jvm),
    _.settings(
      name := "toguru-scala-client-play26",
      libraryDependencies ++= dependencies.play(versions.play26),
    )
  )
