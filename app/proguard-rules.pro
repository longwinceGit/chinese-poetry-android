# Room
-keep class com.poetry.data.model.** { *; }
-keep class com.poetry.data.UserProfile { *; }
-keep class com.poetry.data.LearningRecord { *; }
-keep class com.poetry.data.DailyStats { *; }

# Keep data classes
-keepattributes *Annotation*

# Room entities
-keep @androidx.room.Entity class * {
    @androidx.room.PrimaryKey <fields>;
    <fields>;
}

# Room DAO
-keep @androidx.room.Dao interface * { *; }

# Pinyin4j
-keep class net.sourceforge.pinyin4j.** { *; }
-keep class com.belerweb.pinyin4j.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# General Android
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
