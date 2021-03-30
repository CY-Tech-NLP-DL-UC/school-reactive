name := "WordCount"
version := "1.0"
scalaVersion := "2.13.0"

resolvers += Resolver.typesafeRepo("releases")

val AkkaVersion = "2.6.12"
val AkkaHttpVersion = "10.2.3"
libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3"
)
