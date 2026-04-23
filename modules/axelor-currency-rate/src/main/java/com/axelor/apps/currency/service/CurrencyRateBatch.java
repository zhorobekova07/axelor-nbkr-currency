package com.axelor.apps.currency.service;

import com.axelor.apps.base.service.administration.AbstractBatch;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch-задание для ежедневной автоматической загрузки курсов валют НБКР. Запускается по расписанию
 * CRON каждый день в 09:00. Также может быть запущен вручную через Administration → Scheduler.
 */
public class CurrencyRateBatch extends AbstractBatch {

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyRateBatch.class);

    private final CurrencyRateService currencyRateService;

    @Inject
    public CurrencyRateBatch(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    @Override
    protected void start() throws IllegalArgumentException {
        LOG.info("=== Запуск загрузки курсов валют НБКР ===");
        try {
            super.start();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void process() {
        try {
            int count = currencyRateService.fetchAndSaveRates();
            addComment(String.format("Успешно обновлено курсов: %d", count));
            incrementDone();
        } catch (Exception e) {
            LOG.error("Ошибка загрузки курсов: {}", e.getMessage(), e);
            addComment("Ошибка: " + e.getMessage());
            incrementAnomaly();
        }
    }

    @Override
    protected void setBatchTypeSelect() {
    }

    @Override
    protected void stop() {
        LOG.info(
                "=== Batch завершён: обработано={}, ошибок={} ===", batch.getDone(), batch.getAnomaly());
        super.stop();
    }
}
