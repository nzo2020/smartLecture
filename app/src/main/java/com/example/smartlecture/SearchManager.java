package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class SearchManager {
    private List<ISearchable> searchableCollection;

    public SearchManager(List<? extends ISearchable> collection) {
        this.searchableCollection = (List<ISearchable>) collection;
    }

    // חיפוש חופשי (לפי טקסט)
    public List<ISearchable> search(String query) {
        List<ISearchable> results = new ArrayList<>();
        if (query == null || query.isEmpty()) return searchableCollection;

        String lowerQuery = query.toLowerCase().trim();

        for (ISearchable item : searchableCollection) {
            for (String field : item.getSearchableFields()) {
                // הבונוס האמיתי: החיפוש בודק אם המילה קיימת בתוך השדה (למשל בתוך ה-SummaryText)
                if (field != null && field.toLowerCase().contains(lowerQuery)) {
                    results.add(item);
                    break;
                }
            }
        }
        return results;
    }
}