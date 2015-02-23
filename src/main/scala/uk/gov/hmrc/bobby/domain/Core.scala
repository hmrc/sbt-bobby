package uk.gov.hmrc.bobby.domain

import org.joda.time.{LocalDate, LocalTime, DateTime}
import play.api.libs.json.{JsPath, Reads, Json}
import play.api.libs.functional.syntax._

import sbt.ModuleID


object Core {

  def getMandatoryVersions(fileContents:String):Map[OrganizationName, String]={
    fileContents.split('\n')
      .filterNot { line => line.startsWith("#") }
      .map { line => removeWhiteSpace(line).split(',') match {
        case Array(org, name, version) => OrganizationName(org, name) -> version
      }}
      .toMap
  }

  def getMandatoryVersionsJson(fileContents:String):Map[OrganizationName, String]={
    println(Json.prettyPrint(Json.parse(fileContents)))
    Json.parse(fileContents).as[Seq[VersionInfo]]
      .map { vi => vi.organisationName -> vi.error.get.version }
      .toMap
  }

  def removeWhiteSpace(st:String) = st.replace("\\s+","")

  object Threashold{
    implicit val formats = Json.format[Threashold]
  }
  object VersionInfo{
    implicit val r:Reads[VersionInfo] = (
      (JsPath \ "organisation").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "error").readNullable[Threashold]
    )((a, b, c) => VersionInfo.apply(OrganizationName(a, b), c))
  }

  case class Threashold(version:String, message:String)
  case class VersionInfo(organisationName:OrganizationName,error:Option[Threashold])
  case class OrganizationName(module:String, revision:String)

  object OrganizationName{
    def apply(module:ModuleID):OrganizationName = OrganizationName(module.organization, module.name)
  }

  def verify(dependencyVersion:Version, excludes:Seq[Exclude]):DependencyCheckResult={
    ???
  }

  case class Exclude(range:String, reason:String, date:LocalDate)
}
