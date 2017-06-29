import com.felstar.scalajs.leaflet.{ControlLayerOptions, Icon, IconOptions, L, LMap, LMapOptions, LayerGroup, Marker, MarkerOptions, TileLayerOptions}
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js.annotation.{JSExport, JSExportDescendentObjects, JSExportTopLevel}
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.reflect.Reflect.lookupLoadableModuleClass
import scala.util.{Failure, Success}
import upickle._

import scala.collection.immutable.{HashMap, HashSet}


class Bike(val id:Int, val latitude: Double, val longitude: Double){
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
      new Bike(id.num.toInt, lat.num, lon.num)
  }
}

case class Bikes(bikes: Array[Bike])
@JSExportTopLevel("BikeState")
class BikeState
@JSExportTopLevel("Rented")
case object Rented extends BikeState
@JSExportTopLevel("Available")
case object Available extends BikeState

@JSExportTopLevel("Map")
object Map extends {

  val mapOptions=LMapOptions.zoom(13).center((50.05, 19.95))
  val lmap = L.map("mapid", mapOptions)
  var bikesAvailable: Set[Bike] = Set()
  var bikesRented: Set[Bike] = Set()
  var markers: HashMap[Bike, (Marker, LayerGroup)] = new HashMap[Bike, (Marker, LayerGroup)]()
  val rented = L.layerGroup()
  val available = L.layerGroup()
  val leafOptions=IconOptions.iconSize((40,80))
  val icon_rented=L.icon(leafOptions.iconUrl("img/bike.ico"))
  val icon=L.icon(leafOptions.iconUrl("img/bike-icon.png"))
  val requestRented = HttpRequest("https://wavelo-live.herokuapp.com/rented")
  val requestAvailable = HttpRequest("https://wavelo-live.herokuapp.com/available")

  @JSExport
  def main(el: String): Unit = {

    update(Available)
    update(Rented)

    val tileLayer = L.tileLayer("https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiZmFuY2VsbHUiLCJhIjoiY2oxMHRzZm5zMDAyMDMycndyaTZyYnp6NSJ9.AJ3owakJtFAJaaRuYB7Ukw",
      TileLayerOptions.id("mapbox.streets").maxZoom(19).attribution("""Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors,
                                                                      |<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>,
                                                                      |Imagery © <a href="http://mapbox.com">Mapbox</a>""".stripMargin))
    val basemaps = literal("base"->tileLayer)
    val overlays = literal("Rented"->rented, "Available"->available)

    L.control.layers(basemaps, overLays = overlays, ControlLayerOptions.collapsed(false).hideSingleBase(true)).addTo(lmap)

    rented.addTo(lmap)
    available.addTo(lmap)
    tileLayer.addTo(lmap)


    L.control.scale().addTo(lmap)
  }

  @JSExport
  def update(state: BikeState):Unit = {
    val request = state match {
      case Rented => requestRented
      case _ => requestAvailable
    }
    request.send().onComplete({
      case res:Success[SimpleHttpResponse] =>
        correct(res, state)
      case e: Failure[SimpleHttpResponse] => println("Houston, we got a problem!")
    })
  }

  def changeState(bikes: Traversable[Bike], newState: BikeState): Unit ={
    val (ico, newLayer) = newState match {
      case Rented => (icon_rented, rented)
      case _ => (icon, available)
    }
    bikes.foreach(bike => {
      markers.get(bike) match {
        case Some((marker, layer)) =>
          markers = markers + (bike -> ((marker, newLayer)))
          layer.removeLayer(marker)
          newLayer.addLayer(marker)
          marker.setIcon(ico)
        case None => ()
      }
    })
  }

  def correct(res: Success[SimpleHttpResponse], typ: BikeState): Unit = {
    val newBikes = default.read[Bikes](res.get.body).bikes.toSet

    bikesAvailable = typ match {
      case Rented =>
        (bikesAvailable | bikesRented) &~ newBikes
      case _ =>
        newBikes
    }

    bikesRented = typ match {
      case Rented =>
        newBikes
      case _ =>
        (bikesAvailable | bikesRented) &~ newBikes
    }

    val (dest, sec, ico) = typ match {
      case Rented =>
        changeState(bikesAvailable &~ newBikes, Available)
        (rented, available, icon_rented)
      case _ =>
        changeState(bikesRented &~ newBikes, Rented)
        (available, rented, icon)
    }

    for (bike <- newBikes) {
      markers.get(bike) match {
        case Some((marker, layer)) =>
          if(layer != dest) {
            markers = markers + (bike -> ((marker, dest)))
            layer.removeLayer(marker)
            dest.addLayer(marker)
            marker.setIcon(ico)
          }
          marker.bindPopup(s"BikeID:\t${bike.id}<br>Latitude:\t${bike.latitude}<br>Longitude:\t${bike.longitude}")
          .setLatLng((bike.latitude, bike.longitude))
        case None =>
          val marker = L.marker((bike.latitude, bike.longitude), MarkerOptions.icon(ico))
            .bindPopup(s"BikeID:\t${bike.id}<br>Latitude:\t${bike.latitude}<br>Longitude:\t${bike.longitude}")
          dest.addLayer(marker)
          markers = markers + (bike -> ((marker, dest)))
      }
    }
  }
}