package com.axelor.apps.currency.db.repo;

import com.axelor.apps.currency.db.CurrencyRate;
import com.axelor.db.Query;

import java.time.LocalDate;

/**
 * Репозиторий для работы с курсами валют. Содержит методы поиска по коду валюты и дате.
 */
public class CurrencyRateBaseRepository extends CurrencyRateRepository {

    /**
     * Найти курс валюты по коду и дате.
     *
     * @param code код валюты (например USD)
     * @param date дата курса
     * @return найденный курс или null
     */
    public CurrencyRate findByCodeAndDate(String code, LocalDate date) {
        return Query.of(CurrencyRate.class)
                .filter("self.code = :code AND self.rateDate = :date")
                .bind("code", code)
                .bind("date", date)
                .fetchOne();
    }
}
