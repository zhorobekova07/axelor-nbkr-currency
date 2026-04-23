package com.axelor.apps.currency.web;

import com.axelor.apps.currency.service.CurrencyRateService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web-контроллер для управления курсами валют из интерфейса Axelor. Обрабатывает нажатие кнопки
 * «Обновить курсы сегодня» в UI.
 */
@Singleton
public class CurrencyRateController {

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyRateController.class);

    private final CurrencyRateService currencyRateService;

    @Inject
    public CurrencyRateController(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    public void fetchTodayRates(ActionRequest request, ActionResponse response) {
        try {
            int count = currencyRateService.fetchAndSaveRates();
            response.setAlert(
                    String.format(
                            "Курсы валют успешно обновлены: %d записей за %s",
                            count, LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
            response.setReload(true);
        } catch (Exception e) {
            LOG.error("Ошибка обновления курсов: {}", e.getMessage(), e);
            response.setError("Ошибка загрузки курсов: " + e.getMessage());
        }
    }
}
