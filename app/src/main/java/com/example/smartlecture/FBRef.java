package com.example.smartlecture;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class FBRef {

    // --- אימות משתמשים (Authentication) ---
    // אובייקט לניהול התחברות, הרשמה וניתוק משתמשים
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();

    // --- מסד נתונים בזמן אמת (Realtime Database) ---
    // אובייקט הגישה הראשי למסד הנתונים של Firebase
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();

    // יצירת קישורים לטבלאות (Nodes) הראשיות במסד הנתונים:
    public static DatabaseReference refUsers = FBDB.getReference("users");      // נתוני משתמשים (שם, אימייל וכו')
    public static DatabaseReference refLectures = FBDB.getReference("Lectures"); // ריכוז כל ההרצאות במערכת
    public static DatabaseReference refReminders = FBDB.getReference("reminders"); // תזכורות חכמות ואירועי יומן

    // תתי-קישורים למיון הרצאות לפי סטטוס הפרסום שלהן (Public/Private):
    public static DatabaseReference refPubTrue = refLectures.child("pub_true");   // הרצאות ששותפו עם כולם
    public static DatabaseReference refPubFalse = refLectures.child("pub_false"); // הרצאות פרטיות של המשתמש

    // --- אחסון קבצים (Firebase Storage) ---
    // אובייקט הגישה הראשי לאחסון קבצים פיזיים (אודיו, טקסט וכו')
    public static FirebaseStorage FBST = FirebaseStorage.getInstance();

    // קישור לשורש (Root) של מרחב האחסון
    public static StorageReference stRef = FBST.getReference();

    // יצירת תיקיות ייעודיות באחסון הענן:
    public static StorageReference stRecordings = stRef.child("recordings"); // תיקייה השומרת את קבצי הקול (MP3/3GP) של ההרצאות
    public static StorageReference stSummaries = stRef.child("summaries");   // תיקייה השומרת קבצי טקסט או סיכומים שנוצרו
}