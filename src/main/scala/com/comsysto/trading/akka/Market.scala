package com.comsysto.trading.akka

import akka.actor.{ActorLogging, PoisonPill, ActorRef, Actor}
import scala.concurrent.duration._
import com.comsysto.trading.akka.Market.{TradeSecurity, Close, Open}
import com.comsysto.trading.akka.OrderRouter.{ListSecuritiesResponse, ListSecurities}
import com.comsysto.trading.domain._
import scala.util.Random
import com.comsysto.trading.domain.Security
import com.comsysto.trading.domain.Bid
import com.comsysto.trading.domain.Depot
import com.comsysto.trading.akka.OrderRouter.ListSecuritiesResponse
import com.comsysto.trading.domain.Deposit
import scala.concurrent.ExecutionContext.Implicits.global

object Market {
  case object Open
  case object TradeSecurity
  case object Close
}


/**
 * Created by sturmm on 11.03.14.
 */
//TODO: Consider renaming this to MarketParticipant
class Market(val orderRouter : ActorRef, var depot : Depot, var deposit : Deposit) extends Actor with ActorLogging {
  private var tradedSecurities : List[Security] = Nil

  //TODO: Consider storing trades in flight (wait on in-flight trades before closing the market?)

  orderRouter ! ListSecurities

  def receive = {
    case ListSecuritiesResponse(s) => tradedSecurities = s
    case Open => {
      context.system.scheduler.schedule(100.milliseconds, 100.milliseconds, self, TradeSecurity)
      context.become(open)
    }
  }

  def open : Receive = {
    case TradeSecurity => trade()
    case Close => context.become(closing)
  }

  //TODO: We need to get a feedback from the OrderBook whether the order has been fulfilled. Then we need to update our balances...
  //Choose some security randomly and buy / sell it
  private def trade() {
    if (Random.nextBoolean()) {
      //try buying
      if (deposit.available) {
        //TODO: This has to be way more sophisticated
        orderRouter ! Bid(depot.security, 5, 110)
      } else if (depot.available) { //no money - we need to sell some security
        orderRouter ! Ask(depot.security, depot.volume, 105)
      } else {
        log.info("No money, no security. I'm jumping... :(")
        self ! PoisonPill
      }
    } else if (depot.available) { // try selling
      orderRouter ! Ask(depot.security, 1, 103)
    }
  }


  def closing : Receive = {
    case Open => context.become(open)
    //TODO: Is this too restrictive - what about poison pills etc.?
    case _ => //ignore - we're closing
  }
}
