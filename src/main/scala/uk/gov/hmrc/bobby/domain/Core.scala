package uk.gov.hmrc.bobby.domain

import org.joda.time.{LocalDate, LocalTime, DateTime}
import play.api.libs.json.{JsPath, Reads, Json}
import play.api.libs.functional.syntax._

import sbt.ModuleID


object Core {

  def getMandatoryVersionsJson(fileContents:String):Map[OrganizationName, Seq[Exclude]]={
    println(Json.prettyPrint(Json.parse(fileContents)))
    Json.parse(fileContents).as[Seq[DependencyExcludes]]
      .map { vi => vi.organisationName -> vi.excludes }
      .toMap
  }

  def removeWhiteSpace(st:String) = st.replace("\\s+","")

  object Threashold{
    implicit val formats = Json.format[Threashold]
  }
  object Exclude{
    implicit val formats = Json.format[Exclude]
  }
  object DependencyExcludes{
    implicit val r:Reads[DependencyExcludes] = (
      (JsPath \ "organisation").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "excludes").read[Seq[Exclude]]
    )((a, b, c) => DependencyExcludes.apply(OrganizationName(a, b), c))
  }

  case class Threashold(version:String, message:String)
  case class DependencyExcludes(organisationName:OrganizationName, excludes:Seq[Exclude])
  case class OrganizationName(module:String, revision:String)

  object OrganizationName{
    def apply(module:ModuleID):OrganizationName = OrganizationName(module.organization, module.name)
  }

  def verify(dependencyVersion:Version, excludes:Seq[Exclude]):DependencyCheckResult={
    OK
  }

  case class Exclude(range:String, reason:String, from:LocalDate)
}
