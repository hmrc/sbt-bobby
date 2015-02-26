package uk.gov.hmrc.bobby.domain

case class Dependency(organisation: String, name: String) {

  override def equals(obj: scala.Any): Boolean = obj match {
    case d: Dependency => d.organisation.equals(organisation) && d.name.equals(name)
    case _ => false
  }
}