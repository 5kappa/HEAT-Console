# Fitness Tracker Project Roadmap

### Logic & Code Hygiene
- [x] **Database Consistency:** Made `DatabaseManager` methods consistent.
- [x] **Error Handling:** Standardized `try-catch` blocks across `FitnessService` and new services.
- [x] **Separation of Concerns:** Reduced business logic in `ConsoleDashboard` and `DatabaseManager`.
- [x] **Encapsulation:** Verified that all methods have correct access modifiers.
- [x] **Null Safety:** Added checks to prevent null pointer exceptions or printing empty objects in `ConsoleDashboard`.
- [x] **Formatting:** Fixed the logic for printing new lines and padding in `ConsoleDashboard`.

### UX & Formatting
- [x] **Error Messages:** Made error messages more informative for the user.

---

## Completed Milestones

### Core Features
- [x] **Streaks:** Fixed streak tracking logic.
- [x] **Quotes:** Fixed and implemented the motivational quote feature.
- [x] **Goals:**
    - Added goal creation.
    - Implemented automatic completion checks.
    - Implemented expiration checks (if `today == endDate`).
- [x] **Deletion:**
    - Updated model classes to include `id` fields.
    - Added safeguards (don't prompt delete if list is empty).
    - Implemented Body Metrics deletion (with logic to revert user profile to the 2nd most recent entry).

### Safety & Integrity
- [x] **Exception Handling:** `DatabaseManager` methods now throw `SQLException` instead of swallowing errors.
- [x] **Transactions:** Implemented `autocommit` logic for atomic actions.
- [x] **Rollbacks:** `db.rollbackTransaction()` is triggered upon exceptions in the Service layer.

### Architecture & Refactoring
- [x] **Service Layer:** Flattened `FitnessService` into specific `UserService` and `GoalService`.
- [x] **Persistence:** `activities.csv` and `quotes.csv` are now migrated to database storage.
- [x] **Optimization:** `db.deleteGoal` now iterates over lists efficiently.
- [x] **Calculations:** Purely weighted PRs are now based on total volume, not just external weight.
- [x] **Logic:** Moved BMI and BMR calculations inside `WorkoutService`.

### UX & Polish
- [x] **Menu:** Reordered menu items (Universal 'go back' button is 0).
- [x] **Sorting:** Appending adds to the start of the list (newest workouts/metrics/goals show first).
- [x] **Formatting:**
    - Dates are now printed with workouts.
    - Fixed POJO `toString()` methods to align with border length (161 chars).
    - Typecasted goal values according to goal type.
    - Body Metric list is printed *before* prompting for ID.
- [x] **Feedback:**
    - "NEW PR!" message appears when achieved.
    - Pagination added for long print lists.
    - User profile displays current streak.

---

## Testing Phase Checklist

### 1. User Profile
- [x] **1.1 New User:** App launches main menu; `[11] View Profile` matches input; `[5] View Weight` shows 1 entry.
- [x] **1.2 Updating Metrics:** Update weight -> `[5]` shows 2 entries -> `[11]` matches new value.

### 2. Workout Logging
- [x] **2.1 First Log:** Logging Strength details are correct; visible in `[8] All Workouts`; visible in `[7] PRs`.
- [x] **2.2 Weaker Log:** Log lighter weight -> `[8]` shows both -> `[7]` shows original PR (unchanged).
- [x] **2.3 Stronger Log:** Log heavier weight -> `[8]` shows 3 entries -> `[7]` updates to new PR.
- [x] **2.4 Special Cases:**
    - 1-rep vs 2-rep max logic works.
    - `0kg` (reps) vs `5kg` (loaded) are treated as separate PR categories.
    - New higher weights overwrite previous entries correctly.
    - Deleting workouts correctly reverts PRs to previous bests.
- [x] **2.5 Cardio:** Logging Cardio correctly updates Cardio PRs.

### 3. Deletion Logic
- [x] **3.1 Standard Delete:** Deleting a non-PR workout succeeds; PR remains unchanged.
- [x] **3.2 PR Holder Delete:** Deleting a PR workout succeeds; PR recalculates to runner-up.
- [x] **3.3 Wipe Data:** Deleting all workouts results in "No workouts logged" message.

### 4. Goals Integration
- [x] **4.1 Creation:** Frequency goal created; visible in `[1] Active Goals`.
- [x] **4.2 Tracking:** Verified via DB Browser: Bodyweight, Load, Reps, Duration, Frequency.
- [x] **4.3 Deletion:** Delete Goal B -> List shows A and C with correct indices. Invalid deletion handles gracefully.
- [x] **4.4 Expiration:** Goal with `endDate == tomorrow` moves to `EXPIRED` status upon date change and app relaunch.

---

## Resolved Bugs

The following known issues have been **fixed**:

- [x] **Body Metrics:** Row IDs no longer reset to 0 on restart.
- [x] **PR Conflicts:** Reps vs. Loaded PRs no longer override each other.
- [x] **PR Sync:** PR table now updates immediately when a workout is deleted.
- [x] **PR Units:** Fixed wrong units printing.
- [x] **Feedback:** Success message now shows for loaded PRs even if rep variant exists.
- [x] **Validation:** Invalid index error messages corrected (0-* instead of 1-*).
- [x] **Data Loading:** Body metric history now loads correctly.
- [x] **Goals:**
    - `allGoals` status updates correctly alongside `activeGoals`.
    - Non-active goals hidden from active list.
    - Completed goals removed from active list.
    - Current value updates in database (not just memory).
- [x] **Streaks:** Streak increments correctly even if `lastWorkoutDate` wasn't today.
- [x] **Syncing:** Updating the most recent body metric now syncs with User Profile.
- [x] **Calculations:** Removed double calculation of BMI/BMR in User POJO.
- [x] **Input:** `handleEditProfile` now has input validation; removed prompt to edit profile immediately after registration.