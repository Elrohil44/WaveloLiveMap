name := "WaveloLiveMap"

//bootSnippet := "example.T1().main(document.getElementById('mydiv'));"
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.4.3"
libraryDependencies += "fr.hmil" %%% "roshttp" % "2.0.1"
libraryDependencies += "com.github.fancellu.scalajs-leaflet" %%% "scalajs-leaflet" % "v0.1"

enablePlugins(ScalaJSPlugin)
scalaVersion in ThisBuild := "2.11.8"
    