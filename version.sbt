
val lastReleaseVersion = Def.settingKey[String]("The version string of the last release")

lastReleaseVersion := "0.7-alpha-11"

val nextReleaseVersion = Def.settingKey[String]("The version string of the next release")

nextReleaseVersion := "0.7.0"

val releaseBranch = Def.settingKey[String]("The branch from which releases are made")

releaseBranch := "develop"

version in ThisBuild <<= Def.setting[String] {
  val lastVer = lastReleaseVersion.value
  val lastTag = s"v${lastVer}"
  if (Process("git", List("describe", "--abbrev=0", "--tags", "--match", lastTag, "HEAD")).run(BasicIO(new StringBuffer, None, false)).exitValue() != 0) {
    throw new RuntimeException(s"This build defines $lastVer as the most recently released version, but tag $lastTag is not the nearest tag!")
  } else {
    val distanceToTag = (Process(s"git rev-list ${lastTag}..HEAD") #| Process("wc -l")).!!.trim.toInt
    if (distanceToTag == 0) {
      lastVer
    } else {
      val distanceToReleaseBranch = (Process(s"git rev-list ${releaseBranch.value}..HEAD") #| Process("wc -l")).!!.trim.toInt
      val headSha = Process("git rev-parse --short HEAD").!!.stripLineEnd
      s"${nextReleaseVersion.value}-${distanceToTag - distanceToReleaseBranch}.${distanceToReleaseBranch}+${headSha}"
    }
  }
}
