# Fitness-Tracker

## To do:
 * ~~Fix streaks~~
 * ~~Fix quote feature~~
 * ~~Add goals~~

 * ~~Make it so the program automatically checks if a goal is completed~~
    - ~~Program should also check if today == endDate and mark goal as expired~~

 * ~~Add delete option~~
    - ~~Update model classes to include "id" as a field for easy deletion~~
    - ~~Don't make the user delete if the list is empty~~
    - ~~Add body metrics deletion~~
      - ~~If most recent entry was deleted, user profile is now the second most recent entry~~
   
 * ~~Critical safety & database integrity~~
    - ~~DatabaseManager methods should be void and implement throws SQLException~~
    - ~~Make sure autocommit is utilized during each action~~
    - ~~db.rollbackTransaction() should be called upon catching exceptions in FitnessService~~

 * Architecture & refactoring
    - ~~Flatten FitnessService by making UserService and GoalService~~
    - Reorganize code according to usage
    - ~~activities.csv and quotes.csv should also be stored in the database~~
    - ~~Make db.deleteGoal iterate over a list of Goals~~
    - ~~Purely weighted PRs should be based on total volume and not external weight~~

 * Testing Phase
    - ~~do surface-level checks and verify that object creation, reading, and deletion are fully functional and have no bugs~~
      - To be done first before adding features like updateWorkout and updateGoal to avoid compounding bugs


<p align="center">Testing Phase Checklist</p>

 1.1. New User Registration
   - [x] App launches main menu immediately
   - [x] Choosing `[11] View Profile` should match data that was inputted
   - [x] Choosing `[5] View Weight Progress` should show exactly 1 entry

 1.2. Updating Metrics
   - [x] Choosing `[9] Update Weight` and subsequently `[5] View Weight Progress` should show 2 bodyMetric entries
   - [x] Choosing `[11] View Profile` matches the new value

 2.1. Logging the very first workout
   - [x] Logging a StrengthWorkout shows the workout with the right details
   - [x] Choosing `[8] View All Workouts` shows the workout
   - [x] Choosing `[7] View Personal Records` shows a PR

 2.2. Log a weaker workout (no PR update)
   - [x] Log the same StrengthWorkout but lighter; choosing `[8] View All Workouts` shows both workouts
   - [x] Choosing `[7] View Personal Records` shows the older workout

 2.3. Logging a stronger workout (triggers a PR update)
   - [x] Log the same StrengthWorkout but heavier; choosing `[8] View All Workouts` shows 3 workouts now
   - [x] Choosing `[7] View Personal Records` updates to show the heavier workout

 2.4. Test special cases for strength workouts
   - [x] Log a 100kg 1-rep bench press; log a 100kg 2-rep bench press; `[7] View Personal Records` shows workout B
   - [x] Log pull-ups (0kg): 10 reps; log pull-ups (5kg): 5 reps; `[7] View Personal Records` shows TWO separate workouts: "Pull-ups (loaded)" and "Pull-ups (reps)"
   - [x] Log pull-ups (0kg): 11 reps; log pull-ups (6kg): 5 reps; `[7] View Personal Records` overwrites the two previously logged PRs
   - [x] Delete the previously logged workouts; Log pull-ups (0kg): 9 reps; log pull-ups (6kg): 4 reps; `[7] View Personal Records` should show these workouts and NOT 10 reps & 5 kg

 2.5. Cardio workouts
   - [x] Log a CardioWorkout and see if `[7] View Personal Records` is updated

 3.1. Deleting a standard workout
   - [x] Delete the weaker workout from 2.2; console gives a success message
   - [x] Choose `[7] View Personal Records` and see if the PR remains unchanged

 3.2. Delete a PR holder
   - [x] Delete the strong workout from 2.3; console gives a success message
   - [x] PR should be recalculated and reverts to the workout logged in 2.1

 3.3. Delete the very last workout
   - [x] Delete the last workout from 2.1 and `[8] View All Workouts` says "No workouts logged yet" and returns
   - [x] Choosing `[7] View Personal Records` also says something like "No PRs yet"

 4.1. Creating goals
   - [x] Create a frequency goal and set tomorrow as the end date; choosing `[3] View Goals` -> `[1] Active Goals` should show that goal

 4.2. Check if progress tracking is functional (not implemented in program yet, monitor with DB Browser)
   - [x] target bodyweight
   - [x] weight lifted
   - [x] reps
   - [x] duration
   - [x] frequency
   - Note: duration should be reworked so it tallies up the minutes and doesn't select the max
           same with reps

 4.3. Goal deletion
   - [x] Create 3 dummy goals (A, B, C); delete goal B; list should immediately reprint showing only A and C with indices 1, 2
   - [x] Immediately try to delete goal C; check if the program deletes the wrong one

 4.3. Goal expiration
   - [x] Create a goal with endDate == tomorrow; close program; change system date to two days from now; relaunch application; verify if goal is gone from `[1] View Active Goals`; choose `[2] View All Goals` and see if goal is there with status == EXPIRED

 * ~~Rework duration and rep goals so it tallies up goalValues instead of selecting the max~~

 * ~~Add updating for workouts, goals, and bodyMetrics~~
    > Update - Modifying existing data without creating a new record (e.g., changing a password, editing profile info).

