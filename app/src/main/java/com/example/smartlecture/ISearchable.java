package com.example.smartlecture;

import java.util.List;

/**
 * Interface for objects that can be indexed and searched within the application.
 * Classes implementing this interface define which of their text fields should be
 * exposed to the search engine logic.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
interface ISearchable {
    /**
     * Retrieves a list of string values representing the searchable content of the object.
     * @return A list of strings (e.g., title, summary, location) to be used for matching search queries.
     */
    // מחזיר רשימת מחרוזות שבהן נרצה לחפש (כותרת, סיכום וכו')
    List<String> getSearchableFields();
}