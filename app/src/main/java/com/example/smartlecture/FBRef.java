package com.example.smartlecture;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FBRef {

    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();

    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();

    public static DatabaseReference refUsers = FBDB.getReference("users");
    public static DatabaseReference refLectures = FBDB.getReference("Lectures");
    public static DatabaseReference refReminders = FBDB.getReference("reminders");

    public static DatabaseReference refPubTrue = refLectures.child("pub_true");
    public static DatabaseReference refPubFalse = refLectures.child("pub_false");

    public static FirebaseStorage FBST = FirebaseStorage.getInstance();
    public static StorageReference stRef = FBST.getReference();

    public static StorageReference stRecordings = stRef.child("recordings");
    public static StorageReference stSummaries = stRef.child("summaries");
}