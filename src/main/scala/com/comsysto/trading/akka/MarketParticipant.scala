package com.comsysto.trading.akka

import akka.actor._
import scala.concurrent.duration._
import com.comsysto.trading.akka.MarketParticipant.{TradeSecurity, Close, Open}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import com.comsysto.trading.domain.Security
import com.comsysto.trading.domain.Bid
import com.comsysto.trading.domain.Depot
import com.comsysto.trading.domain.Ask
import com.comsysto.trading.domain.Deposit
import com.comsysto.trading.akka.OrderBook.{AskResponse, BidResponse}

object MarketParticipant {
  case object Open
  case object TradeSecurity
  case object Close
}


/**
 * Created by sturmm on 11.03.14.
 */
class MarketParticipant(id: Int, val orderBook : ActorSelection, var depot : Depot, var deposit : Deposit) extends Actor with ActorLogging with Stash {

  //TODO: Consider storing trades in flight (wait on in-flight trades before closing the market?)
  var currentPrice : BigDecimal = 105


//  override def preStart() = orderRouter ! ListSecurities

  def receive = awaitingOpen(Nil)

  def awaitingOpen(securities : List[Security]) : Receive = {
    case Open => {
      context.system.scheduler.schedule(100.milliseconds, 200.milliseconds, self, TradeSecurity)
      context.become(open)
    }
  }

  def open : Receive = {
    case TradeSecurity => trade()
    case BidResponse(bid, volume, price) => {
      //we got security...
      depot = depot.receive(volume)
      //TODO: Consider correcting the amount in the deposit

      currentPrice = price
    }

    case AskResponse(bid, volume, price) => {
      //we got money
      deposit = deposit.deposit(volume * price)
      currentPrice = price
    }

    case Close => context.become(closing)


    case m => log.info(s"Received $m")
  }

  //Choose some security randomly and buy / sell it
  private def trade() {
    if (Random.nextBoolean()) {
      if (deposit.canDraw) {
        buySecurity()
      } else if (depot.canDraw) { //no money - we need to sell some security
        sellSecurity()
      } else {
        // neither money nor security - waiting for open trades
        log.info(s"[$id]No money and securities left.")
      }
    } else if (depot.canDraw) {
      sellSecurity()
    }
  }

  private def buySecurity() {
    val biddingPrice = currentPrice + Random.nextInt(5) - 3
    val maxBiddingVolume = ((deposit.balance / biddingPrice).toInt - 1) max 1
    val biddingVolume = 1 + Random.nextInt(maxBiddingVolume)

    //no order cancelling - we draw the full order volume now
    deposit = deposit.draw(biddingPrice * biddingVolume)
    orderBook ! Bid(depot, depot.security, biddingVolume, biddingPrice)
  }

  private def sellSecurity() {
    val askingPrice = currentPrice + Random.nextInt(4) - 3
    val askingVolume = 1 + Random.nextLong() % depot.volume

    //no order cancelling - we post the full order volume now
    depot = depot.post(askingVolume)
    orderBook ! Ask(depot, depot.security, askingVolume, askingPrice)
  }



  def closing : Receive = {
    case Open => context.become(open)
    //TODO: Is this too restrictive - what about poison pills etc.?
    case _ => //ignore - we're closing
  }
}
