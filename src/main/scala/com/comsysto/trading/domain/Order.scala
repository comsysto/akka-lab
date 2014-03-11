package com.comsysto.trading.domain

import java.util.Date

sealed trait Order {
  val paper: Paper
  val volume: Long
  val price: BigDecimal
  val requested = new Date()
}

case class Bid(paper: Paper, volume: Long, price: BigDecimal) extends Order
case class Ask(paper: Paper, volume: Long, price: BigDecimal) extends Order




