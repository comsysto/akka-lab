package com.comsysto.trading.akka

import akka.actor._
import scala.concurrent.duration._
import com.comsysto.trading.akka.Market.{TradeSecurity, Close, Open}
import com.comsysto.trading.akka.OrderRouter.ListSecurities
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import com.comsysto.trading.domain.Security
import com.comsysto.trading.domain.Bid
import com.comsysto.trading.domain.Depot
import com.comsysto.trading.domain.Ask
import com.comsysto.trading.akka.OrderRouter.ListSecuritiesResponse
import com.comsysto.trading.domain.Deposit

object Market {
  case object Open
  case object TradeSecurity
  case object Close
}


/**
 * Created by sturmm on 11.03.14.
 */
//TODO: Consider renaming this to MarketParticipant
class Market(val orderRouter : ActorRef, var depot : Depot, var deposit : Deposit) extends Actor with ActorLogging with Stash {

  //TODO: Consider storing trades in flight (wait on in-flight trades before closing the market?)


  override def preStart() = orderRouter ! ListSecurities

  def receive = awaitingListSecurities


  def awaitingListSecurities: Receive = {
    case ListSecuritiesResponse(s) => {
      unstashAll()
      context.become(awaitingOpen(s))
    }
    case _ => stash()
  }

  def awaitingOpen(securities : List[Security]) : Receive = {
    case Open => {
      context.system.scheduler.schedule(100.milliseconds, 400.milliseconds, self, TradeSecurity)
      context.become(open)
    }
  }

  def open : Receive = {
    case TradeSecurity => trade()
    case Close => context.become(closing)
    case m => log.info(s"Received $m")
  }

  //TODO: We need to get a feedback from the OrderBook whether the order has been fulfilled. Then we need to update our balances...
  //Choose some security randomly and buy / sell it
  private def trade() {
    if (Random.nextBoolean()) {
      //try buying
      if (deposit.canDraw) {
        //TODO: This has to be way more sophisticated
        orderRouter ! Bid(depot, depot.security, 5, 110)
      } else if (depot.canDraw) { //no money - we need to sell some security
        orderRouter ! Ask(depot, depot.security, depot.volume, 105)
      } else {
        log.info("No money, no security. I'm jumping... :(")
        self ! PoisonPill
      }
    } else if (depot.canDraw) { // try selling
      orderRouter ! Ask(depot, depot.security, 1, 103)
    }
  }


  def closing : Receive = {
    case Open => context.become(open)
    //TODO: Is this too restrictive - what about poison pills etc.?
    case _ => //ignore - we're closing
  }
}
