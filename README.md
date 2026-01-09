# HEAT Console - Technical Project Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture & Design](#architecture--design)
3. [Core Components](#core-components)
4. [Data Models](#data-models)
5. [Database Schema](#database-schema)
6. [Business Logic](#business-logic)
7. [API Reference](#api-reference)
8. [User Interface](#user-interface)
9. [Setup & Configuration](#setup--configuration)
10. [Development Guide](#development-guide)

---

## Project Overview

### Purpose
H.E.A.T.: Health, Exercise, and Activity Tracker is a comprehensive fitness tracking application built in Java with SQLite persistence. It provides users with robust tools to track workouts, monitor body metrics, set fitness goals, and maintain workout streaks.

### Technology Stack
- **Language:** Java (Object-Oriented)
- **Database:** SQLite (JDBC)
- **Architecture:** Monolithic Layered Architecture
- **Persistence:** DAO Pattern with Transaction Management

### Key Features
- Strength & Cardio Workout Logging
- Automatic Personal Record (PR) Tracking & Recalculation
- Smart Goal Management with Progress Tracking
- Body Metric History with BMI/BMR Calculations
- Workout Streak Tracking with Validation
- Motivational Quote System
- Paginated Data Views (10 items per page)
- Full CRUD Operations on All Entities

---

## Architecture & Design

### Architectural Pattern: Layered Architecture

```
┌─────────────────────────────────────────────┐
│         Presentation Layer (UI)             │
│  ConsoleDashboard, InputHelper              │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Service Layer (Business Logic)      │
│  WorkoutService, UserService, GoalService   │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│    Data Access Layer (Persistence)          │
│  WorkoutDAO, UserDAO, GoalDAO               │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Database (SQLite)                   │
│  DatabaseConnection (Singleton)             │
└─────────────────────────────────────────────┘
```

### Design Patterns Used

#### 1. Singleton Pattern
**Class:** `DatabaseConnection`
- Ensures single database connection throughout application lifecycle
- Thread-safe with `synchronized getInstance()`
- Manages connection lifecycle and transaction state

#### 2. Data Access Object (DAO) Pattern
**Classes:** `WorkoutDAO`, `UserDAO`, `GoalDAO`
- Abstracts database operations from business logic
- Encapsulates SQL queries and result set processing
- Provides clean separation of concerns

#### 3. Service Layer Pattern
**Classes:** `WorkoutService`, `UserService`, `GoalService`
- Coordinates business logic and transaction management
- Orchestrates multiple DAO operations within transactions
- Maintains in-memory caches synchronized with database

#### 4. Template Method Pattern
**Class:** `InputHelper`
- `printWorkouts()`, `printGoals()`, `printBodyMetrics()` share pagination logic
- Common structure with specialized rendering per entity type

#### 5. Strategy Pattern (Implicit)
**Class:** `WorkoutService`
- Different PR calculation strategies for weighted vs bodyweight exercises
- Different calorie calculation based on MET values and workout type

---

## Core Components

### 1. Presentation Layer (`heat.ui`)

#### ConsoleDashboard
**Purpose:** Main entry point and menu controller

**Key Responsibilities:**
- Display main menu and route user choices
- Initialize services with proper dependency injection
- Validate streak on application startup

**Dependencies:**
```java
- UserService userService
- InputHelper inputHelper
```

**Main Flow:**
```java
1. Check if user is registered → captureUserProfileInput()
2. Display menu loop
3. Delegate to InputHelper methods
4. Handle exit gracefully
```

#### InputHelper
**Purpose:** Handles all user input/output operations and data presentation

**Key Responsibilities:**
- Capture and validate user input
- Display formatted data with pagination
- Manage CRUD operations for all entities
- Route between viewing, editing, and deleting workflows

**Major Methods:**
- `captureWorkoutInput()` - Log strength/cardio workouts
- `captureGoalInput()` - Create fitness goals
- `showAllWorkouts()` - View workout history with CRUD options
- `showPersonalRecords()` - View PRs with delete option
- `showGoalsMenu()` - Navigate goal views
- `showBodyMetricHistory()` - View body metric history
- `printWorkouts()` / `printGoals()` / `printBodyMetrics()` - Paginated display

**Pagination Logic:**
- Displays 10 items per page
- Navigation: `[N]ext`, `[P]revious`, `[Q]uit`
- Automatically handles single-page vs multi-page views

---

### 2. Service Layer (`heat.service`)

#### WorkoutService
**Purpose:** Core workout and PR management logic

**Key Features:**
```java
// In-Memory Caches
- List<Workout> workouts
- Map<String, PersonalRecord> personalRecords
- Map<String, List<String>> activitiesByCategory
- Map<String, Activity> activitiesByName
- Map<String, List<String>> quoteCatalog
```

**Critical Methods:**

**`logWorkout(Workout w)`**
```java
Transaction Flow:
1. BEGIN TRANSACTION
2. Save workout to database (WorkoutDAO)
3. Check if new PR → update PR table
4. Check goals → mark completed if threshold met
5. COMMIT TRANSACTION
6. Update in-memory caches
7. Trigger streak recalculation
```

**`updateWorkout(Workout original, Workout updated)`**
```java
Complex Logic:
1. Update workout in database
2. Check if PR key changed (exercise name or loaded/reps variant)
3. If old PR holder deleted → recalculate old PR
4. If new values beat current PR → update PR
5. Refresh goal progress
6. Recalculate streak (workout date may have changed)
```

**`deleteWorkout(Workout w)`**
```java
1. Delete from database
2. If workout was PR holder → recalculate PR from remaining workouts
3. Refresh goal progress (currentValue may decrease)
4. Recalculate streak
```

**PR Key Generation Logic:**
```java
private String generateKey(Workout w) {
    if (w instanceof StrengthWorkout) {
        if (bodyWeightFactor != 0 && externalWeight > 0) 
            return baseName + " (loaded)";
        else if (externalWeight == 0 && bodyWeightFactor != 0) 
            return baseName + " (reps)";
    }
    return baseName;
}
```

**PR Evaluation Logic:**
```java
isNewPR(Workout w, PersonalRecord oldPR):
  For StrengthWorkout:
    - If externalWeight > 0: Compare by weight first, then reps
    - If externalWeight == 0: Compare by reps only
  For CardioWorkout:
    - Compare by duration
```

#### UserService
**Purpose:** User profile and body metrics management

**Key Features:**
```java
// State Management
- User currentUser
- List<BodyMetric> bodyMetricHistory
- GoalService goalService (circular dependency)
```

**Critical Methods:**

**`saveUserProfile(...)` / `updateProfile(User updatedUser)`**
```java
Transaction Flow:
1. BEGIN TRANSACTION
2. Calculate BMI and BMR
3. Update/Insert user profile
4. Evaluate weight goals (may complete "weight loss"/"weight gain" goals)
5. Update goal statuses in batch
6. COMMIT TRANSACTION
7. Archive completed goals in memory
```

**`recalculateStreak(List<LocalDate> allWorkoutDates)`**
```java
Algorithm:
1. Sort dates descending (newest first)
2. Start with streak = 1 at most recent date
3. Iterate through dates:
   - If gap == 1 day → increment streak
   - If gap > 1 day → break (streak ends)
4. Update user profile if streak changed
```

**`validateStreakOnStartup()`**
```java
Checks if last workout date is > 1 day ago
If yes → reset streak to 0
Displays notice to user
```

**`deleteBodyMetric(BodyMetric bm)`**
```java
Special Logic:
- If deleting the most recent metric:
  1. Revert user profile to 2nd most recent entry
  2. Recalculate BMR with previous values
  3. Re-evaluate weight goals
```

**BMI/BMR Calculations:**
```java
calculateBMI(weight, height): 
  weight / (height/100)²

calculateBMR(height, weight, age, sex):
  Male:   88.36 + (13.4 * weight) + (4.8 * height) - (5.7 * age)
  Female: 447.6 + (9.2 * weight) + (3.1 * height) - (4.3 * age)
```

#### GoalService
**Purpose:** Goal lifecycle management and progress tracking

**Key Features:**
```java
// State Management
- List<Goal> goals (all goals)
- List<Goal> activeGoals (status == ACTIVE only)
```

**Critical Methods:**

**`refreshGoalsForWorkout(Workout w)`**
```java
Called after every workout logged/updated/deleted:

1. Filter goals by:
   - Status != EXPIRED
   - Workout date within goal period
   - Exercise name matches (if applicable)
   
2. For each relevant goal:
   a. Recalculate currentValue from database
   b. Check if target reached
   c. If newly completed → add to completedGoals list
   d. If previously completed but now below target → revive to ACTIVE
   
3. Update database with new currentValues and statuses
4. Return list of newly completed goals
```

**`getCurrentValue(goalType, exerciseName, startDate)`**
```java
Queries database based on goal type:
- "weight loss"/"weight gain" → userService.getWeightKg()
- "reps" → MAX(reps) WHERE exercise = X AND date >= startDate
- "duration" → SUM(duration) WHERE exercise = X AND date >= startDate
- "weight lifted" → MAX(weight_kg) WHERE exercise = X AND date >= startDate
- "frequency" → COUNT(*) WHERE exercise = X AND date >= startDate
```

**`checkGoalExpiration()`**
```java
Called on service initialization:
1. Find all active goals where endDate < today
2. Batch update to EXPIRED status
3. Remove from activeGoals list
4. Maintain in goals list with EXPIRED status
```

**`isGoalCompleted(currentValue, targetValue, goalType)`**
```java
"weight loss" → currentValue <= targetValue
Others        → currentValue >= targetValue
```

---

### 3. Data Access Layer (`heat.dao`)

#### DatabaseConnection (Singleton)
**Purpose:** Manage single SQLite connection and transactions

**Key Methods:**
```java
getInstance()           // Thread-safe singleton access
getConnection()         // Returns active connection
beginTransaction()      // Sets autoCommit = false
commitTransaction()     // Commits and resets autoCommit = true
rollbackTransaction()   // Rollback and resets autoCommit = true
initializeTables()      // Creates schema on first run
```

**Transaction Management Pattern:**
```java
try {
    dbConnection.beginTransaction();
    // Multiple DAO operations
    dbConnection.commitTransaction();
} catch (SQLException e) {
    dbConnection.rollbackTransaction();
    // Error handling
}
```

#### WorkoutDAO
**Purpose:** Workout and PR persistence

**Key Methods:**

**`saveStrengthWorkout(Workout workout)` / `saveCardioWorkout(Workout workout)`**
- Inserts workout with auto-generated ID
- Returns generated keys and sets workout ID

**`updateWorkout(Workout w)`**
- Polymorphic handling for Strength vs Cardio
- Different SQL for different workout types

**`recalculatePR(String rawName, String PRName, String type)`**
```java
Complex Logic:
1. Delete existing PR
2. Build query based on PR type:
   - Cardio: ORDER BY duration DESC
   - "(reps)": WHERE weight = 0 ORDER BY reps DESC
   - "(loaded)": WHERE weight > 0 ORDER BY weight DESC, reps DESC
   - Default: ORDER BY weight DESC, reps DESC
3. Fetch top workout matching criteria
4. Insert as new PR (or leave deleted if no workouts remain)
```

**`loadWorkouts()` / `loadPersonalRecords()`**
- Load all data on service initialization
- ORDER BY date DESC, id DESC (newest first)

**`performInitialSetup()`**
- Checks if activities/quotes tables are empty
- Loads from CSV files if needed
- Only runs once per database

#### UserDAO
**Purpose:** User profile and body metrics persistence

**Key Methods:**

**`saveUserProfile(User u)`**
```java
Smart INSERT vs UPDATE:
1. Check row count in user_profile
2. If 0 → INSERT new profile
3. If > 0 → UPDATE existing (single-user app)
4. Sets DEFAULT_USER_ID = 1
```

**`loadBodyMetrics()`**
- ORDER BY date DESC, id DESC
- Most recent entry is always index 0 in service layer

#### GoalDAO
**Purpose:** Goal persistence and progress queries

**Key Methods:**

**`updateGoalStatusBatch(List<Integer> goalIds, GoalStatus newStatus)`**
```java
Batch update for efficiency:
UPDATE goals SET status = ? WHERE id IN (?, ?, ...)
```

**Progress Query Methods:**
- `getMaxWeightLifted(exercise, startDate)` → MAX(weight_kg)
- `getMostRepsDone(exercise, startDate)` → MAX(reps)
- `getTotalMinutes(exercise, startDate)` → SUM(duration)
- `getWorkoutFrequency(exercise, startDate)` → COUNT(*)

---

## Data Models

### Workout Hierarchy

```
Workout (Abstract)
├── id: int
├── name: String
├── type: String
├── date: LocalDate
├── caloriesBurned: double
└── durationMinutes: int

StrengthWorkout extends Workout
├── setCount: int
├── repCount: int
├── bodyWeightKg: double
├── externalWeightKg: double
├── trainingVolumeKg: double
└── bodyWeightFactor: double

CardioWorkout extends Workout
└── distanceKm: double
```

### User Profile

```
User
├── name: String
├── age: int
├── heightCm: double
├── weightKg: double
├── sex: String
├── BMI: double
├── BMR: double
├── currentStreak: int
└── lastWorkoutDate: LocalDate
```

### Body Metric

```
BodyMetric
├── id: int
├── age: int
├── heightCm: double
├── weightKg: double
├── BMI: double
└── date: LocalDate
```

### Goal

```
Goal
├── id: int
├── goalTitle: String
├── exerciseName: String
├── startDate: LocalDate
├── endDate: LocalDate (nullable)
├── goalType: String
├── currentValue: double
├── targetValue: double
└── status: GoalStatus (ACTIVE, COMPLETED, EXPIRED)
```

### Personal Record

```
PersonalRecord
├── activityName: String (key)
├── duration: int
├── reps: int
├── weight: double
└── date: LocalDate
```

### Supporting Models

```
Activity
├── id: int
├── activityName: String
├── workoutType: String
├── category: String
├── metValue: double
└── bodyWeightFactor: double

Quote
├── level: String (harsh, firm, standard)
└── quote: String
```

---

## Database Schema

### Tables

**workouts**
```sql
CREATE TABLE workouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exercise_name TEXT NOT NULL,
    type TEXT NOT NULL,
    date DATE DEFAULT CURRENT_DATE,
    duration_minutes INTEGER,
    calories_burned REAL,
    distance_km REAL,
    sets INTEGER,
    reps INTEGER,
    weight_kg REAL,
    volume_kg REAL,
    bodyweight_factor REAL
)
```

**personal_records**
```sql
CREATE TABLE personal_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exercise_name TEXT UNIQUE NOT NULL,
    duration_minutes INTEGER,
    reps INTEGER,
    weight_kg REAL,
    date DATE DEFAULT CURRENT_DATE
)
```

**body_metrics**
```sql
CREATE TABLE body_metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    age INTEGER,
    height_cm REAL NOT NULL,
    weight_kg REAL NOT NULL,
    BMI REAL NOT NULL,
    date DATE DEFAULT CURRENT_DATE
)
```

**user_profile**
```sql
CREATE TABLE user_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    age INTEGER,
    height_cm REAL NOT NULL,
    weight_kg REAL NOT NULL,
    sex TEXT NOT NULL,
    BMI REAL NOT NULL,
    BMR REAL NOT NULL,
    current_streak INTEGER,
    last_workout_date DATE
)
```

**goals**
```sql
CREATE TABLE goals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    goal_title TEXT NOT NULL,
    exercise_name TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    goal_type TEXT NOT NULL,
    current_value DOUBLE NOT NULL,
    target_value DOUBLE NOT NULL,
    status TEXT NOT NULL
)
```

**activities**
```sql
CREATE TABLE activities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    activity_name TEXT NOT NULL,
    workout_type TEXT NOT NULL,
    category TEXT NOT NULL,
    met_value DOUBLE NOT NULL,
    bodyweight_factor DOUBLE NOT NULL
)
```

**quotes**
```sql
CREATE TABLE quotes (
    id INTEGER PRIMARY KEY,
    level TEXT NOT NULL,
    quote TEXT NOT NULL
)
```

---

## Business Logic

### Workout Logging Flow

```
1. User selects workout type (Strength/Cardio)
2. User selects exercise from categorized list
3. User enters workout details (sets, reps, weight, duration)
4. System calculates calories burned using MET values
5. WorkoutService.logWorkout() called:
   
   BEGIN TRANSACTION
   ├─ Save workout to database
   ├─ Check if new PR
   │  ├─ If yes: Update personal_records table
   │  └─ Display "NEW PR!" message
   ├─ Refresh all relevant goals
   │  ├─ Recalculate currentValue for each
   │  ├─ Check if newly completed
   │  └─ Batch update goal statuses
   └─ COMMIT TRANSACTION
   
   ├─ Add workout to in-memory list
   ├─ Update PR cache
   ├─ Archive completed goals
   └─ Recalculate workout streak
```

### PR Tracking Logic

#### Key Concepts
- **Bodyweight exercises** (pullups, dips): Tracked separately for reps-only vs weighted
- **Weighted exercises** (bench press, squats): Tracked by total volume (weight × reps)
- **Cardio exercises**: Tracked by duration

#### PR Keys
```
Bench Press 100kg × 5 reps → "Bench Press"
Pullups (0kg) × 15 reps → "Pullups (reps)"
Pullups (20kg) × 8 reps → "Pullups (loaded)"
Running 30 mins → "Running"
```

#### Update Logic
```java
if (workout is Strength) {
    if (bodyWeightFactor != 0 && externalWeight > 0) {
        key = name + " (loaded)"
        compare by weight first, then reps
    } else if (externalWeight == 0 && bodyWeightFactor != 0) {
        key = name + " (reps)"
        compare by reps only
    } else {
        key = name
        compare by weight first, then reps
    }
} else {
    key = name
    compare by duration
}
```

### Goal Progress Tracking

#### Goal Types
1. **weight loss** / **weight gain**: Current weight vs target
2. **weight lifted**: MAX(weight) for specific exercise since start date
3. **reps**: MAX(reps) for specific exercise since start date
4. **duration**: SUM(duration) for specific exercise since start date
5. **frequency**: COUNT(*) for specific exercise since start date

#### Automatic Evaluation Points
- After logging a workout
- After updating a workout
- After deleting a workout
- After updating body weight
- After updating body metrics
- On application startup (expiration check)

#### Status Transitions
```
ACTIVE → COMPLETED: When currentValue >= targetValue
ACTIVE → EXPIRED: When today > endDate
COMPLETED → ACTIVE: If workout deleted and currentValue drops below target
```

### Streak Calculation

```java
Algorithm:
1. Get all unique workout dates
2. Sort descending (newest first)
3. Start streak counter at 1 with most recent date
4. Iterate through consecutive dates:
   - Calculate days between current and previous
   - If gap == 1: increment streak
   - If gap > 1: break (end of streak)
5. Save streak and last_workout_date to user profile
```

**Edge Cases Handled:**
- Workout date updated → recalculate
- Workout deleted → recalculate
- Multiple workouts same day → count as 1 day
- Last workout > 1 day ago on startup → reset to 0

---

## API Reference

### WorkoutService

```java
// Workout Management
void logWorkout(Workout w)
boolean updateWorkout(Workout original, Workout updated)
boolean deleteWorkout(Workout w)

// Retrieval
List<Workout> getAllWorkouts()
List<Workout> getWeeklyWorkouts()
List<PersonalRecord> getAllPRs()

// Calculations
double computeTotalCalories(List<Workout> workouts)
double computeTotalTrainingVolumeKg(List<Workout> workouts)
static double calculateCaloriesBurned(double met, double weight, int duration)

// Metadata
List<String> getActivityNamesByCategory(String category)
double getMetForActivity(String activityName)
double getBodyWeightFactorForActivity(String activityName)

// Motivational
String getQuote()

// Personal Records
boolean deletePR(String prName)
```

### UserService

```java
// Profile Management
void saveUserProfile(String name, int age, double height, double weight, String sex)
boolean updateProfile(User updatedUser)
boolean correctProfileDetails(User updatedUser)
String showProfileDetails()

// Body Metrics
void addBodyMetric(BodyMetric bm)
boolean updateBodyMetric(BodyMetric original, BodyMetric updated)
boolean deleteBodyMetric(BodyMetric bm)
List<BodyMetric> getBodyMetricHistory()

// Calculations
double calculateBMI(double weight, double height)
double calculateBMR(double height, double weight, int age, String sex)

// Streak Management
void recalculateStreak(List<LocalDate> allWorkoutDates)
void validateStreakOnStartup()

// Getters
User getCurrentUser()
String getName()
int getAge()
double getWeightKg()
double getHeightCm()
String getSex()
double getBMI()
double getBMR()
int getStreak()
boolean isRegistered()
boolean hasHistory()
```

### GoalService

```java
// Goal Management
boolean createGoal(Goal g)
boolean updateGoal(Goal original, Goal updated)
boolean deleteGoal(Goal g)

// Retrieval
List<Goal> getAllGoals()
List<Goal> getActiveGoals()
int getGoalsSize()

// Progress Evaluation
List<Goal> refreshGoalsForWorkout(Workout w) throws SQLException
List<Goal> evaluateWeightGoals(double currentWeight) throws SQLException
boolean isGoalCompleted(double current, double target, String type)
double getCurrentValue(String goalType, String exercise, LocalDate startDate)

// Maintenance
void archiveCompletedGoals(List<Goal> completed) throws SQLException
List<Integer> getCompletedGoalsId(List<Goal> completedGoals)
```

---

## User Interface

### Menu Structure

```
Main Menu
├─ Daily Actions
│  ├─ [1] Log Workout
│  ├─ [2] Set a Goal
│  ├─ [3] View Goals
│  └─ [4] View Motivational Quote
├─ Data
│  ├─ [5] View Weight Progress
│  ├─ [6] View Weekly Summary
│  ├─ [7] View Personal Records
│  └─ [8] View All Workouts
├─ User Profile
│  ├─ [9] Update Weight
│  ├─ [10] Update Body Metrics
│  └─ [11] View Profile
└─ [0] Exit
```

### Workout Logging Workflow

```
Log Workout
├─ Select Type: Strength / Cardio / Cancel
│
├─ STRENGTH PATH
│  ├─ Select Body Part: Arms / Chest / Back / Legs / Core
│  ├─ Select Exercise from category list
│  ├─ Enter sets (0 to cancel)
│  ├─ Enter reps (0 to cancel)
│  ├─ Enter external weight (0 valid for bodyweight)
│  ├─ Enter duration (0 to cancel)
│  └─ Display success + "NEW PR!" if applicable
│
└─ CARDIO PATH
   ├─ Select Category: HIIT / Endurance / Sports & Recreation
   ├─ Select Exercise from category list
   ├─ Enter duration (0 to cancel)
   ├─ Enter distance (0 to skip/auto-calc) [optional]
   └─ Display success
```

### Goal Creation Workflow

```
Set a Goal
├─ Select Goal Type
│  ├─ [1] Target Body Weight
│  ├─ [2] Weight Lifted (Load)
│  ├─ [3] Rep Max
│  ├─ [4] Total Duration
│  └─ [5] Workout Frequency
│
├─ Enter goal title/description
├─ Select exercise (if applicable)
├─ Enter target value
├─ Enter start date (default: today)
├─ Enter end date (optional: leave blank for open-ended)
└─ Display success
```

### Pagination Interface

```
[ Page 1 of 3 ]

  id | Exercise Name    | Details...
────┼──────────────────┼────────────
  1 | Bench Press      | ...
  2 | Squats           | ...
  ...
 10 | Pull-ups         | ...
────┴──────────────────┴────────────

[ N ] Next Page    [ P ] Prev Page    [ Q ] Done Viewing

Enter choice: _
```

### CRUD Pattern (All List Views)

```
After displaying any list:

What would you like to do?

[ 1 ] Delete an entry    [ 2 ] Update an entry    [ 0 ] Back

Choice prompts for row ID selection
Validation prevents invalid indices
Success/failure messages displayed
```

---

## Setup & Configuration

### Prerequisites
- Java JDK 17 or higher
- SQLite JDBC Driver (in `lib/` directory)

### Setup Steps
1. **Compile the Project**
- Navigate to the project root directory and compile the Java files, including the SQLite library in the classpath.
```bash
javac -d bin -cp "lib/sqlite-jdbc-3.41.2.1.jar;src" src/Main.java
```

2. **Run the Application**
```bash
java -cp "bin;lib/sqlite-jdbc-3.41.2.1.jar" Main
```

3. **Database Initialization**
- On the first run, the DatabaseConnection class will automatically detect if data/HEATDatabase.db exists. If not, it will create the file and initialize all necessary tables (workouts, users, goals, etc.) automatically.

---

### Directory Structure
```
HEAT-Console/
├── data/
│   └── HEATDatabase.db (auto-created)
├── lib/
│   └── sqlite-jdbc-x.x.x.jar
├── src/
│   └── heat/
│       ├── dao/
│       │   ├── DatabaseConnection.java
│       │   ├── WorkoutDAO.java
│       │   ├── UserDAO.java
│       │   └── GoalDAO.java
│       ├── model/
│       │   ├── Workout.java
│       │   ├── StrengthWorkout.java
│       │   ├── CardioWorkout.java
│       │   ├── User.java
│       │   ├── BodyMetric.java
│       │   ├── Goal.java
│       │   ├── PersonalRecord.java
│       │   ├── Activity.java
│       │   └── Quote.java
│       ├── service/
│       │   ├── WorkoutService.java
│       │   ├── UserService.java
│       │   └── GoalService.java
│       ├── ui/
│       │   ├── ConsoleDashboard.java
│       │   └── InputHelper.java
│       ├── util/
│       │   └── ConsoleUtils.java
│       └── resources/
│           ├── activities.csv
│           └── quotes.csv
├── .gitignore
└── README.md
```

### Database Configuration

**Location:** `data/HEATDatabase.db`  
**Connection String:** `jdbc:sqlite:data/HEATDatabase.db`

**Auto-Initialization:**
- Database and tables created on first run
- Activities and quotes loaded from CSV on first run only
- Single connection maintained throughout app lifecycle

### Initial Setup Files

**activities.csv Format:**
```csv
activity_name,workout_type,category,met_value,bodyweight_factor
Bench Press,Strength,Chest,3.5,0.0
Pull-ups,Strength,Back,8.0,1.0
Running,Cardio,Endurance,9.8,0.0
```

**quotes.csv Format:**
```csv
level|quote
harsh|Stop making excuses and get moving!
firm|Consistency is key. Keep pushing.
standard|Great work! Stay dedicated.
```

---

## Development Guide

### Adding a New Workout Type

1. **Create Model Class**
```java
public class FlexibilityWorkout extends Workout {
    private int holdDurationSeconds;
    
    // Constructor, getters, setters
}
```

2. **Update WorkoutDAO**
```java
public void saveFlexibilityWorkout(Workout workout) {
    // SQL INSERT with flexibility-specific fields
}

// Update loadWorkouts() to handle new type
```

3. **Update WorkoutService**
```java
// Add to logWorkout() switch/if statement
if (w instanceof FlexibilityWorkout) {
    workoutDAO.saveFlexibilityWorkout(w);
}
```

4. **Update InputHelper**
```java
// Add menu option
// Create captureFlexibilityWorkout() method
// Update selectWorkoutType()
```

### Adding a New Goal Type

1. **Update GoalDAO queries**
```java
public int getNewMetric(String exerciseName, LocalDate startDate) {
    // Custom query for new goal type
}
```

2. **Update GoalService**
```java
// Add case to getCurrentValue()
else if (goalType.equals("new_type")) {
    return goalDAO.getNewMetric(exerciseName, startDate);
}

// Update isGoalCompleted() if needed
```

3. **Update InputHelper**
```java
// Add menu option in captureGoalInput()
// Handle new goal type selection
```

### Transaction Management Best Practices

**Always use this pattern:**
```java
try {
    dbConnection.beginTransaction();
    
    // Multiple DAO calls
    dao1.operation();
    dao2.operation();
    
    dbConnection.commitTransaction();
    
    // Update in-memory caches AFTER commit
    
} catch (SQLException e) {
    try {
        dbConnection.rollbackTransaction();
    } catch (SQLException ex) {
        // Log rollback failure
    }
    // Handle error, return false
}
```

###