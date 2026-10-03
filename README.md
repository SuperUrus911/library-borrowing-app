# Stacks: Library Book Borrowing App

An Android app for running a library's book lending: members browse the catalog and borrow or return books; librarians manage the collection and the circulation desk. Built with **Kotlin**, **Jetpack Compose** and **Firebase** (Authentication + Cloud Firestore).

<p>
  <img src="docs/screenshots/03-catalog.png" width="230" alt="Catalog" />
  <img src="docs/screenshots/05-book-detail.png" width="230" alt="Book detail" />
  <img src="docs/screenshots/07-my-loans.png" width="230" alt="My loans" />
  <img src="docs/screenshots/10-dashboard.png" width="230" alt="Librarian dashboard" />
</p>

## Features

**Members**
- Register and sign in with email and password
- Browse the catalog with live search (title, author, ISBN) and category filters
- See real-time availability ("2 of 4 available") and borrow with one tap
- Track active loans with due-date countdowns, overdue warnings and late fees
- Return books and review borrowing history
- Profile with reading stats

**Librarians (admin role)**
- Dashboard: titles, copies on the shelf, active loans, overdue loans with outstanding fees, recent activity
- Add, edit and delete books (cover preview from ISBN, copy count stepper)
- See who currently holds each book
- Circulation desk: filter all loans by *On loan / Overdue / Returned*, search by member or book, check books back in
- One-tap "Load sample books" to fill an empty catalog

**Lending rules** (in [`LibraryPolicy.kt`](app/src/main/java/dev/ijlal/stacks/data/LibraryPolicy.kt))

| Rule | Value |
| --- | --- |
| Loan period | 14 days |
| Books per member at a time | 3 |
| Same title twice at once | Not allowed |
| Late fee | Rp2.000 per day |

## Screenshots

### Member

| Sign in | Register | Catalog | Search & filter |
| --- | --- | --- | --- |
| <img src="docs/screenshots/01-login.png" width="200"/> | <img src="docs/screenshots/02-register.png" width="200"/> | <img src="docs/screenshots/03-catalog.png" width="200"/> | <img src="docs/screenshots/04-catalog-search.png" width="200"/> |

| Book detail | Borrow | Borrowed | My loans |
| --- | --- | --- | --- |
| <img src="docs/screenshots/05-book-detail.png" width="200"/> | <img src="docs/screenshots/06-borrow-confirm.png" width="200"/> | <img src="docs/screenshots/06b-borrow-success.png" width="200"/> | <img src="docs/screenshots/07-my-loans.png" width="200"/> |

| Return | History | Profile | Loan limit reached |
| --- | --- | --- | --- |
| <img src="docs/screenshots/08-return-confirm.png" width="200"/> | <img src="docs/screenshots/08b-history.png" width="200"/> | <img src="docs/screenshots/09-profile.png" width="200"/> | <img src="docs/screenshots/09b-limit-reached.png" width="200"/> |

### Librarian

| Dashboard | Catalog | Book detail (borrowers) | Add / edit book |
| --- | --- | --- | --- |
| <img src="docs/screenshots/10-dashboard.png" width="200"/> | <img src="docs/screenshots/11-admin-catalog.png" width="200"/> | <img src="docs/screenshots/12-admin-book-detail.png" width="200"/> | <img src="docs/screenshots/13-book-form.png" width="200"/> |

| Loans: on loan | Loans: overdue | Check in | Profile |
| --- | --- | --- | --- |
| <img src="docs/screenshots/14-admin-loans.png" width="200"/> | <img src="docs/screenshots/15-admin-overdue.png" width="200"/> | <img src="docs/screenshots/16-checkin-confirm.png" width="200"/> | <img src="docs/screenshots/17-admin-profile.png" width="200"/> |

## Try it

### Option 1: install the APK

Download `stacks-v1.0.0.apk` from the [latest release](../../releases/latest) and install it on any Android 8.0+ device. It talks to a live demo Firebase project.

### Option 2: build from source

Requirements: Android Studio (or JDK 17+ and the Android SDK with platform 37).

```bash
git clone https://github.com/SuperUrus911/library-borrowing-app.git
cd library-borrowing-app
./gradlew installDebug   # or open the folder in Android Studio and press Run
```

