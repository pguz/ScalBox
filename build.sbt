import com.typesafe.sbt.SbtStartScript

name := "ScalBox"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= {
  val verAkka         = "2.3.9"
  val verCommonsCodec = "1.10"
  val verCommonsIo    = "2.4"
  val verRxScala      = "0.23.0"
  val verRxSwing      = "0.21.0"
  val verSwing        = "1.0.1"
  val verScalatest    = "2.1.3"
  val verSlf4j        = "1.7.12"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"      % verAkka,
    "com.typesafe.akka"       %%  "akka-remote"     % verAkka,
    "com.typesafe.akka"       %%  "akka-testkit"    % verAkka,
    "commons-codec"           %   "commons-codec"   % verCommonsCodec,
    "commons-io"              %   "commons-io"      % verCommonsIo,
    "io.reactivex"            %%  "rxscala"         % verRxScala,
    "io.reactivex"            %   "rxswing"         % verRxSwing,
    "org.scala-lang"          %   "scala-compiler"  % scalaVersion.value,
    "org.scala-lang.modules"  %%  "scala-swing"     % verSwing,
    "org.scalatest"           %   "scalatest_2.11"  % verScalatest,
    "org.slf4j"               %   "slf4j-api"       % verSlf4j,
    "org.slf4j"               %   "slf4j-simple"    % verSlf4j
  )
}

seq(SbtStartScript.startScriptForClassesSettings: _*)