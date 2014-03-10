package akka.trading.system

import akka.trading.domain.Order

case class Rsp(order: Order, status: Boolean)
