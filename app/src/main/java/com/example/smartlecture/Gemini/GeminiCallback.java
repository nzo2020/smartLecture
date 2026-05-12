package com.example.smartlecture.Gemini;

/**
 * Interface definition for a callback to be invoked when a Gemini AI operation
 * is completed, either successfully or with an error.
 * <p>
 * This allows the UI or other services to respond to the asynchronous nature
 * of AI content generation.
 * </p>
 *
 * @author Noa Zohar (nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public interface GeminiCallback {

    /**
     * Called when the AI successfully generates a response.
     * @param result The generated text or summary from Gemini.
     */
    void onSuccess(String result);

    /**
     * Called when an error occurs during the AI communication or processing.
     * @param error The exception or error encountered.
     */
    void onFailure(Throwable error);
}