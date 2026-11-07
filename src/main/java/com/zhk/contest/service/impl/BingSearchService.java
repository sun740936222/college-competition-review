package com.zhk.contest.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Scrapes Bing search results HTML to extract text items.
 * No API key required — uses Jsoup to fetch and parse the public Bing search page.
 */
@Service
public class BingSearchService {

    private static final String BING_URL = "https://www.bing.com/search?q=%s&setlang=zh-cn&count=20";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Search Bing and extract text from result titles, snippets, and links.
     * @return deduplicated list of extracted text strings
     */
    public List<String> search(String query) {
        Set<String> items = new LinkedHashSet<>();
        try {
            String url = String.format(BING_URL, URLEncoder.encode(query, StandardCharsets.UTF_8));
            Document doc = fetchDocument(url);
            extractTextItems(doc, items);
        } catch (Exception e) {
            // Return whatever we have; empty list signals failure to caller
        }
        return new ArrayList<>(items);
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .timeout(15000)
                .get();
    }

    private void extractTextItems(Document doc, Set<String> items) {
        // Bing search result items: <li class="b_algo">
        Elements results = doc.select("li.b_algo");
        for (Element result : results) {
            // Extract title text from <h2><a>
            Elements titles = result.select("h2 a");
            for (Element title : titles) {
                String text = title.text().trim();
                if (!text.isEmpty()) {
                    items.add(text);
                }
            }

            // Extract snippet from <div class="b_caption"> <p>
            Elements captions = result.select(".b_caption p, .b_caption .b_lineclamp2, .b_caption .b_lineclamp3");
            for (Element p : captions) {
                String text = p.text().trim();
                if (!text.isEmpty()) {
                    items.add(text);
                }
            }

            // Also get all link texts within the result
            Elements links = result.select("a");
            for (Element link : links) {
                String text = link.text().trim();
                if (!text.isEmpty() && text.length() < 50) {
                    items.add(text);
                }
            }
        }
    }
}
