package ch.twenty.medlineGraph.explore

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer

/**
 * Handles configuration, context and so
 *
 * @author Alexandre Masselot.
 */
object SparkCommons {
  //build the SparkConf  object at once
  lazy val conf = {
    new SparkConf(false)
      .setMaster("local[*]")
      .setAppName("play demo")
      .set("spark.logConf", "true")
  }

  lazy val sc = SparkContext.getOrCreate(conf)
  lazy val sqlContext = new SQLContext(sc)

  def registerUdfFilter(name: String, f: (ArrayBuffer[(Double, Double)]) => Boolean) = {
    sqlContext.udf.register(name, f)
  }

  registerUdfFilter("isSizeGE2", { (ab: ArrayBuffer[(Double, Double)]) => ab.size >= 2 })

//  def udfize(f: (String) => Boolean) = {
//    import sqlContext.implicits._
//    val funcAffContains: (ArrayBuffer[String] => Boolean) = (xs: ArrayBuffer[String]) => xs.toList.exists(s => s != null && s.toLowerCase.contains("genentech"))
//    val udfFuncAffContains = udf(funcAffContains)
//  }
}
