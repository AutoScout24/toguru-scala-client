import sbt._

object PlayAxis {
  lazy val play26 = PlayAxis("2.6")
  lazy val play27 = PlayAxis("2.7")
  lazy val play28 = PlayAxis("2.8")
}
case class PlayAxis(version: String) extends VirtualAxis.WeakAxis {
  def idSuffix: String        = s"Play${version.replace('.', '_')}"
  def directorySuffix: String = s"play_$version"
}
