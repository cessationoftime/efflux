import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object AkkaAmqpBuild extends Build {
  import dependencies._
 

    lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )
  
  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }
  
  lazy val standardSettings = Project.defaultSettings ++ formatSettings ++ Seq(
  	resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/releases/",
	resolvers += "repository.jboss.org" at "http://repository.jboss.org/nexus/content/groups/public/",	
    organization := "com.github.cessationoftime",
    version			 := "0.4.0",
    scalaVersion := "2.10.0"
  )
  
  lazy val root = Project(
    id        = "efflux",
    base      = file("."),
    settings = standardSettings ++ Seq(
    	libraryDependencies ++= Seq( 
	      junit,
		  sbtJunitSupport,
		netty,
	      slf4jApi,
        slf4jLog4j12,
		log4j)
    )
  )
}

object dependencies {
    def sbtJunitSupport = "com.novocode" % "junit-interface" % "0.8" % "test->default"
	def junit = "junit" % "junit" % "4.7" % "test"
	def netty = "org.jboss.netty" % "netty" % "3.2.2.Final"
	def slf4jApi = "org.slf4j" % "slf4j-api" % "1.6.1"
	def slf4jLog4j12 = "org.slf4j" % "slf4j-log4j12" % "1.6.1" % "runtime"
	def log4j = "log4j" % "log4j" % "1.2.16" % "runtime"
}
