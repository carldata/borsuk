package carldata.borsuk.helper

import java.time._
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

import org.slf4j.LoggerFactory

/**
  * Created by Krzysztof Langner on 2018-03-21.
  */
object DateTimeHelper {

  private val Log = LoggerFactory.getLogger(this.getClass.getName)

  def dateParse(str: String): LocalDateTime = {
    val formatter = new DateTimeFormatterBuilder()
      .parseCaseInsensitive
      .appendValue(ChronoField.YEAR)
      .appendLiteral('-')
      .appendValue(ChronoField.MONTH_OF_YEAR)
      .appendLiteral('-')
      .appendValue(ChronoField.DAY_OF_MONTH)
      .optionalStart.appendLiteral(' ').optionalEnd
      .optionalStart.appendLiteral('T').optionalEnd
      .optionalStart
      .appendValue(ChronoField.HOUR_OF_DAY)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR)
      .optionalStart.appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE).optionalEnd
      .optionalStart.appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd
      .optionalStart.appendLiteral('Z').optionalEnd
      .optionalEnd
      .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
      .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
      .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
      .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
      .toFormatter

    LocalDateTime.parse(str.replaceAll("\"", ""), formatter)
  }

  def instantToLDT(dt: Instant): LocalDateTime = {
    LocalDateTime.ofInstant(dt, ZoneOffset.UTC)
  }

  def isDateHoliday(dt: LocalDate): Boolean = {
    dt.getDayOfWeek.getValue == 6 || dt.getDayOfWeek.getValue == 7
  }

  def isHoliday(dt: Instant): Boolean = {
    val day = instantToLDT(dt).toLocalDate
    day.getDayOfWeek.getValue == 6 || day.getDayOfWeek.getValue == 7
  }

  def dayToInstant(dt: LocalDate): Instant = {
    Instant.ofEpochSecond(dt.toEpochDay * 86400)
  }

  def dtToInstant(dt: LocalDateTime): Instant = {
    dt.toInstant(ZoneOffset.UTC)
  }

  def instantToDay(dt: Instant): LocalDate = {
    LocalDate.ofEpochDay(dt.getEpochSecond / 86400)
  }

  def instantToTime(dt: Instant): LocalTime = {
    instantToLDT(dt).toLocalTime
  }

  /***
    * Time logger for function
    * @param text comment what you want to show
    * @param block function which we want to calculate the time of running
    * @tparam R type of function block
    * @return results of function block
    * @example DateTimeHelper.logTime("PVCHelper.loadModel with path: " + path + " and id: " + id, PVCHelper.loadModelBinary[EnvelopeFileContent](path, id))
    * @example DateTimeHelper.logTime("loadModel in RDIIList type: " + mt + " and id: " + id, loadModel(mt, id))
    */
  def logTime[R](text: String, block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()

    Log.debug(text + " takes: " + (t1 - t0)/ Math.pow(10, 6) + "ms")
    result
  }

}
