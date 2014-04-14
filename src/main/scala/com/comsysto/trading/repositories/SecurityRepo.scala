package com.comsysto.trading.repositories

import com.comsysto.trading.domain.Security
import com.comsysto.trading.provider.ConfigProvider

import scala.collection.JavaConversions._

object SecurityRepo extends ConfigProvider {

  lazy val securities: List[Security] = (for {
    name <- config.getStringList("securities")
  } yield Security(name)).toList

}
