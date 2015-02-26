package uk.gov.hmrc.bobby.conf

import scala.io.Source

class ConfigFile(fileName: String) {

  private val kvMap = {
    Source.fromFile(fileName)
      .getLines().toSeq
      .map(_.split("="))
      .map { case Array(key, value) => key.trim -> value.trim}.toMap
  }

  def getString(path: String) = kvMap(path)
  def hasPath(path: String) = kvMap.isDefinedAt(path)

}
