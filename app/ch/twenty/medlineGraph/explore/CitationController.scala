package ch.twenty.medlineGraph.explore

import org.apache.spark.sql.DataFrame
import play.api.cache.Cached
import play.api.mvc._
import javax.inject.Inject

import play.cache.CacheApi


/**
 * @author Alexandre Masselot.
 */
class CitationController @Inject()(cached: Cached) extends Controller {


  /**
   * dataframe can output, with toJSON, a list of json string. They just need to be wrapped with [] and commas
   * @param rdd
   * @return
   */
  def toJsonString(rdd: DataFrame, selectColumns: List[String] = Nil): String = {
    val rddOut = selectColumns match {
      case Nil => rdd
      case fields => rdd.select(fields.head, fields.tail: _*)
    }

    "[" + rddOut.toJSON.collect.toList.mkString(",\n") + "]"

  }

  def count = Action {
    Ok(CitationsWithLocations.nbTot.toString)
  }

  def countMulti = Action {
    Ok(CitationsWithLocations.filterByMultipleLocation().count.toString)
  }

  def filterByAffiliation(q: String) = cached(req => "rest-" + req.uri) {
    Action {
      Ok(CitationsWithLocations.filterByMultipleLocation().filter(CitationsWithLocations.rdd("authors.affiliation.orig").contains(q)).count.toString)
    }
  }

  def aggregateGeoLinks(q: String) = cached(req => "rest-" + req.uri) {
    Action {
      //    Ok(CitationsWithLocations.filterByMultipleLocation.filter(CitationsWithLocations.rdd("authors.affiliation.orig").contains(q)).count.toString)
      Ok(toJsonString(CitationsWithLocations.aggregateGeoLink(q)))
    }
  }

  def countByCoords(affiliation: Option[String]) = cached(req => "rest-" + req.uri) {
    Action {
      Ok(toJsonString(CitationsWithLocations.countByCoords(affiliation)))
    }
  }
}