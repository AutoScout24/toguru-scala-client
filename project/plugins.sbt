resolvers += Resolver.url(
  "as24-ivy-releases",
  new URL("https://fast.services.as24.tech/artifactory/public/")
)(Resolver.ivyStylePatterns)
resolvers += "fast-releases".at("https://fast.services.as24.tech/artifactory/public/")
credentials += (
  if (sys.env.contains("FAST_USER"))
    Credentials(
      "Artifactory Realm",
      "fast.services.as24.tech",
      sys.env("FAST_USER"),
      sys.env("FAST_TOKEN")
    )
  else Credentials(Path.userHome / ".sbt" / ".credentials")
)

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.6.1")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.2.7")

addSbtPlugin("com.rallyhealth.sbt" % "sbt-git-versioning" % "1.6.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.5.2")

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.2")