***
<p align="center"><strong>WE ARE HERE!</strong></p>

* Logic & code hygiene
  - ~~Viewing should give the user the option to (a) update a row (b) delete a row (c) go back to main menu~~
  - ~~Calculate BMI and BMR inside WorkoutService~~
  - Make DatabaseManager more consistent
  - Also make FitnessService (and new services) more consistent, particularly in the way that try-catch blocks are written
  - Make ConsoleDashboard and DatabaseManager have less logic
  - Check if methods have incorrect access modifiers
  - Prevent null or weird formatting by checking if ConsoleDashboard is trying to print an empty object
  - Do smth about the way new lines are printed for padding (ConsoleDashboard)
***


 * UX & formatting
    - ~~Date should also be printed when workouts are shown~~
    - "Enter start date (leave blank for today): "
    - ~~Appending adds to the end of the list; newest workouts/bodymetrics/goals should show up first~~
    - ~~bodyMetric List should be printed before prompting user to enter row ID~~
    - ~~Reorder menu items~~
      - ~~Universal 'go back' button should be 0~~
    - ~~Fix POJO toString() methods to align with border length = 161~~
    - Make error messages more informative
    - ~~If list is empty, user shouldn't be prompted to delete~~
    - ~~User profile should show streak~~
    - Fix formatting (Typecast goalValue according to goalType)
    - ~~Print a "NEW PR!" message when a new PR is achieved~~
    - ~~Paginate print methods that may return more than 10 lines at once~~
    - Make cleaner PR and goal notifs

## Known bugs:
 * ~~Subsequent body metric row IDs reset to 0 on application restart if table is already populated~~
    > ~~1, 1, 2.0, 3.0, 7500.00, 2025-12-26<br/>~~
    > ~~2, 1, 2.0, 66.0, 165000.00, 2025-12-26<br/>~~
    > ~~3, 6, 2.0, 66.0, 165000.00, 2025-12-26<br/>~~
    > ~~4, 6, 2.0, 66.0, 165000.00, 2025-12-26<br/>~~
    > ~~5, 6, 177.0, 66.0, 21.07, 2025-12-26<br/>~~
    > ~~<ins>**0**</ins>, 6, 177.0, 15.0, 4.79, 2025-12-26~~

 * ~~reps and kg (loaded) PRs override each other~~
 * ~~PR table should get updated when a workout gets deleted~~
 * ~~PR printing the wrong units~~
 * ~~Success message doesn't show when (loaded) PR is saved if (reps) variant already exists~~
 * ~~Invalid index error message should say 0-* instead of 1-*~~
 * ~~Body metric history not loading~~
 * ~~activeGoals status gets updated but not allGoals~~
 * ~~goals where status != active shouldn't show up in activeGoals~~
 * ~~If user updates workoutDate to today and lastWorkoutDate != today, streak should be incremented~~
 * ~~Updating the most recent bodyMetric entry does not update userProfile~~
 * ~~weeklyWorkouts should be based off of workouts; remove loadWeeklyWorkouts()~~
 * ~~User POJO doubly calculates BMI and BMR~~
 * ~~Goal doesn't get removed from activeGoals after being completed~~
 * ~~Goal currentValue is updated in memory but not in database~~
 * ~~handleEditProfile doesn't have input validation~~
 * ~~Program shouldn't prompt user to edit profile upon registration~~

 ## Extra:
 * Add a calculateCalorieTarget() method
 * Make workout selection search-based instead of selection-based
 * Add a progress meter to goals
 * When user chooses to exit, display one final summary and a congratulatory message