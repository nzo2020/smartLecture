package com.example.smartlecture;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Helper class that centralizes all Firebase references used in the application.
 * This includes references for Authentication, Realtime Database nodes, and Cloud Storage folders.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class FBRef {

    // --- אימות משתמשים (Authentication) ---
    /** Static reference to the Firebase Authentication instance */
    // אובייקט לניהול התחברות, הרשמה וניתוק משתמשים
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();

    // --- מסד נתונים בזמן אמת (Realtime Database) ---
    /** Static reference to the Firebase Realtime Database instance */
    // אובייקט הגישה הראשי למסד הנתונים של Firebase
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();

    // יצירת קישורים לטבלאות (Nodes) הראשיות במסד הנתונים:
    /** Database reference to the "users" node */
    public static DatabaseReference refUsers = FBDB.getReference("users");      // נתוני משתמשים (שם, אימייל וכו')
    /** Database reference to the "Lectures" node */
    public static DatabaseReference refLectures = FBDB.getReference("Lectures"); // ריכוז כל ההרצאות במערכת
    /** Database reference to the "reminders" node */
    public static DatabaseReference refReminders = FBDB.getReference("reminders"); // תזכורות חכמות ואירועי יומן

    // תתי-קישורים למיון הרצאות לפי סטטוס הפרסום שלהן (Public/Private):
    /** Database reference to public lectures (shared with everyone) */
    public static DatabaseReference refPubTrue = refLectures.child("pub_true");   // הרצאות ששותפו עם כולם
    /** Database reference to private lectures (user-only) */
    public static DatabaseReference refPubFalse = refLectures.child("pub_false"); // הרצאות פרטיות של המשתמש

    // --- אחסון קבצים (Firebase Storage) ---
    /** Static reference to the Firebase Storage instance */
    // אובייקט הגישה הראשי לאחסון קבצים פיזיים (אודיו, טקסט וכו')
    public static FirebaseStorage FBST = FirebaseStorage.getInstance();

    // קישור לשורש (Root) של מרחב האחסון
    /** Storage reference to the root of the cloud storage bucket */
    public static StorageReference stRef = FBST.getReference();

    // יצירת תיקיות ייעודיות באחסון הענן:
    /** Storage reference to the "recordings" folder for audio files */
    public static StorageReference stRecordings = stRef.child("recordings"); // תיקייה השומרת את קבצי הקול (MP3/3GP) של ההרצאות
    /** Storage reference to the "summaries" folder for generated text files */
    public static StorageReference stSummaries = stRef.child("summaries");   // תיקייה השומרת קבצי טקסט או סיכומים שנוצרו
}