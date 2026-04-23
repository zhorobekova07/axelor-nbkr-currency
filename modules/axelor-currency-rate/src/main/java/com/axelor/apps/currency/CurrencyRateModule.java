package com.axelor.apps.currency;

import com.axelor.app.AxelorModule;
import com.axelor.apps.currency.db.repo.CurrencyRateBaseRepository;
import com.axelor.apps.currency.db.repo.CurrencyRateRepository;
import com.axelor.apps.currency.service.CurrencyRateJob;
import com.axelor.apps.currency.service.CurrencyRateService;
import com.axelor.apps.currency.service.CurrencyRateServiceImpl;

/**
 * Guice-модуль для регистрации зависимостей модуля Currency Rate. Связывает интерфейсы с их
 * реализациями.
 */
public class CurrencyRateModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(CurrencyRateService.class).to(CurrencyRateServiceImpl.class);
    bind(CurrencyRateRepository.class).to(CurrencyRateBaseRepository.class);
    bind(CurrencyRateJob.class);
  }
}
