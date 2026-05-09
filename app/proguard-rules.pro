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

# --- Ads (AdMob) ---
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
