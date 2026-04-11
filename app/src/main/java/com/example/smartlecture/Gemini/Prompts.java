package com.example.smartlecture.Gemini;

public class Prompts {
    public static final String PHOTO_PROMPT = "כתוב לי מהו הפרי או הירק שצולם ובנוסף תן לי מתכון אשר כולל אותו.\n" +
            "אם אתה לא מוצא פרי או ירק בתמונה תן לי הנחיה לצלם את התמונה מחדש,\n" +
            "כך שהפרי או הירק יופיע בבירור בתמונה.";

    // בתוך מחלקת Prompts.java
    public static final String LECTURE_SUMMARY_PROMPT = "סכם את הקלטת השיעור הבאה בעברית. " +
            "אנא ספק סיכום תמציתי של הנושאים העיקריים, מילות מפתח, ונקודות חשובות שנאמרו.";

    public static final String INGREDIENTS_SCHEMA = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
            "  \"title\": \"Dish Ingredients\",\n" +
            "  \"description\": \"Schema for ingredients in a dish, designed for a dietary diary.\",\n" +
            "  \"type\": \"array\",\n" +
            "  \"items\": {\n" +
            "    \"type\": \"object\",\n" +
            "    \"properties\": {\n" +
            "      \"name\": {\n" +
            "        \"type\": \"string\",\n" +
            "        \"description\": \"The name of the ingredient.\",\n" +
            "        \"example\": \"Chicken Breast\"\n" +
            "      },\n" +
            "      \"quantity\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The quantity of the ingredient used.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 150\n" +
            "      },\n" +
            "      \"unit\": {\n" +
            "        \"type\": \"string\",\n" +
            "        \"description\": \"The unit of measurement for the quantity.\",\n" +
            "        \"enum\": [\"g\", \"kg\", \"ml\", \"L\", \"piece\", \"cup\", \"tablespoon\", \"teaspoon\", \"oz\", \"lb\"],\n" +
            "        \"example\": \"g\"\n" +
            "      },\n" +
            "      \"calories\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The caloric content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 230\n" +
            "      },\n" +
            "      \"protein\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The protein content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 30\n" +
            "      },\n" +
            "      \"carbohydrates\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The carbohydrate content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 0\n" +
            "      },\n" +
            "      \"fat\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The fat content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 10\n" +
            "      },\n" +
            "      \"fiber\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The fiber content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 0\n" +
            "      },\n" +
            "      \"sugar\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The sugar content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 0\n" +
            "      },\n" +
            "      \"sodium\": {\n" +
            "        \"type\": \"number\",\n" +
            "        \"description\": \"The sodium content of the ingredient, per the specified quantity.\",\n" +
            "        \"minimum\": 0,\n" +
            "        \"example\": 70\n" +
            "      },\n" +
            "      \"notes\": {\n" +
            "        \"type\": \"string\",\n" +
            "        \"description\": \"Optional notes about the ingredient, such as preparation methods or specific brands.\",\n" +
            "        \"example\": \"Skinless, grilled\"\n" +
            "      },\n" +
            "      \"foodGroup\": {\n" +
            "        \"type\": \"string\",\n" +
            "        \"description\": \"The food group to which the ingredient belongs.\",\n" +
            "        \"enum\": [\"Protein\", \"Vegetable\", \"Fruit\", \"Grain\", \"Dairy\", \"Fat\", \"Spice\", \"Other\"],\n" +
            "        \"example\": \"Protein\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"required\": [\"name\", \"quantity\", \"unit\", \"calories\", \"protein\", \"carbohydrates\", \"fat\"]\n" +
            "  }\n" +
            "}";

    public static final String PHOTOS_PROMPT = "based on the photos write me the amount of carbs, proteins, fats and calories in the dish.\n" +
            "If you can't find any food in the photos, please ask me to take a better photo."+
            "return the data in the given schema:" + INGREDIENTS_SCHEMA;

    public static final String FILE_PROMPT = "נתח את מערך הבייטים המצורף וספק סיכום של תכולת הקובץ.\n" +
            "אנא נתח את הקובץ וספק בעברית סיכום תמציתי של תוכנו. אם תכולת הקובץ אינה ברורה או שאינך יכול לנתח אותה, החזר הודעה מתאימה.";

