package carldata.borsuk.storms

import java.time.{Duration, Instant, LocalDateTime}
import java.util.UUID.randomUUID

import carldata.borsuk.helper.DateTimeHelper.dtToInstant
import carldata.borsuk.storms.ApiObjects.FitStormsParams
import carldata.series.Sessions.Session
import carldata.series.{Gen, Sessions, TimeSeries}

import scala.annotation.tailrec
import scala.collection.immutable

object Storms {

  case class StormParams(session: Session, sessionWindow: Duration, values: Vector[Double], childIds: Seq[String])

  /** Get all storms (with merged storms) from rainfall */
  def getAllStorms(rainfall: TimeSeries[Double]): List[(String, StormParams)] = {
    val baseSessions: List[(String, StormParams)] = Sessions.findSessions(rainfall)
      .zipWithIndex
      .map(x =>
        x._2.toString ->
          StormParams(x._1, rainfall.resolution, rainfall.slice(x._1.startIndex
            , x._1.endIndex.plusSeconds(rainfall.resolution.getSeconds)).values, Seq())
      ).toList

    if (baseSessions != Nil) {
      val listOfSessionWindows: Seq[Duration] =
        baseSessions.map(x => x._2.session.endIndex).zip(baseSessions.tail.map(x => x._2.session.startIndex))
          .map(x => Duration.between(x._1, x._2))
          .distinct.sorted

      val mergedSession = mergeSessions(baseSessions, baseSessions, listOfSessionWindows, rainfall.resolution)
      mergedSession
    }
    else List()
  }

  @tailrec
  def mergeSessions(prev: List[(String, StormParams)], res: List[(String, StormParams)]
                            , sessionWindows: Seq[Duration], resolution: Duration): List[(String, StormParams)] = {
    if (sessionWindows.isEmpty) res
    else {
      val sessionWindow = sessionWindows.head
      val next: List[(String, StormParams)] = prev.tail.foldLeft[List[(String, StormParams)]](List(prev.head))((zs, x) => {
        if (sessionWindow.compareTo(Duration.between(zs.head._2.session.endIndex, x._2.session.startIndex)) >= 0) {
          val gapDuration = Duration.between(zs.head._2.session.endIndex, x._2.session.startIndex)

          val gapValues = for (_ <- 1 until (gapDuration.toMillis / resolution.toMillis).toInt) yield 0.0

          (randomUUID().toString
            , StormParams(Session(zs.head._2.session.startIndex, x._2.session.endIndex)
            , sessionWindow
            , zs.head._2.values ++ gapValues ++ x._2.values
            , zs.head._2.childIds ++ Seq(zs.head._1, x._1))
          ) :: zs.tail
        } //merge sessions
        else
          x :: zs
      }).reverse
      mergeSessions(next, res ++ next, sessionWindows.tail, resolution)
    }
  }
}

class Storms(modelType: String, id: String) {
  var model: immutable.HashMap[String, Storms.StormParams] = immutable.HashMap.empty[String, Storms.StormParams]
  var buildNumber: Int = 0

  /** Fit model */
  def fit(params: FitStormsParams): Unit = {

    if (params.rainfall.values.nonEmpty) {

      val resolution = params.rainfall.resolution
      val endIndex: LocalDateTime = params.rainfall.startDate.plusSeconds(resolution.getSeconds * params.rainfall.values.length)
      val index: Seq[Instant] = Gen.mkIndex(dtToInstant(params.rainfall.startDate), dtToInstant(endIndex), params.rainfall.resolution)
      val rainfall: TimeSeries[Double] = TimeSeries(index.toVector, params.rainfall.values.toVector)

      val mergedSession: List[(String, Storms.StormParams)] = Storms.getAllStorms(rainfall)
      model = immutable.HashMap(mergedSession: _*)
      buildNumber += 1
    }
  }

  /**
    * List all storms
    */
  def list(sessionWindow: Duration): Seq[(String, Session)] = {
    val lessOrEqualModel = model.filter(x => x._2.sessionWindow.compareTo(sessionWindow) <= 0)
      .toSeq
      .sortBy(_._2.sessionWindow)

    val childIds = lessOrEqualModel.flatMap(_._2.childIds).distinct

    lessOrEqualModel.filter(x => !childIds.contains(x._1))
      .map(x => (x._1, x._2.session))
  }

  /** Get the storm */
  def get(storm_id: String): Option[(Instant, Instant, Seq[Double])] = {
    model.filter(_._1 == storm_id)
      .map(x => (x._2.session.startIndex, x._2.session.endIndex, x._2.values))
      .headOption
  }
}
