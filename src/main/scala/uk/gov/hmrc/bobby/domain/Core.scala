package uk.gov.hmrc.bobby.domain

import play.api.libs.json.{JsValue, Reads, Json}
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

  def getMandatoryVersionsJson(fileContents:String):Seq[VersionInfo]={
    Json.parse(fileContents).as[Seq[VersionInfo]]
  }

  def removeWhiteSpace(st:String) = st.replace("\\s+","")

  object Threashold{
    val formats = Json.format[Threashold]
  }
  object VersionInfo{
    implicit val r =new Reads[VersionInfo] {
      def reads(js: JsValue) = {
        VersionInfo(
          OrganizationName((js \ "organisation").as[String], (js \ "name").as[String]),
          (js \ "warn").asOpt[Threashold],
          (js \ "error").asOpt[Threashold]
        )
      }
    }
  }

  case class Threashold(version:String, message:String)
  case class VersionInfo(organisationName:OrganizationName, warn:Option[Threashold], error:Option[Threashold])

  case class OrganizationName(module:String, revision:String)
  object OrganizationName{
    def apply(module:ModuleID):OrganizationName = OrganizationName(module.organization, module.name)
  }

}
