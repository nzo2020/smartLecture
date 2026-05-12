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

/**
 * Singleton class that manages communication with the Google Gemini AI model.
 * It handles session initialization, system instructions, and asynchronous message delivery.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class GeminiChatManager {
    /** Singleton instance of the manager */
    // שימוש בתבנית עיצוב Singleton: מבטיח שיהיה רק מנהל AI אחד בכל האפליקציה כדי לחסוך במשאבים.
    private static GeminiChatManager instance;
    /** The generative model instance used to process requests */
    private GenerativeModel gemini; // המודל הגנרטיבי עצמו
    /** The active chat session that maintains conversation history */
    private Chat chat; // אובייקט המנהל את היסטוריית השיחה (Session)
    /** Logging tag for debugging */
    private final String TAG = "GeminiChatManager";

    /**
     * Initializes a new chat session with an empty history.
     */
    private void startChat() {
        chat = gemini.startChat(Collections.emptyList());
    }

    /**
     * Private constructor to initialize the Gemini model with specific system instructions.
     * @param systemPrompt The instruction set that defines the AI's role and behavior.
     */
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

    /**
     * Provides access to the Singleton instance of the manager.
     * @param systemPrompt The prompt to use if the instance needs initialization.
     * @return The single instance of GeminiChatManager.
     */
    public static GeminiChatManager getInstance(String systemPrompt) {
        if (instance == null) {
            instance = new GeminiChatManager(systemPrompt);
        }
        return instance;
    }

    /**
     * Sends a user message to the AI and handles the response asynchronously.
     * @param prompt The message or question from the user.
     * @param callback Interface to handle success or failure results.
     */
    public void sendChatMessage(String prompt, GeminiCallback callback) {
        // sendMessage היא פעולה אסינכרונית (פועלת ברשת) ולכן דורשת "Continuation"
        chat.sendMessage(prompt,
                new Continuation<GenerateContentResponse>() {
                    /**
                     * Returns the coroutine context.
                     * @return The empty coroutine context.
                     */
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        // ריצה על הקשר ריק (בדרך כלל באנדרואיד זה יחזור ל-Main Thread דרך ה-Callback)
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    /**
                     * Resumes the execution with the result of the AI response.
                     * @param result The result object containing the success response or failure exception.
                     */
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