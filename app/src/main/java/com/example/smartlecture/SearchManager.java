package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class SearchManager {
    private List<ISearchable> searchableCollection;

    public SearchManager(List<ISearchable> collection) {
        this.searchableCollection = collection;
    }

    /**
     * Filters the collection based on a string query across all searchable fields.
     */
    public List<ISearchable> search(String query) {
        List<ISearchable> results = new ArrayList<>();

        if (query == null || query.isEmpty()) {
            return searchableCollection;
        }

        String lowerQuery = query.toLowerCase().trim();

        for (ISearchable item : searchableCollection) {
            List<String> fields = item.getSearchableFields();

            for (String field : fields) {
                if (field != null && field.toLowerCase().contains(lowerQuery)) {
                    results.add(item);
                    break; // Move to next item once a match is found
                }
            }
        }
        return results;
    }
}