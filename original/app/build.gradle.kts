import java.util.Properties

plugins {
    id("com.android.application")
}

val secrets = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val recipeKey = secrets.getProperty("spoonacularApiKey", "").trim()
require(recipeKey.matches(Regex("[A-Za-z0-9_-]*"))) { "Invalid Spoonacular key format" }

android {
    namespace = "com.example.dietpal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dietpal"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0-original"
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$recipeKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures { buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.25")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
