package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class SearchManager {
    private List<ISearchable> searchableCollection;

    public List<ISearchable> search(String query) {
        List<ISearchable> results = new ArrayList<>();
        for (ISearchable item : searchableCollection) {
            for (String field : item.getSearchableFields()) {
                if (field != null && field.contains(query)) {
                    results.add(item);
                    break;
                }
            }
        }
        return results;
    }
}
