package com.axelor.apps.currency.service;

import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job для ежедневной автоматической загрузки курсов валют НБКР. Запускается по расписанию
 * CRON каждый день в 09:00.
 */
public class CurrencyRateJob implements Job {

    @Inject
    protected CurrencyRateService currencyRateService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            currencyRateService.fetchAndSaveRates();
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }
}
