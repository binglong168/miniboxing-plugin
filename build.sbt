
name := "miniboxing"

scalaVersion in Global := "2.11.7"

crossScalaVersions in Global := Seq("2.11.7", "2.10.6") // "2.12.0-SNAPSHOT" - partest not published :(

version in Global := "0.4-SNAPSHOT"

organization in Global := "org.scala-miniboxing.plugins"

organizationName in Global := "LAMP/EPFL"

organizationHomepage in Global := Some(url("http://lamp.epfl.ch"))

licenses in Global := Seq("BSD-style" -> url("http://scala-miniboxing.org/license.html"))

homepage in Global := Some(url("http://scala-miniboxing.org"))

resolvers in ThisBuild ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
