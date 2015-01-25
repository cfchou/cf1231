name := "cf1231"

version := "1.0"

scalaVersion := "2.10.4"

// resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers ++= {
  Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Local Maven Repository" at "file:///Users/cfchou/.m2/repository"
  )
}

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-client"    % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"      % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"    % akkaV,
    "org.apache.kafka"    %%  "kafka"           % "0.8.1.1"
      exclude("javax.jms", "jms")
      exclude("com.sun.jdmk", "jmxtools")
      exclude("com.sun.jmx", "jmxri"),
    "com.typesafe.play"   %%  "play-json"       % "2.3.7",
    // "com.fasterxml.jackson.core"  %   "jackson-core"  %   "2.4.4",
    // "com.typesafe.play"   %%  "play-iteratee"   % "2.3.7",
    "org.clapper"         %%  "grizzled-slf4j"  % "1.0.2",
    "ch.qos.logback"      %   "logback-classic" % "1.1.2",
    "joda-time"           %   "joda-time"       % "2.7",
    "org.scalacheck"      %%  "scalacheck"      % "1.11.6",
    "org.scalatest"       %%  "scalatest"       % "2.2.1"   % "test"
  )
}
