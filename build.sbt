import sbtassembly.AssemblyKeys.assembly

enablePlugins(GatlingPlugin, AssemblyPlugin)

scalaVersion := "3.1.0"

assembly / mainClass := Some("Engine")

assemblyJarName in assembly := "gatling-execution.jar"

assemblyMergeStrategy := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x if x.contains("META-INF/versions/9/module-info.class") => MergeStrategy.discard
  case x if x.contains("module-info.class") => MergeStrategy.discard

  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}
val akkaversion = "2.6.17"
val akkahttpversion ="10.2.7"

libraryDependencies ++= Seq(
  "io.gatling" % "gatling-core" % "3.7.2" % "provided",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.7.2",
  "io.gatling"            % "gatling-test-framework"    % "3.7.2"
)
