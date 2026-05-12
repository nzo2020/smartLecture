package com.example.smartlecture.Gemini;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartlecture.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.BlobPart;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.ImagePart;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;

import java.util.ArrayList;
import java.util.List;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Singleton manager class responsible for communicating with the Google Gemini AI API.
 * It provides methods to generate content based on text, images, and raw file data.
 * <p>
 * This class uses the "gemini-2.5-flash" model and handles the bridge between
 * Kotlin Coroutines and Java using the {@link Continuation} interface.
 * </p>
 *
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class GeminiManager {

    /** The single instance of the GeminiManager */
    private static GeminiManager instance;

    /** The core generative model object from Google's AI SDK */
    private GenerativeModel gemini;

    /** Tag used for logging events and errors within this class */
    private final String TAG = "GeminiManager";

    /**
     * Private constructor that initializes the GenerativeModel using the API Key
     * defined in the project's BuildConfig.
     */
    private GeminiManager() {
        gemini = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.Gemini_API_Key
        );
    }

    /**
     * Provides access to the Singleton instance of GeminiManager.
     * @return The existing instance or a new one if it hasn't been created yet.
     */
    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    /**
     * Sends a simple text-only prompt to the AI.
     * @param prompt The question or instruction for the AI.
     * @param callback The interface to handle the AI's response or failure.
     */
    public void sendTextPrompt(String prompt, GeminiCallback callback) {
        gemini.generateContent(prompt,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            Log.i(TAG, "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        } else {
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }

    /**
     * Sends a text prompt combined with an image (multimodal request).
     * @param prompt Contextual text explaining the image.
     * @param photo A Bitmap object representing the image to analyze.
     * @param callback The interface to handle the AI's response or failure.
     */
    public void sendTextWithPhotoPrompt(String prompt, Bitmap photo, GeminiCallback callback) {
        List<Part> parts = new ArrayList<>();
        parts.add(new TextPart(prompt));
        parts.add(new ImagePart(photo));

        Content[] content = new Content[1];
        content[0] = new Content(parts);

        gemini.generateContent(content,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            Log.i(TAG, "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        } else {
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }

    /**
     * Sends a text prompt with a raw file (e.g., audio, PDF) provided as a byte array.
     * @param prompt The instruction for processing the file.
     * @param bytes The raw data of the file.
     * @param mimeType The type of file (e.g., "audio/wav", "application/pdf").
     * @param callback The interface to handle the AI's response or failure.
     */
    public void sendTextWithFilePrompt(String prompt, byte[] bytes, String mimeType, GeminiCallback callback) {
        List<Part> parts = new ArrayList<>();
        parts.add(new TextPart(prompt));
        parts.add(new BlobPart(mimeType, bytes));

        Content[] content = new Content[1];
        content[0] = new Content(parts);

        gemini.generateContent(content,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            Log.i(TAG, "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        } else {
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }
}