package carldata.borsuk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main {

  private val Log = LoggerFactory.getLogger(Main.getClass.getName)

  implicit val system: ActorSystem = ActorSystem("dss")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class Params(dataPath: String, statsDHost: String)

  /** Routing */
  def route(models: ModelsDB): Route = {

    path("model" / Remaining) { modelName =>
      post {
        entity(as[String]) { data =>
          models.getModel(modelName).foreach(_.addSample(data))
          complete("ok")
        }
      }
    } ~ pathPrefix("static") {
      getFromDirectory("static")
    }
  }

  /** Parse application arguments */
  def parseArg(args: Array[String]): Params = {
    val dataPath = stringArg(args, "data-path", "data")
    val statsDHost = stringArg(args, "statsd-host")
    Params(dataPath, statsDHost)
  }

  /** Parse single argument */
  def stringArg(args: Array[String], key: String, default: String = ""): String = {
    val name = "--" + key + "="
    args.find(_.contains(name)).map(_.substring(name.length)).getOrElse(default).trim
  }

  def main(args: Array[String]) {
    val params = parseArg(args)
    StatsD.init("dss", params.statsDHost)
    val storage = new FileStorage(params.dataPath)
    val models = new ModelsDB(storage, "config.json")


    Log.info("Server started. Open http://localhost:7074/static/index.html")
    Await.result(Http().bindAndHandle(route(models), "0.0.0.0", 7074), Duration.Inf)
  }

}