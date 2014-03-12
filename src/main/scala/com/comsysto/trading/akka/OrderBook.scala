package com.comsysto.trading.akka

import akka.actor.{Actor, ActorLogging}
import com.comsysto.trading.domain._
import com.comsysto.trading.algorithm.{MarketPriceCalculator, TradeMatcher}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object OrderBook {
  case object Trade
  case object ListPrice
  case class ListPriceResponse(currentPrice : BigDecimal)
}

class OrderBook(val security: Security, var currentPrice: BigDecimal = 0) extends Actor with ActorLogging {

  this: TradeMatcher with MarketPriceCalculator =>

  import com.comsysto.trading.akka.OrderBook._

  //TODO: We have to store senders. As soon as an order is fulfilled we need to notify both parties so they update their balances accordingly
  var asks: List[Ask] = Nil
  var bids: List[Bid] = Nil


  override def preStart() = {
    context.system.scheduler.schedule(1.seconds, 2.seconds, self, Trade)
  }

  override def receive = {
    case ask@Ask(s, v, p) if s == security => asks = ask :: asks
    case bid@Bid(s, v, p) if s == security => bids = bid :: bids
    case Trade => {
      log.debug(s"Triggering market price recalculation for $security")
      recalculate()
    }

    case ListPrice => sender ! ListPriceResponse(currentPrice)
  }

  private def recalculate() {
    val (newAsks, newBids, successfulTrades) = doTrades(asks, bids)

    asks = newAsks
    bids = newBids
    currentPrice = calculatePrice(successfulTrades, currentPrice)
    log.info(s"Current price for $security is $currentPrice")
  }
}
