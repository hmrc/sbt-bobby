package uk.gov.hmrc.bobby.domain

case class VersionRange(lowerBound: Option[Version], lowerBoundInclusive: Boolean, upperBound: Option[Version], upperBoundInclusive: Boolean) {
  def includes(version: Version): Boolean = {
    val lbRange = lowerBound.fold(true)(lb => version.isAfter(lb) || (lowerBoundInclusive && lb.equals(version)))
    val ubRange = upperBound.fold(true)(ub => version.isBefore(ub) || (upperBoundInclusive && ub.equals(version)))
    lbRange && ubRange
  }

}

/**
 *
 * Supporting the following expressions:
 * Range               | Meaning
 * (,1.0.0]            | x <= 1.0.0
 * [1.0.0]             | Hard requirement on 1.0.0
 * [1.2.0,1.3.0]       | 1.2.0 <= x <= 1.3.0
 * [1.0.0,2.0.0)       | 1.0.0 <= x < 2.0.0
 * [1.5.0,)            | x >= 1.5.0
 *
 * All versions must have all 3 numbers, 1.0 is not supported for example
 *
 * @throws IllegalArgumentException when an illegal format is used
 */
object VersionRange {

  implicit def toVersion(v: String): Version = Version(v)

  val ValidFixedVersion = """^\[(\d+\.\d+.\d+)\]""".r
  val ValidVersionRangeLeftOpen = """^\(,?(\d+\.\d+.\d+)[\]\)]""".r
  val ValidVersionRangeRightOpen = """^[\[\(](\d+\.\d+.\d+),[\]\)]""".r
  val ValidVersionRangeBetween = """^[\[\(](\d+\.\d+.\d+),(\d+\.\d+.\d+)[\]\)]""".r

  def apply(range: String): VersionRange = {
    range.replaceAll(" ", "") match {
      case ValidFixedVersion(v) => VersionRange(Some(v), true, Some(v), true)
      case ValidVersionRangeLeftOpen(v) => VersionRange(None, false, Some(v), range.endsWith("]"))
      case ValidVersionRangeRightOpen(v) => VersionRange(Some(v), range.startsWith("["), None, false)
      case ValidVersionRangeBetween(v1, v2) => VersionRange(Some(v1), range.startsWith("["), Some(v2), range.endsWith("]"))
      case _ => throw new IllegalArgumentException(s"'$range' is not a valid range expression")
    }
  }
}
