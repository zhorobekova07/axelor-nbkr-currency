package com.axelor.apps.currency.service;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Сервис для загрузки и сохранения курсов валют Национального банка Кыргызской Республики (НБКР).
 *
 * <p>Источник данных: https://www.nbkr.kg/XML/daily.xml
 */
public interface CurrencyRateService {

    /**
     * Загружает курсы валют на сегодняшнюю дату. Если записи за сегодня уже существуют — обновляет
     * их.
     *
     * @return количество обработанных валют
     * @throws IOException при ошибке сети или парсинга XML
     */
    int fetchAndSaveRates() throws IOException;

    /**
     * Загружает курсы валют на указанную дату.
     *
     * @param date дата на которую нужны курсы
     * @return количество обработанных валют
     * @throws IOException при ошибке сети или парсинга XML
     */
    int fetchAndSaveRates(LocalDate date) throws IOException;
}
