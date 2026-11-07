package com.zhk.contest.service.impl;

import com.zhk.contest.dto.response.CollegeLookupResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Look up colleges/departments under a university by searching Bing.
 */
@Service
public class CollegeLookupService {

    private static final int MAX_RESULTS = 60;

    private final BingSearchService bingSearchService;

    public CollegeLookupService(BingSearchService bingSearchService) {
        this.bingSearchService = bingSearchService;
    }

    public CollegeLookupResponse lookupColleges(String universityName) {
        try {
            Set<String> colleges = new LinkedHashSet<>();

            // Primary search: "{universityName} 院系设置"
            List<String> items1 = bingSearchService.search(universityName + " 院系设置");
            extractColleges(items1, colleges);

            // Supplement: "{universityName} 学院" if we don't have enough results
            if (colleges.size() < 5) {
                List<String> items2 = bingSearchService.search(universityName + " 学院");
                extractColleges(items2, colleges);
            }

            if (colleges.size() > MAX_RESULTS) {
                List<String> limited = new ArrayList<>(colleges).subList(0, MAX_RESULTS);
                return CollegeLookupResponse.success(universityName, limited, "bing");
            }

            if (!colleges.isEmpty()) {
                return CollegeLookupResponse.success(universityName, new ArrayList<>(colleges), "bing");
            }

            return CollegeLookupResponse.error(universityName, "未能从搜索结果中提取到学院列表，请手动输入");
        } catch (Exception e) {
            return CollegeLookupResponse.error(universityName, "查询失败: " + e.getMessage());
        }
    }

    /**
     * Scan extracted text items from Bing results and pick out college names.
     */
    private void extractColleges(List<String> textItems, Set<String> result) {
        for (String text : textItems) {
            // Split on common separators in case a snippet lists multiple colleges
            String[] parts = text.split("[,，、；;\\s]{2,}");
            for (String part : parts) {
                String cleaned = part.trim();
                if (isCollegeName(cleaned)) {
                    // Remove trailing parenthetical notes like "（二级学院）"
                    cleaned = cleaned.replaceAll("[（(][^)）]{0,15}[)）]$", "").trim();
                    result.add(cleaned);
                }
            }
            // Also try splitting the whole text with single-char separators
            if (text.contains("学院") && text.length() < 200) {
                String[] singleSplit = text.split("[,，、]");
                for (String part : singleSplit) {
                    String cleaned = part.trim();
                    if (isCollegeName(cleaned)) {
                        cleaned = cleaned.replaceAll("[（(][^)）]{0,15}[)）]$", "").trim();
                        result.add(cleaned);
                    }
                }
            }
        }
    }

    private boolean isCollegeName(String text) {
        if (text == null || text.isBlank()) return false;
        if (text.length() < 3 || text.length() > 30) return false;
        if (!text.contains("学院") && !text.contains("系") && !text.contains("部")) return false;
        String[] excludePatterns = {"大学", "百科", "参考资料", "扩展阅读", "参考文献", "http", "www", "简介", "历史", "办学"};
        for (String ex : excludePatterns) {
            if (text.contains(ex)) return false;
        }
        return true;
    }
}
