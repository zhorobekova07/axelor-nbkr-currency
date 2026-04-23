package com.axelor.apps.currency.service;

import com.axelor.apps.currency.db.CurrencyRate;
import com.axelor.apps.currency.db.repo.CurrencyRateBaseRepository;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Реализация сервиса загрузки курсов валют НБКР.
 *
 * <p>Алгоритм работы:
 *
 * <ol>
 *   <li>Отправляет HTTP запрос к API НБКР
 *   <li>Парсит полученный XML ответ
 *   <li>Сохраняет курсы в БД (upsert по коду и дате)
 * </ol>
 */
@Singleton
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyRateServiceImpl.class);

    private static final String NBKR_URL = "https://www.nbkr.kg/XML/daily.xml";
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 15_000;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CurrencyRateBaseRepository currencyRateRepo;

    @Inject
    public CurrencyRateServiceImpl(CurrencyRateBaseRepository currencyRateRepo) {
        this.currencyRateRepo = currencyRateRepo;
    }

    /**
     * Загружает курсы на сегодня. Делегирует в fetchAndSaveRates(LocalDate).
     */
    @Override
    public int fetchAndSaveRates() throws IOException {
        return fetchAndSaveRates(null);
    }

    /**
     * Загружает курсы на указанную дату. Если дата null — берёт сегодняшнюю.
     *
     * @param targetDate дата курсов или null для сегодня
     * @return количество сохранённых валют
     * @throws IOException если НБКР недоступен
     */
    @Override
    public int fetchAndSaveRates(LocalDate targetDate) throws IOException {
        String urlStr =
                targetDate == null ? NBKR_URL : NBKR_URL + "?date=" + targetDate.format(DATE_FORMAT);

        LOG.info("Загрузка курсов валют НБКР: {}", urlStr);

        Document doc = fetchXml(urlStr);
        List<CurrencyRate> parsed = parseDocument(doc);

        if (parsed.isEmpty()) {
            LOG.warn("Ответ от НБКР не содержит курсов валют");
            return 0;
        }

        int saved = persistRates(parsed);
        LOG.info("Курсы валют обновлены: {} записей", saved);
        return saved;
    }

    /**
     * Выполняет HTTP запрос к НБКР и возвращает XML документ.
     *
     * @param urlStr URL для запроса
     * @return распарсенный XML документ
     * @throws IOException если сервер недоступен
     */
    private Document fetchXml(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Accept", "application/xml");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Сервер НБКР вернул код: " + code);
            }

            try (InputStream is = conn.getInputStream()) {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Ошибка парсинга XML: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Парсит XML документ НБКР и возвращает список курсов валют.
     *
     * @param doc XML документ от НБКР
     * @return список курсов валют
     */
    private List<CurrencyRate> parseDocument(Document doc) {
        Element root = doc.getDocumentElement();
        root.normalize();

        LocalDate rateDate = parseDateAttribute(root);
        NodeList nodes = root.getElementsByTagName("Currency");
        List<CurrencyRate> result = new ArrayList<>(nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            try {
                result.add(parseCurrency(el, rateDate));
            } catch (Exception e) {
                LOG.warn("Пропуск валюты: {}", e.getMessage());
            }
        }
        return result;
    }

    private LocalDate parseDateAttribute(Element root) {
        String dateAttr = root.getAttribute("Date");
        if (dateAttr == null || dateAttr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateAttr.trim(), DATE_FORMAT);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private CurrencyRate parseCurrency(Element el, LocalDate rateDate) {
        String code = el.getAttribute("ISOCode");
        String nominal = getTag(el, "Nominal");
        String value = getTag(el, "Value");

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Нет ISOCode");
        }

        CurrencyRate rate = new CurrencyRate();
        rate.setCode(code.trim().toUpperCase());
        rate.setName(code.trim().toUpperCase());
        rate.setNominal(nominal != null ? Integer.parseInt(nominal.trim()) : 1);
        rate.setRate(new BigDecimal(value.trim().replace(",", ".")));
        rate.setRateDate(rateDate);
        return rate;
    }

    private String getTag(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    /**
     * Сохраняет курс без дублирования (upsert). Найдена запись за эту дату — обновляет. Не найдена —
     * создаёт новую.
     */
    @Transactional
    protected int persistRates(List<CurrencyRate> rates) {
        int count = 0;
        for (CurrencyRate incoming : rates) {
            try {
                upsert(incoming);
                count++;
            } catch (Exception e) {
                LOG.error("Ошибка сохранения {}: {}", incoming.getCode(), e.getMessage());
            }
        }
        return count;
    }

    private void upsert(CurrencyRate incoming) {
        CurrencyRate existing =
                currencyRateRepo.findByCodeAndDate(incoming.getCode(), incoming.getRateDate());
        if (existing != null) {
            existing.setName(incoming.getName());
            existing.setNominal(incoming.getNominal());
            existing.setRate(incoming.getRate());
            currencyRateRepo.save(existing);
        } else {
            currencyRateRepo.save(incoming);
        }
    }
}
