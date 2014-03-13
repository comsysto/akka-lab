package com.comsysto.trading.provider

import com.comsysto.trading.domain.Security
import com.comsysto.trading.repositories.SecurityRepo

trait SecuritiesProvider {
  def securities: List[Security]
}

trait SimpleSecuritiesProvider extends SecuritiesProvider{
  override def securities = SecurityRepo.securities
}
