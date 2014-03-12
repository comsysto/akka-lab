package com.comsysto.trading.domain

trait Account {
  def canDraw : Boolean
  def accountNumber : String
}

case class Deposit(accountNumber : String, balance : BigDecimal) extends Account {
  def canDraw = balance > 0
}

//very, very simple and limited - each account has only one security
case class Depot(accountNumber : String, security : Security, volume : Long) extends Account {
  def canDraw = volume > 0
}
