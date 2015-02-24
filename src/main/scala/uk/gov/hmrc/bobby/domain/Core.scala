package uk.gov.hmrc.bobby.domain

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import sbt.ModuleID


object Core {

  def getMandatoryVersionsJson(fileContents:String):Map[DependencyName, Seq[Exclude]]={
    println(Json.prettyPrint(Json.parse(fileContents)))
    Json.parse(fileContents).as[Seq[DependencyExcludes]]
      .map { vi => vi.organisationName -> vi.excludes }
      .toMap
  }

  def removeWhiteSpace(st:String) = st.replace("\\s+","")

  object Threashold{
    implicit val formats = Json.format[Threashold]
  }

  object DependencyExcludes{

    implicit val r:Reads[DependencyExcludes] = (
      (JsPath \ "organisation").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "excludes").read[Seq[Exclude]]
    )((a, b, c) => DependencyExcludes.apply(DependencyName(a, b), c))
  }

  case class Threashold(version:String, message:String)
  case class DependencyExcludes(organisationName:DependencyName, excludes:Seq[Exclude])
  case class DependencyName(organisation:String, name:String)

  object DependencyName{
    def apply(module:ModuleID):DependencyName = DependencyName(module.organization, module.name)
  }
}
