package ch.twenty.medlineGraph.explore

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{Row, DataFrame}

import scala.collection.mutable.ArrayBuffer

/**
 * @author Alexandre Masselot.
 */

object CitationsWithLocations {

  import SparkCommons.sqlContext.implicits._


  val conf = ConfigFactory.load();
  val path = conf.getString("spark.path.citations_locations")

  lazy val rdd = SparkCommons.sqlContext.read.parquet(path)

  def count: Long = rdd.count()

  lazy val udfBinCoords = {
    val binSize = 0.25
    val udfName = "binCoords"

    val f: (ArrayBuffer[Any]) => List[(Double, Double)] = { (coords: ArrayBuffer[Any]) =>
      coords.toList.map({ case Row(x: Double, y: Double) => (Math.round(x / binSize) * binSize, Math.round(y / binSize) * binSize) })
    }
    SparkCommons.sqlContext.udf.register(udfName, f)
  }

  lazy val udfIsSizeGE2 = {
    val udfName = "isSizeGE2"
    SparkCommons.sqlContext.udf.register(udfName, { (ab: ArrayBuffer[(Double, Double)]) => ab.size >= 2 })
  }

  def udfFilterAffiliation(q: String) = {

    val quc = q.toLowerCase()
    val funcAffContains: (ArrayBuffer[String] => Boolean) = (xs: ArrayBuffer[String]) => xs.toList.exists(s => s != null && s.toLowerCase.contains(quc))
    val udfFuncAffContains = SparkCommons.sqlContext.udf.register("haha", funcAffContains)
  }

  def filterByMultipleLocation(df: DataFrame = rdd): DataFrame = {
    //rdd.filter("nbcoordinates > 1").filter("")
    df.filter(udfIsSizeGE2(df("coordinates")))
  }

  def filterByAffiliation(q: String, df: DataFrame = rdd): DataFrame = {
    df.filter(df("authors.affiliation.orig").contains(q))
  }

  def aggregateGeoLink(q: String): DataFrame = {
    val df = filterByAffiliation(q, filterByMultipleLocation(rdd))
    val newCol = udfBinCoords(df("coordinates"))
    val dfRounded = df.withColumn("roundedCoordinates", newCol).select("roundedCoordinates", "pubmedId")
    dfRounded.flatMap({ case Row(coords: List[Any], pmid: String) =>
      if (coords.size > 7) Nil
      else coords.toList.combinations(2).toList.map({ case c1 :: c2 :: Nil => ((c1, c2), pmid) })

    }).groupByKey().map({ case (x, y) =>
      val l = x.asInstanceOf[Tuple2[(Double, Double), (Double, Double)]]
      (l, y.toList)

    }).toDF("coordsLinks", "pubmedIds")
  }

  def countByCoords(q: String): DataFrame = {
    val df = filterByAffiliation(q, filterByMultipleLocation(rdd))
    val newCol = udfBinCoords(df("coordinates"))
    val dfRounded = df.withColumn("roundedCoordinates", newCol).select("roundedCoordinates")
    dfRounded.select("roundedCoordinates")
      .flatMap({ case Row(l: List[Any]) => l.map(x => (x.asInstanceOf[(Double, Double)], 1)) })
      .reduceByKey((a, b) => a + b)
      .toDF("coords", "count")
  }
}