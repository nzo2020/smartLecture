package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class SearchManager {
    // אוסף של פריטים הניתנים לחיפוש
    private List<ISearchable> searchableCollection;

    // בנאי המקבל את רשימת הנתונים שבה נרצה לבצע את החיפוש
    public SearchManager(List<ISearchable> collection) {
        this.searchableCollection = collection;
    }

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