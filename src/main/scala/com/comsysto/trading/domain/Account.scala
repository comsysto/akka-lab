package com.comsysto.trading.domain

trait Account {
  def available : Boolean
}

case class Deposit(balance : BigDecimal) extends Account {
  def available = balance > 0
}

//very, very simple and limited - each account has only one security
case class Depot(security : Security, volume : Long) extends Account {
  def available = volume > 0
}
