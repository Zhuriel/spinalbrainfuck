// See README.md for license details.

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "1.0.0"
ThisBuild / organization     := ""
ThisBuild / transitiveClassifiers := Seq(Artifact.SourceClassifier)

val spinalVersion = "1.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "spinalbrainfuck",
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
      "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
      compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion),
      "org.scalactic" %% "scalactic" % "3.0.0",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    ),
    fork := true
  )
