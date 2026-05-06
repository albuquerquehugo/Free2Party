# 📅 Free2Party: Let's get together sometime - Social Availability Planner

**Free2Party** is a social Android application designed to help friends coordinate their availability in real-time, simplifying the logistics of group gatherings. Whether you're looking for someone to grab a coffee with or planning a night out, Free2Party lets you see who is "Free" at a glance. This project serves as a showcase of how to build a scalable, user-centric product from ideation to a production-ready state.

---

## ✨ Features
- **Real-time Availability**: Toggle your status between "Free to Party" and "Busy" with a single tap.
- **Social Synchronization**: Your friends' availability updates instantly on your screen thanks to Firebase Firestore's real-time listeners.
- **Passive Interaction**: Let friends come to you when you are free.
- **Friend Management**:
    - Invite friends by email.
    - Accept or decline incoming friend requests.
    - Mutual friend removal and invite cancellation.
- **Secure Authentication**: Robust sign-up and login system powered by Firebase Authentication.
- **Server-Side Accuracy**: Uses Google's server timestamps for all social interactions to ensure consistency across devices.
- **Cost-Efficiency**: Built on a serverless architecture (Firebase) for zero overhead during development.
- **Modern UI**: A clean, Material 3 interface with a focus on ease of use.
- **Adaptive Layouts**:
    - **Intelligent Orientation**: Automatically locks to portrait on phones but enables full landscape rotation on tablets/foldables.
    - **Tablet-Optimized Calendar**: A dedicated side-by-side view in landscape mode for managing plans efficiently.

---

## 🛠 Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/).
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - Material 3 (Modern Declarative UI).
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html).
- **Backend**: [Firebase](https://firebase.google.com/)
    - **Authentication**: Email/Password and Google login options.
    - **Cloud Firestore**: Real-time NoSQL database for user profiles and friendship tracking.
    - **Cloud Messaging**: Reliable and battery-efficient cross-platform messaging (Planned).
- **Architecture**: MVVM (Model-View-ViewModel) with the Repository pattern.
- **Dependency Injection**: Hilt for robust, scalable dependency management.
- **Dependency Management**: Gradle (Version Catalog/KTS).
- **Min SDK**: API 24 (Android 7.0).
- **Target SDK**: API 37 (Android 17).
- **Other Libraries**:
    - Navigation Compose for app routing.
    - Lifecycle ViewModel for state management.
    - Google Fonts (Montserrat) for custom typography.
    - Compose Material 3 Adaptive for window size management.
---

## 🏗 Architecture / Project Structure
To ensure the app remains maintainable and scalable for a future App Store launch, the project follows a modular and clean architecture approach. This separates the business logic from the UI and data sources.
- **`data/`**: Repositories, Data Sources, and Models.
- **`di/`**: Hilt modules for providing project-wide dependencies.
- **`exception/`**: Custom exception mapping for cleaner error handling.
- **`ui/`**: Screen-specific Composables and ViewModels.
- **`util/`**: Helper functions and extensions.

---

## 🛡 Security & Best Practices
* **Environment Variables**: All sensitive keys are not committed to version control.
* **Input Validation**: Robust client-side and server-side validation to ensure data integrity.
* **Performance**: Optimized asset loading and memoization to ensure 60fps performance on mid-range devices.

---

## 📝 MVP Roadmap (Sprint 1) -- DONE --
- [x] **Phase 1: Foundation**
    - [x] Project setup & Firebase connection.
    - [x] Google/Email Authentication.
    - [x] Basic Profile creation (Name, Bio).
- [x] **Phase 2: The Core Logic**
    - [x] "I'm Free" toggle button.
    - [x] Firestore logic to sync status in real-time.
    - [x] Availability schedule (e.g., "Free from 12 pm to 1 pm").
- [x] **Phase 3: Social Layer**
    - [x] Search users by email.
    - [x] Friend request system.
    - [x] Friends list showing "Free/Busy" status.
- [x] **Phase 4: Interaction**
    - [x] "Invite" button (triggers Push Notification).
    - [x] Deep-link to WhatsApp/SMS for chat.

## 📝 MVP Roadmap (Sprint 2) -- PLANNED --
- [ ] Phase 5: Groups & Circles
    - [x] Create "Circles" (e.g., Close Friends, Coworkers, Family) to filter availability.
    - [ ] Group privacy settings (choose which circles see your "Free" status).
- [ ] Phase 6: Plan Proposals (RSVP System)
    - [ ] Propose a specific plan (e.g., "Beer at 7pm @ The Local Pub").
    - [ ] Friends can RSVP "In", "Out", or "Maybe" directly in the app.
    - [ ] Shared plan chat or comments for quick logistics.
- [ ] Phase 7: Smart Engagement
    - [ ] Status History/Trends: See when friends are typically free during the week (Passive Analytics).
    - [ ] Rich Notifications: Interactive notifications (Accept/Decline invitations from the lock screen).
- [ ] Phase 8: Production Polish
    - [ ] App Analytics & Crashlytics: Integrating Firebase Crashlytics to monitor stability.
    - [ ] Theming: User-selectable "Status Colors" or custom emojis for the "Free" state.
    - [ ] Onboarding: A dedicated tutorial flow for new users explaining the "Passive Interaction" concept.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Panda or newer.
- A Firebase project.

### Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/Free2Party.git
   ```
2. **Firebase Configuration**:
    - Go to the [Firebase Console](https://console.firebase.google.com/).
    - Create a new project named "Free2Party".
    - Add an Android app with the package name `com.example.free2party`.
    - Download the `google-services.json` and place it in the `app/` directory.
    - Enable **Email/Password** authentication in the Firebase Auth tab.
    - Create a **Firestore Database** in test mode (or setup appropriate security rules).
3. **Build and Run**:
    - Open the project in Android Studio.
    - Sync Gradle.
    - Run the app on an emulator or physical device.

---
*Created by [Hugo Albuquerque](https://github.com/albuquerquehugo)*