    public static final String FILES_COMPARISON_PROMPT = "אנא השווה בין הקבצים המצורפים וספק ניתוח מפורט:\n" +
            "1. זהה את סוג הקבצים ותאר בקצרה את תוכנם.\n" +
            "2. ציין את ההבדלים העיקריים בין הקבצים.\n" +
            "3. ציין את הנקודות המשותפות בין הקבצים.\n" +
            "4. אם אלו קבצי קוד, השווה את הפונקציונליות, האלגוריתמים והמבנה.\n" +
            "5. אם אלו קבצי טקסט, השווה את התוכן, המבנה והסגנון.\n" +
            "6. אם אלו קבצי מדיה או אחרים, תאר את ההבדלים הבולטים במאפיינים.\n\n" +
            "אם לא ניתן לקרוא את הקבצים או להשוות ביניהם, אנא ציין זאת והסבר מדוע.\n" +
            "הצג את התוצאות בפורמט מובנה עם כותרות וסעיפים להקלה על הקריאה.";

    public static final String SYSTEM_PROMPT = "אתה ג'מיני, מנחה משחק טריוויה אינטראקטיבי בשם 'שאל את ג'מיני'. תפקידך הוא להנחות את המשתמשים במשחק, להציג שאלות בנושא רכיבי תצוגה באנדרואיד (TextView, Button, ImageView וכו'), לנתח את תשובותיהם ולספק הסברים מפורטים.\n" +
            "\n" +
            "הנחיות:\n" +
            "\n" +
            "יצירת שאלות: צור שאלות מגוונות ומרתקות בנושא רכיבי תצוגה באנדרואיד. השתדל לכלול שאלות ברמות קושי שונות, החל משאלות בסיסיות ועד לשאלות מתקדמות.\n" +
            "ניתוח תשובות: נתח את תשובות המשתמשים וספק להם פידבק מיידי.\n" +
            "אם התשובה נכונה, שבח את המשתמש, ספק הסבר קצר ומעניין על הנושא ועדכן את המשתמש על כמה שאלות הוא ענה נכון עד כה מתוך סך כל השאלות שנשאלו.\n" +
            "אם התשובה שגויה, בדוק אם התשובה שונה באופן מהותי מהתשובה הנכונה. אם התשובה קרובה מספיק לתשובה הנכונה היה סבלני ותן למשתמש לנסות שוב. אם תשובת המשתמש שונה באופן מהותי מהתשובה הנכונה הסבר את הטעות וספק הסבר מפורט על התשובה הנכונה.\n" +
            "הסברים מפורטים: ספק הסברים מפורטים וברורים על רכיבי התצוגה באנדרואיד. אין צורך בדוגמאות קוד, אבל ניתן להפנות לקישורים ויזואליים באינטרנט, במידת האפשר, כדי להקל על ההבנה.\n" +
            "התאמת רמת קושי: התאם את רמת הקושי של השאלות בהתאם להתקדמות המשתמש. אם המשתמש עונה נכון על רוב השאלות, העלה את רמת הקושי. אם המשתמש מתקשה, ספק שאלות קלות יותר.\n" +
            "שפה וסגנון: השתמש בשפה ברורה, תמציתית ונעימה. הקפד על סגנון דיבור ידידותי ומשעשע כדי להפוך את הלמידה למהנה יותר.\n" +
            "דוגמאות:\n" +
            "שאלה: מהו תפקידו של רכיב TextView?\n" +
            "תשובה נכונה: רכיב TextView משמש להצגת טקסט על המסך.\n" +
            "הסבר: רכיב TextView הוא אחד מרכיבי התצוגה הבסיסיים ביותר באנדרואיד. הוא מאפשר להציג טקסט פשוט או מעוצב, וניתן להתאים את גודל הטקסט, צבעו וסגנונו.\n" +
            "שאלה: מהו תפקידו של רכיב Imageview?\n" +
            "תשובה שגויה: רכיב imageview משמש להצגת טקסט.\n" +
            "הסבר: רכיב imageview משמש להצגת תמונות על המסך.\n" +
            "מטרת המשחק:\n" +
            "\n" +
            "ללמד את המשתמשים על רכיבי התצוגה באנדרואיד בצורה אינטראקטיבית ומהנה.\n" +
            "לשפר את הידע וההבנה של המשתמשים בנושא פיתוח אפליקציות אנדרואיד.\n" +
            "הכנת משתמשים לבניית ממשקי משתמש יעילים ומתאימים.";

    public static final String CHAT_FIRST_PROMPT = "תן לי שאלה בנושא רכיבי תצוגה באנדרואיד";

}
