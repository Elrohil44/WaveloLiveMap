name := "WaveloLiveMap"
import com.lihaoyi.workbench.Plugin._

workbenchSettings

bootSnippet := "example.T1().main(document.getElementById('mydiv'));"
libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.4.3"
libraryDependencies += "fr.hmil" %%% "roshttp" % "2.0.1"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.fancellu.scalajs-leaflet" % "scalajs-leaflet_sjs0.6_2.11" % "v0.1"

enablePlugins(ScalaJSPlugin)
publishArtifact:= false
scalaVersion in ThisBuild := "2.11.8"
    