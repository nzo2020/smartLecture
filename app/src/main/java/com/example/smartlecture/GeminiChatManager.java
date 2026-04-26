package com.example.smartlecture;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartlecture.Gemini.GeminiCallback;
import com.example.smartlecture.BuildConfig;
import com.google.ai.client.generativeai.Chat;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.ai.client.generativeai.type.TextPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;


public class GeminiChatManager {
    // שימוש בתבנית עיצוב Singleton: מבטיח שיהיה רק מנהל AI אחד בכל האפליקציה כדי לחסוך במשאבים.
    private static GeminiChatManager instance;
    private GenerativeModel gemini; // המודל הגנרטיבי עצמו
    private Chat chat; // אובייקט המנהל את היסטוריית השיחה (Session)
    private final String TAG = "GeminiChatManager";


    private void startChat() {
        chat = gemini.startChat(Collections.emptyList());
    }

    private GeminiChatManager(String systemPrompt) {
        List<Part> parts = new ArrayList<Part>();
        parts.add(new TextPart(systemPrompt)); // הפיכת הטקסט ל"חלק" (Part) שהמודל יודע לקרוא

        // אתחול המודל עם פרמטרים קבועים
        gemini = new GenerativeModel(
                "gemini-2.0-flash", // שם המודל (מהיר ויעיל למובייל)
                BuildConfig.Gemini_API_Key, // מפתח ה-API שנשמר ב-BuildConfig מטעמי אבטחה
                null,
                null,
                new RequestOptions(),
                null,
                null,
                new Content(parts) // הגדרת תוכן המערכת (System Instructions)
        );
        startChat();
    }


    public static GeminiChatManager getInstance(String systemPrompt) {
        if (instance == null) {
            instance = new GeminiChatManager(systemPrompt);
        }
        return instance;
    }


    public void sendChatMessage(String prompt, GeminiCallback callback) {
        // sendMessage היא פעולה אסינכרונית (פועלת ברשת) ולכן דורשת "Continuation"
        chat.sendMessage(prompt,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        // ריצה על הקשר ריק (בדרך כלל באנדרואיד זה יחזור ל-Main Thread דרך ה-Callback)
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        // בדיקה האם הפעולה נכשלה
                        if (result instanceof Result.Failure) {
                            Log.i(TAG, "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        } else {
                            // הפעולה הצליחה - שליפת הטקסט מתוך אובייקט התגובה
                            Log.i(TAG, "Success: " + ((GenerateContentResponse) result).getText());
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }
}