scalaVersion := "2.12.2"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += Opts.resolver.sonatypeSnapshots

lazy val akkaVersion = "2.5.3"



lazy val commonSettings = Seq(
  version := "0.1.3",
  scalaVersion := "2.12.2",
  libraryDependencies ++= Seq(
    "com.google.zxing" % "javase" % "3.3.0",
    "com.github.sarxos" % "webcam-capture" % "0.3.11",
    "com.github.sarxos" % "v4l4j" % "0.9.1-r507",
    "com.github.sarxos" % "webcam-capture-driver-v4l4j" % "0.3.11",
    "javax.mail" % "javax.mail-api" % "1.5.5",
    "com.sun.mail" % "javax.mail" % "1.5.5",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.github.scullxbones" %% "akka-persistence-mongo-casbah" % "2.0.2",
    "org.mongodb" %% "casbah" % "3.1.1",
    "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1",
    "com.google.apis" % "google-api-services-gmail" % "v1-rev70-1.22.0",
    "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
    "com.google.api-client" % "google-api-client" % "1.22.0"

  )
)

lazy val root = (project in file(".")).
  settings(
    commonSettings,
    name := "root"
  )
 
lazy val raspi = (project in file("techin-raspi")).
  settings(
    commonSettings,
    name := "techin-raspi",
    fork := true,
    assemblyJarName in assembly := "raspi.jar"
  ).dependsOn(techin % "compile")

lazy val techinClient = (project in file("techin-client")).
  settings(
    commonSettings,
    name := "techin-client",
    fork := true,
    assemblyJarName in assembly := "client.jar",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "8.0.102-R11",
      "com.github.tototoshi" %% "scala-csv" % "1.3.4"
    )
  ).dependsOn(techin % "compile")


lazy val techin = (project in file("techin")).
  settings(
    commonSettings,
    name := "techin"
  )
