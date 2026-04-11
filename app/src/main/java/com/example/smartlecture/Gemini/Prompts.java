package com.example.smartlecture.Gemini;

public class Prompts {
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
}
