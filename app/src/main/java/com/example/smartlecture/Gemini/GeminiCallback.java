package com.example.smartlecture.Gemini;

/**
 * The {@code GeminiCallback} interface defines a callback mechanism for receiving results
 * from asynchronous operations performed by the {@link GeminiManager}.
 *
 * <p>
 *     <b>Remarks:</b>
 *     <ul>
 *         <li>This interface is used to handle the results of operations that interact with the Gemini AI model.</li>
 *         <li>It provides two methods: {@link #onSuccess(String)} for successful operations and {@link #onFailure(Throwable)} for handling errors.</li>
 *         <li>The {@link #onSuccess(String)} method is called when the Gemini AI model successfully returns a text response.</li>
 *         <li>The {@link #onFailure(Throwable)} method is called when an error occurs during the interaction with the Gemini AI model.</li>
 *         <li>Implementations of this interface are typically used in classes that interact with the {@link GeminiManager}.</li>
 *         <li>This interface is used to handle asynchronous operations.</li>
 *     </ul>
 * </p>
 */
public interface GeminiCallback {
    /**
     * Called when the Gemini AI model successfully returns a text response.
     *
     * @param result The text response received from the Gemini AI model.
     */
    public void onSuccess(String result);

    /**
     * Called when an error occurs during the interaction with the Gemini AI model.
     *
     * @param error The {@link Throwable} object representing the error that occurred.
     */
    public void onFailure(Throwable error);
}