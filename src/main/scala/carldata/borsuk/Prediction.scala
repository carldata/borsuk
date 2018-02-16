package carldata.borsuk

import java.time.Instant

import carldata.series.{Csv, Gen}

object Prediction {
  def find(): String = {
    val idx = (for (i <- 0 to 288) yield Instant.parse("2018-02-15T00:00:00.00Z").plusSeconds(60 * 60 * i)).toVector
    val ts = Gen.randomNoise(idx, 0.5, 0.7)

    Csv.toCsv(ts)
  }
}