`app/google-services.json` points to the demo Firebase project, so the app works out of the box.

### Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Librarian | `librarian@stacks.test` | `stacks123` |
| Member | `concerto@stacks.test` | `stacks123` |

The member account comes with a borrowing history, including an overdue book. You can also register a new member from the sign-in screen.

## Using your own Firebase project

1. Create a project in the [Firebase console](https://console.firebase.google.com/) and add an Android app with package name `dev.ijlal.stacks`.
2. Download its `google-services.json` into `app/`, replacing the existing one.
3. Enable **Authentication → Sign-in method → Email/Password**.
4. Create a **Cloud Firestore** database, then deploy the security rules:
   ```bash
   firebase use --add <your-project-id>
   firebase deploy --only firestore
   ```
5. Register an account in the app, then make it a librarian by setting `role` to `"admin"` on its document in `users/{uid}` in the Firestore console.
6. Sign in as the librarian and tap **Load sample books**.

## Architecture

```
app/src/main/java/dev/ijlal/stacks
├── data/                 Firebase access and business rules
│   ├── model/Models.kt   UserProfile, Book, Loan (Firestore documents)
│   ├── AuthRepository    Auth state + live user profile → Session
│   ├── BookRepository    Catalog CRUD, stock-safe edits/deletes
│   ├── LoanRepository    Borrow / return as Firestore transactions
│   ├── LibraryPolicy     Loan period, limits, late fees, due-date state
│   └── AppContainer      Manual dependency container
└── ui/                   Jetpack Compose, one ViewModel per screen
    ├── auth/             Sign in, register
    ├── catalog/          Catalog with search and category filters
    ├── book/             Book detail, add/edit form
    ├── loans/            Member's loans and history
    ├── admin/            Dashboard, circulation desk
    ├── profile/          Profile and sign out
    ├── home/             Navigation graph and role-based bottom bar
    └── components/       Book covers, loan cards, pills, dialogs
```

- **MVVM + unidirectional data flow.** Repositories expose Firestore snapshot listeners as Kotlin `Flow`s; ViewModels combine them into `StateFlow` UI state; Compose screens render it. Every screen updates in real time, so a librarian sees a loan the moment a member borrows.
- **Role-based UI.** One app, two experiences. The `role` field on the user's profile picks the bottom navigation: *Catalog · My loans · Profile* for members, *Dashboard · Catalog · Loans · Profile* for librarians.
- **Consistent stock.** Borrowing reads the book and the member's profile and then writes the loan, the stock decrement and the member's active-book list in one **Firestore transaction**. Two members can't take the last copy at the same time, and the per-member limit can't be bypassed by tapping twice. Returning reverses all three writes atomically.
- **Security rules** ([`firestore.rules`](firestore.rules)) enforce the same rules on the server. Members read only their own profile and loans, can only move a book's stock by exactly one copy, and can only create a loan in the same transaction that takes a copy off the shelf. Only librarians can edit the catalog or other members' loans.
- **Covers** come from the Open Library Covers API by ISBN (loaded with Coil); books without one get a generated cloth-bound cover.

### Firestore data model

| Collection | Fields |
| --- | --- |
| `users/{uid}` | `name`, `email`, `role` (`member` \| `admin`), `activeBookIds[]`, `createdAt` |
| `books/{id}` | `title`, `author`, `isbn`, `category`, `publishedYear`, `description`, `totalCopies`, `availableCopies`, `createdAt` |
| `loans/{id}` | `bookId`, `bookTitle`, `bookAuthor`, `bookIsbn`, `userId`, `userName`, `userEmail`, `status` (`BORROWED` \| `RETURNED`), `borrowedAt`, `dueAt`, `returnedAt` |

Loans copy the book and member names, so lists render without extra reads and history survives a book being deleted.

## Tech stack

| | |
| --- | --- |
| Language | Kotlin 2.4 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Async | Kotlin Coroutines & Flow |
| Backend | Firebase Authentication, Cloud Firestore (BoM 34) |
| Images | Coil 3 |
| Build | Gradle 9.8 (Kotlin DSL, version catalog), AGP 9.4 |
| Min / target SDK | 26 (Android 8.0) / 36 |
