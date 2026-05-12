package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic search engine for the application.
 * This class provides filtering logic for any collection of objects that implement
 * the ISearchable interface, ensuring a consistent search experience across different screens.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class SearchManager {
    /** The source collection containing all searchable items */
    private List<ISearchable> searchableCollection;

    /**
     * Initializes the SearchManager with a specific data source.
     * @param collection A list of items implementing ISearchable (e.g., Lectures, Tasks).
     */
    public SearchManager(List<ISearchable> collection) {
        this.searchableCollection = collection;
    }

    /**
     * Filters the source collection based on a string query.
     * The search is case-insensitive and trims whitespace.
     * @param query The search term entered by the user.
     * @return A list containing only the items that match the search criteria.
     */
    public List<ISearchable> search(String query) {
        List<ISearchable> results = new ArrayList<>();

        // אם אין שאילתה, מחזירים את כל הרשימה המקורית
        if (query == null || query.isEmpty()) {
            return searchableCollection;
        }

        // המרה לאותיות קטנות וניקוי רווחים כדי שהחיפוש לא יהיה רגיש ל-Case Sensitive
        String lowerQuery = query.toLowerCase().trim();

        // מעבר על כל פריט באוסף (למשל: כל הרצאה בנפרד)
        for (ISearchable item : searchableCollection) {
            List<String> fields = item.getSearchableFields();

            // בדיקה אם השאילתה מופיעה באחד מהשדות של אותו פריט
            for (String field : fields) {
                if (field != null && field.toLowerCase().contains(lowerQuery)) {
                    results.add(item); // נמצאה התאמה - מוסיפים לתוצאות
                    break; // מפסיקים לבדוק את שאר השדות של אותו פריט ועוברים לפריט הבא
                }
            }
        }
        return results;
    }
}