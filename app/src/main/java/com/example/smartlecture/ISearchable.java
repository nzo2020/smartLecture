package com.example.smartlecture;

import java.util.List;

interface ISearchable {
    // מחזיר רשימת מחרוזות שבהן נרצה לחפש (כותרת, סיכום וכו')
    List<String> getSearchableFields();
}