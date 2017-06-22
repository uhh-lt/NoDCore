name := "NoDCore"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"

libraryDependencies ++= Seq(
	"net.sf.jung" % "jung-api" % "2.0.1",
    "net.sf.jung" % "jung-graph-impl" % "2.0.1",
    "net.sf.jung" % "jung-algorithms" % "2.0.1",
	"org.scala-lang.modules" %% "scala-xml" % "1.0.4",
	"org.scalikejdbc" %% "scalikejdbc" % "2.2.+",
	"mysql" % "mysql-connector-java" % "5.1.35",
	"com.zaxxer" % "HikariCP" % "2.3.7",
	"com.typesafe" % "config" % "1.3.0",
	"com.typesafe.play" %% "play-json" % "2.3.9",
	"org.jsoup" % "jsoup" % "1.8.2",
	"org.apache.commons" % "commons-compress" % "1.9",
    "org.apache.commons" % "commons-lang3" % "3.4",
	"org.apache.jena" % "apache-jena-libs" % "2.13.0",
	"com.github.scopt" %% "scopt" % "3.3.0"
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (mergeStrategy) =>
  {
	case "META-INF/MANIFEST.MF" => MergeStrategy.rename
	case x => {
	   val strategy = mergeStrategy(x)
	   if (strategy == MergeStrategy.deduplicate) MergeStrategy.first
	   else strategy
	 }
	case _ => MergeStrategy.last
  }
}
