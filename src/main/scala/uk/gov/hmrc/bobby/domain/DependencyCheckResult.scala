package uk.gov.hmrc.bobby.domain


sealed trait DependencyCheckResult {
  def fail:Boolean
}

trait Fail extends DependencyCheckResult { val fail = true}
trait Pass extends DependencyCheckResult { val fail = false}

case class MandatoryFail(latest:String) extends DependencyCheckResult with Fail
case class NexusHasNewer(latest:String) extends DependencyCheckResult with Pass
object NotFoundInNexus extends DependencyCheckResult with Pass
object OK extends DependencyCheckResult with Pass
