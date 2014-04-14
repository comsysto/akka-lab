package com.comsysto.trading.provider

import com.typesafe.config.ConfigFactory

trait ConfigProvider {
  val config = ApplicationConfiguration.appConfig
}

object ApplicationConfiguration {

  val appConfig = ConfigFactory.load("simulation.conf").getConfig("com.comsysto.trading")

}
