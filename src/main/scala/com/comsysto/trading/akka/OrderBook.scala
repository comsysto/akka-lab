package com.comsysto.trading.akka

import akka.actor.{Actor, ActorLogging}
import com.comsysto.trading.domain._
import com.comsysto.trading.algorithm.TradeMatcher

object OrderBook {

  case object Trade

}

class OrderBook(val paper: Paper, var currentPrice: BigDecimal = 0) extends Actor with ActorLogging {

  this: TradeMatcher =>

  import com.comsysto.trading.akka.OrderBook._

  var asks: List[Ask] = Nil
  var bids: List[Ask] = Nil

  override def receive = {
    case Ask(_, v, p) =>
    case Bid(_, v, p) =>
    case Trade =>
  }

}
