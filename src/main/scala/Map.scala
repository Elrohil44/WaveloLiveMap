import com.felstar.scalajs.leaflet.{IconOptions, L, LMap, LMapOptions, Marker, MarkerOptions, TileLayerOptions}
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}
import upickle._

import scala.collection.immutable.{HashMap, HashSet}


class Bike(val id:Int, val latitude: Double, val longitude: Double){

  def equal(other: Bike): Boolean = {
    other.id == id && other.latitude == latitude && other.longitude == longitude
  }

  override def equals(other: Any): Boolean = other match {
    case that: Bike => (that canEqual this) && (id == that.id)
    case _ => false
  }

  override def hashCode(): Int = 0

  def canEqual(other: Any): Boolean = other.isInstanceOf[Bike]
}

object Bike{
  implicit val bike2Writer = upickle.default.Writer[Bike]{
    case t => Js.Obj(("id", Js.Num(t.id)), ("latitude", Js.Num(t.latitude)), ("longitude", Js.Num(t.longitude)))
  }
  implicit val thing2Reader = upickle.default.Reader[Bike]{
    case Js.Obj(("id", id), ("latitude", lat), ("longitude", lon)) =>
      new Bike(id.asInstanceOf[Int], lat.asInstanceOf[Double], lon.asInstanceOf[Double])
  }
}

case class Bikes(bikes: Array[Bike])

@JSExport
object Map extends {


  val mapOptions=LMapOptions.zoom(13).center((50.05, 19.95))
  val lmap = L.map("mapid", mapOptions)
  var bikes: HashSet[Bike] = new HashSet[Bike]()
  var markers: HashMap[Bike, Marker] = new HashMap[Bike, Marker]()

  val leafOptions=IconOptions.iconSize((30,60))
  val iconc=L.icon(leafOptions.iconUrl("img/bike.ico"))
  val icon=L.icon(leafOptions.iconUrl("img/bike-icon.png"))
  val request = HttpRequest("https://wavelo-live.herokuapp.com/toupdate")
  var lat = 50.05
  var lon = 19.95
  val marker = L.marker((lat, lon), MarkerOptions.icon(icon)).bindPopup(s"BikeID:\t${5}\nLatitude:\t${lat}\nLongitude:\t${lon}")
  @JSExport
  def main(el: String): Unit = {

    request.send().onComplete({
      case res:Success[SimpleHttpResponse] =>
        correct(res)
      case e: Failure[SimpleHttpResponse] => println("Huston, we got a problem!")
    })
//    marker.addTo(lmap)
    val tileLayer = L.tileLayer("https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiZmFuY2VsbHUiLCJhIjoiY2oxMHRzZm5zMDAyMDMycndyaTZyYnp6NSJ9.AJ3owakJtFAJaaRuYB7Ukw",
      TileLayerOptions.id("mapbox.streets").maxZoom(19).attribution("""Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors,
                                                                      |<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>,
                                                                      |Imagery © <a href="http://mapbox.com">Mapbox</a>""".stripMargin))
    tileLayer.addTo(lmap)

    L.control.scale().addTo(lmap)

//    lmap
  }

  @JSExport
  def update():Unit = {
    request.send().onComplete({
      case res:Success[SimpleHttpResponse] =>
        correct(res)
//        for (i <- 1 to 1000){
//          lat += 0.001
//          marker.setLatLng((lat, lon)).bindPopup(s"BikeID:\t${5}\nLatitude:\t${lat}\nLongitude:\t${lon}")
//        }
      case e: Failure[SimpleHttpResponse] => println("Huston, we got a problem!")
    })
  }

  def correct(res: Success[SimpleHttpResponse]): Unit = {
    val newBikes: HashSet[Bike] = new HashSet() ++ default.read[Bikes](res.get.body).bikes
    for (bike <- newBikes) {
      markers.get(bike) match {
        case Some(marker) =>

          marker.bindPopup(s"BikeID:\t${bike.id}\nLatitude:\t${bike.latitude}\nLongitude:\t${bike.longitude}")
          .setLatLng((bike.latitude, bike.longitude))
        case None =>
          val marker = L.marker((bike.latitude, bike.longitude), MarkerOptions.icon(icon))
            .bindPopup(s"BikeID:\t${bike.id}\nLatitude:\t${bike.latitude}\nLongitude:\t${bike.longitude}").addTo(lmap)
          markers = markers + ((bike, marker))
      }
    }
    for (bike <- bikes -- newBikes) {
      markers.get(bike) match {
        case Some(marker) =>
          lmap.removeLayer(marker)
          markers = markers - bike
        case None => ()
      }
    }
    println("xD")
    bikes = newBikes
  }
}