package com.example.smartlecture;

/**
 * Interface representing an entity that has recording capabilities.
 * Classes implementing this interface must provide an implementation for starting a recording process.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
interface IRecordable {
    /**
     * Initiates the recording process.
     */
    void startRecording();
}