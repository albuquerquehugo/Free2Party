# 📅 Free2Party - Android Social Planner

**Free2Party** is a "passive" social network designed to eliminate the back-and-forth of "Are you free?" Users simply toggle their availability, and friends can see at a glance who is ready to hang out, attend a party, or grab a healthy meal.

---

## 🚀 The Vision
* **Low Friction:** One tap to signal availability.
* **Passive Interaction:** Let friends come to you when you are free.
* **Cost-Efficient:** Built on a serverless architecture (Firebase) for zero overhead during development.

---

## 🛠 Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Modern Declarative UI)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Backend:** Firebase (Auth, Firestore, Cloud Messaging)
- **Dependency Injection:** Hilt (Planned)
- **Min SDK:** API 24 (Android 7.0)

---

## 📝 MVP Roadmap (Sprint 1)
- [ ] **Phase 1: Foundation**
    - [ ] Project setup & Firebase connection.
    - [ ] Google/Email Authentication.
    - [ ] Basic Profile creation (Name, Bio).
- [ ] **Phase 2: The Core Logic**
    - [ ] "I'm Free" toggle button.
    - [ ] Firestore logic to sync status in real-time.
    - [ ] Availability duration (e.g., "Free for 2 hours").
- [ ] **Phase 3: Social Layer**
    - [ ] Search users by email.
    - [ ] Friend request system.
    - [ ] Friends list showing "Free/Busy" status.
- [ ] **Phase 4: Interaction**
    - [ ] "Invite" button (triggers Push Notification).
    - [ ] Deep-link to WhatsApp/SMS for chat.

---

## 🏗 Project Structure
- `data/`: Firestore models and Repository patterns.
- `ui/`: Compose Screens and ViewModels.
- `util/`: Date formatters and Permission handlers.

---

## 🔒 Private Notes & Environment
- **Firebase Console:** [Link to your project]
- **Key Constraints:** Focus on performance and battery efficiency (low background syncing).
