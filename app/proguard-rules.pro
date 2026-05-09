# --- Firebase & Firestore ---
# Keep your data models to prevent R8 from renaming fields,
# which would break Firestore's automatic mapping.
-keepclassmembers class com.example.free2party.data.model.** { *; }

# --- Hilt / Dagger ---
# Usually handled by Hilt's own rules, but good to have if issues arise.
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends androidx.lifecycle.ViewModel

# --- Compose ---
# Keep Compose-specific attributes
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# --- WorkManager & Room ---
# WorkManager uses Room internally. R8 can strip generated Room implementations
# leading to "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.work.impl.**
