package com.comsysto.trading.repositories

import com.comsysto.trading.domain.Security
import com.comsysto.trading.provider.ConfigProvider

import scala.collection.JavaConversions._

class SecurityRepo(val configPath : String = "securities") extends ConfigProvider {

  lazy val securities: List[Security] = (for {
    name <- config.getStringList(configPath)
  } yield Security(name)).toList

}
