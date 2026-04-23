-- Регистрация CRON задания для загрузки курсов валют НБКР
-- Запускается каждый день в 09:00
INSERT INTO meta_schedule (version, created_on, updated_on, name, job, cron, active, archived, description)
SELECT 0, NOW(), NOW(),
       'NBKR Currency Rate Fetch',
       'com.axelor.apps.currency.service.CurrencyRateJob',
       '0 0 9 * * ?',
       true,
       false,
       'Daily automatic fetch of currency rates from National Bank of Kyrgyz Republic'
    WHERE NOT EXISTS (
    SELECT 1 FROM meta_schedule
    WHERE name = 'NBKR Currency Rate Fetch'
);