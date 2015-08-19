package ch.twenty.medlineGraph.explore

import java.io.File

import com.typesafe.config.ConfigFactory
import play.api.Logger

/**
 * @author Alexandre Masselot.
 */
trait WithPrivateConfig {
  val config = ConfigFactory.parseFile(new File("conf/private.conf"))
  val logger = Logger
}
