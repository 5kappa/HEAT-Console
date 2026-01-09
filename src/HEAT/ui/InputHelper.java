package heat.ui;

import java.time.LocalDate;
import java.util.List;

import heat.model.*;
import heat.service.*;
import heat.util.ConsoleUtils;

public class InputHelper {

    private final WorkoutService workoutService;
    private final UserService userService;
    private final GoalService goalService;
    private final LocalDate today = LocalDate.now();

    public InputHelper(WorkoutService workoutService, UserService userService, GoalService goalService) {
        this.workoutService = workoutService;
        this.userService = userService;
        this.goalService = goalService;
    }

    // ============================================================
    // User Profile Management
    // ============================================================

    public void captureUserProfileInput() {
        String name = "";
        ConsoleUtils.printBorder();

        while (!(userService.isRegistered())) {
            ConsoleUtils.printCentered("[ User Registration ]");

            name = ConsoleUtils.readRequiredString(String.format("%27s", "Enter your name  :  "));
            int age = ConsoleUtils.readRequiredInt(String.format("%27s", "Enter your age  :  "), false);
            double height = ConsoleUtils.readRequiredDouble(String.format("%27s", "Enter your height (cm)  :  "), false);
            double weight = ConsoleUtils.readRequiredDouble(String.format("%27s", "Enter your weight (kg)  :  "), false);

            String sex = "";
            while (true) {
                sex = ConsoleUtils.readRequiredString(String.format("%27s", "Enter sex (M/F)  :  ")).toUpperCase();
                if (sex.equals("M") || sex.equals("F")) {
                    break;
                }
                System.out.println("\t\t\t\t\t[ ! ]   Invalid input. Please enter 'M' or 'F'.");
            }

            userService.saveUserProfile(name, age, height, weight, sex);

            double bmi = userService.calculateBMI(weight, height);

            BodyMetric bm = new BodyMetric(age, height, weight, bmi, today);
            userService.addBodyMetric(bm);
        }

        System.out.println("");
        ConsoleUtils.printCentered("User registration successful!");
        System.out.println("");

        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered(String.format("[ %s's User Profile ]", name));
        System.out.println("");
        ConsoleUtils.printCentered(String.format("Streak  :  %d", userService.getStreak()));
        System.out.println(userService.showProfileDetails());
    }

    public void showUserProfile() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered(String.format("[ %s's User Profile ]", userService.getName()));
            System.out.println("");
            ConsoleUtils.printCentered(String.format("Streak  :  %d", userService.getStreak()));
            
            System.out.println(userService.showProfileDetails());
            
            ConsoleUtils.printThinBorder();
            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t\t[ 1 ]   Edit Profile\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);

            switch (choice) {
                case 1:
                    handleEditProfile();
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please select from 0-1.");
            }

            System.out.println("");
        }
    }

    private void handleEditProfile() {
        ConsoleUtils.printCentered("[ Edit Profile ]");
        
        User current = userService.getCurrentUser();
        
        String newName = current.getName();
        String newSex = current.getSex();
        int newAge = current.getAge();
        double newHeight = current.getHeightCm();
        double newWeight = current.getWeightKg();

        int choice = -1;

        while (choice < 0 || choice > 6) {
            System.out.println("\n\t\t\t\t\tWhat would you like to correct?");
            System.out.println("\t\t\t\t\t[ 1 ]   Name (" + newName + ")");
            System.out.println("\t\t\t\t\t[ 2 ]   Sex (" + newSex + ")");
            System.out.println("\t\t\t\t\t[ 3 ]   Age (" + newAge + ")");
            System.out.println("\t\t\t\t\t[ 4 ]   Height (" + newHeight + " cm)");
            System.out.println("\t\t\t\t\t[ 5 ]   Weight (" + newWeight + " kg)");
            System.out.println("\t\t\t\t\t[ 6 ]   Edit All");
            System.out.println("\t\t\t\t\t[ 0 ]   Cancel");

            choice = ConsoleUtils.readRequiredInt("Select option: ", true);

            if (choice == 0) {
                return;
            } else if (choice > 6) {
                System.out.println("\t\t\t\t\tInvalid input. Please select from 0-6.");
            }
        }

        switch (choice) {
            case 1:
                newName = ConsoleUtils.readStringOrDefault("New Name", newName);
                break;
            case 2:
                while (true) {
                    newSex = ConsoleUtils.readStringOrDefault("New Sex (M/F)", newSex).toUpperCase();
                    if(newSex.equals("M") || newSex.equals("F")) break;
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid input. Enter M or F.");
                }
                break;
            case 3:
                newAge = ConsoleUtils.readIntOrDefault("New Age", newAge);
                break;
            case 4:
                newHeight = ConsoleUtils.readDoubleOrDefault("New Height", newHeight);
                break;
            case 5:
                newWeight = ConsoleUtils.readDoubleOrDefault("New Weight", newWeight);
                break;
            case 6:
                ConsoleUtils.printThinBorder();
                newName = ConsoleUtils.readStringOrDefault("New Name", newName);
                
                while (true) {
                    newSex = ConsoleUtils.readStringOrDefault("New Sex (M/F)", newSex).toUpperCase();
                    if(newSex.equals("M") || newSex.equals("F")) break;
                }
                
                newAge = ConsoleUtils.readIntOrDefault("New Age", newAge);
                newHeight = ConsoleUtils.readDoubleOrDefault("New Height", newHeight);
                newWeight = ConsoleUtils.readDoubleOrDefault("New Weight", newWeight);
                break;
        }
        
        double newBMI = userService.calculateBMI(newWeight, newHeight);
        double newBMR = userService.calculateBMR(newHeight, newWeight, newAge, newSex);

        User updatedUser = new User(
            newName,
            newAge,
            newHeight,
            newWeight,
            newSex,
            newBMI,
            newBMR,
            current.getCurrentStreak(),
            current.getLastWorkoutDate()
        );

        if (userService.correctProfileDetails(updatedUser)) { 
            System.out.println("\t\t\t\t\tProfile updated successfully!");
        } else {
            System.out.println("\t\t\t\t\tUpdate failed.");
        }
    }

    // ============================================================
    // Workout Logging (Create)
    // ============================================================

    public void captureWorkoutInput() {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Workout Logging ]");

        String workoutType = selectWorkoutType();
        if (workoutType == null) {
            return;
        }

        if (workoutType.equalsIgnoreCase("Strength")) {
            captureStrengthWorkout();
        } else if (workoutType.equalsIgnoreCase("Cardio")) {
            captureCardioWorkout();
        } else {
            System.out.println("\t\t\t\t\tInvalid workout type. Returning to main menu.");
            return;
        }
    }

    private String selectWorkoutType() {
        System.out.println("\t\t\t\t\tSelect a workout type.");
        System.out.println("\t\t\t\t\t[ 1 ]   Strength\n\t\t\t\t\t[ 2 ]   Cardio\n");
        System.out.println("\t\t\t\t\t[ 0 ]   Cancel\n");

        String workoutType = null;
        while (workoutType == null) {
            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            switch (choice) {
                case 1 -> workoutType = "Strength";
                case 2 -> workoutType = "Cardio";
                case 0 -> { 
                    return null; 
                }
                default -> System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please enter 1-2.");
            }
        }

        return workoutType;
    }

    private void captureStrengthWorkout() {
        String workoutType = "Strength";
        StrengthWorkout sw = null;

        String selectedExerciseName = selectStrengthExerciseName();
        if (selectedExerciseName == null) { return; }

        int sets = ConsoleUtils.readRequiredInt("Number of sets (0 to cancel): ", true);
        if (sets == 0) { return; }

        int reps = ConsoleUtils.readRequiredInt("Number of reps per set (0 to cancel): ", true);
        if (reps == 0) { return; }

        double bodyWeightFactor = workoutService.getBodyWeightFactorForActivity(selectedExerciseName);

        double externalWeightKg = -1;
        while (externalWeightKg == -1) {
            if (bodyWeightFactor == 0) {
                double val = ConsoleUtils.readRequiredDouble("Weight used (kg) (0 to cancel): ", true);
                if (val == 0) { return; }
                externalWeightKg = val;
            } else {
                // Bodyweight: 0 is valid (unweighted)
                externalWeightKg = ConsoleUtils.readRequiredDouble("Weight used (kg): ", true);
            }
        }

        int duration = ConsoleUtils.readRequiredInt("Duration (mins) (0 to cancel): ", true);
        System.out.println("");
        if (duration == 0) { return; }

        double caloriesBurned = WorkoutService.calculateCaloriesBurned(workoutService.getMetForActivity(selectedExerciseName), userService.getWeightKg(), duration);

        sw = new StrengthWorkout(selectedExerciseName, workoutType, today, caloriesBurned, duration,
            sets, reps, userService.getWeightKg(), externalWeightKg, bodyWeightFactor);

        if (sw != null) {
            workoutService.logWorkout(sw);

            System.out.println("");
            ConsoleUtils.printThinBorder();
            ConsoleUtils.printCentered("Workout logged successfully!");
            System.out.println("");
        }
    }

    private void captureCardioWorkout() {
        String workoutType = "Cardio";
        CardioWorkout cw = null;

        String selectedExerciseName = selectCardioExerciseName();
        if (selectedExerciseName == null) { return; }

        System.out.println("");
        int duration = ConsoleUtils.readRequiredInt("Enter duration (mins) (0 to cancel): ", true);
        System.out.println("");

        if (duration == 0) { return; }

        double caloriesBurned = WorkoutService.calculateCaloriesBurned(workoutService.getMetForActivity(selectedExerciseName), userService.getWeightKg(), duration);

        double distanceKm = 0.0;
        boolean distanceProvided = false;

        if (isDistanceActivity(selectedExerciseName)) {
            distanceKm = ConsoleUtils.readRequiredDouble("Enter distance (km) (0 to skip/auto-calc): ", true);
            if (distanceKm > 0) {
                distanceProvided = true;
            }
        }

        if (distanceProvided) {
            cw = new CardioWorkout(0, selectedExerciseName, workoutType, today, caloriesBurned, duration, distanceKm);
        } else {
            cw = new CardioWorkout(selectedExerciseName, workoutType, today, caloriesBurned, duration);
        }

        if (cw != null) {
            workoutService.logWorkout(cw);

            System.out.println("");
            ConsoleUtils.printThinBorder();
            ConsoleUtils.printCentered("Workout logged successfully!");
            System.out.println("");
        }
    }

    // Helper method to decide if we should ask for distance
    private boolean isDistanceActivity(String name) {
        if (name == null) return false;
        name = name.toLowerCase();
        return name.contains("running") || 
               name.contains("cycling") || 
               name.contains("swimming") || 
               name.contains("walking");
    }

    private String selectStrengthExerciseName() {
        System.out.println("\n\t\t\t\t\tSelect Body Part Trained: ");
        System.out.println("\t\t\t\t\t[ 1 ]   Arms");
        System.out.println("\t\t\t\t\t[ 2 ]   Chest");
        System.out.println("\t\t\t\t\t[ 3 ]   Back");
        System.out.println("\t\t\t\t\t[ 4 ]   Legs");
        System.out.println("\t\t\t\t\t[ 5 ]   Core\n");
        System.out.println("\t\t\t\t\t[ 0 ]   Cancel\n");
        
        String category = null;

        while (category == null) {  
            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            switch (choice) {
                case 1 -> category = "Arms";
                case 2 -> category = "Chest";
                case 3 -> category = "Back";
                case 4 -> category = "Legs";
                case 5 -> category = "Core";
                case 0 -> { 
                    System.out.println("");
                    return null; 
                }
                default -> System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please enter 1-5.");
            }
        }

        List<String> availableActivities = workoutService.getActivityNamesByCategory(category);

        if (availableActivities == null) {
            System.out.println("\t\t\t\t\t[ ! ]   No exercises found for " + category + ".");
            return null;
        }

        System.out.println("");
        ConsoleUtils.printBorder();

        ConsoleUtils.printCentered(String.format("[ %s EXERCISES ]", category.toUpperCase()));

        for (int i = 0; i < availableActivities.size(); i++) {
            System.out.println("\t\t\t\t\t[ " + (i + 1) + " ]   " + availableActivities.get(i));
        }
        System.out.println("\n\t\t\t\t\t[ 0 ]   Cancel");

        int exerciseIndex = -1;
        while (true) {
            System.out.println("");
            exerciseIndex = ConsoleUtils.readRequiredInt("Select exercise number: ", true);

            if (exerciseIndex == 0) {
                return null;
            }

            if (exerciseIndex >= 1 && exerciseIndex <= availableActivities.size()) {
                break;
            }

            System.out.println("\t\t\t\t\tInvalid selection. Please pick a number from 1-" + availableActivities.size() + ".");
        }

        return availableActivities.get(exerciseIndex - 1);
    }

    private String selectCardioExerciseName() {
        System.out.println("\n\t\t\t\t\tSelect the type of cardio activity. ");
        System.out.println("\t\t\t\t\t[ 1 ]   High-Intensity Interval Training (HIIT)");
        System.out.println("\t\t\t\t\t[ 2 ]   Endurance");
        System.out.println("\t\t\t\t\t[ 3 ]   Sports & Recreation\n");
        System.out.println("\t\t\t\t\t[ 0 ]   Cancel");

        String category = null;

        while (category == null) {
            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            switch (choice) {
                case 1 -> category = "HIIT";
                case 2 -> category = "Endurance";
                case 3 -> category = "Recreational";
                case 0 -> { 
                    return null; 
                }
                default -> System.out.println("\t\t\t\t\tInvalid choice. Please enter 1-3.");
            }
        }

        List<String> availableExercises = workoutService.getActivityNamesByCategory(category);

        if (availableExercises == null) {
            System.out.println("\t\t\t\t\tNo exercises found for " + category + ".");
            return null;
        }

        System.out.println();
        ConsoleUtils.printBorder();

        ConsoleUtils.printCentered(String.format("[ %s EXERCISES ]", category.toUpperCase()));
        for (int i = 0; i < availableExercises.size(); i++) {
            System.out.println("\t\t\t\t\t[ " + (i + 1) + " ]   " + availableExercises.get(i));
        }
        System.out.println("\n\t\t\t\t\t[ 0 ]   Cancel");

        System.out.println("");
        int exerciseIndex = -1;
        while (true) {
            exerciseIndex = ConsoleUtils.readRequiredInt("Select exercise number: ", true);

            if (exerciseIndex == 0) {
                return null;
            }

            if (exerciseIndex >= 1 && exerciseIndex <= availableExercises.size()) {
                break;
            }
            System.out.println("\t\t\t\t\tInvalid selection. Please pick a number from the list.");
        }

        return availableExercises.get(exerciseIndex - 1);
    }

    // ============================================================
    // Workout Viewing & Editing (Read/Update/Delete)
    // ============================================================

    public void showAllWorkouts() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            List<Workout> workoutsList = workoutService.getAllWorkouts();

            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered("[ All Workouts ]");
            System.out.println("");

            printWorkouts(workoutsList);
            if (workoutsList.isEmpty()) return;

            System.out.println("\t\t\t\t\tTotal Workouts: " + workoutsList.size());
            System.out.printf("\t\t\t\t\tTotal Training Volume: %.2f kg\n", workoutService.computeTotalTrainingVolumeKg(workoutsList));
            System.out.println("\t\t\t\t\tTotal Calories Burned: " + (int)workoutService.computeTotalCalories(workoutsList) + "\n");
            ConsoleUtils.printBorder();

            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t[ 1 ]   Delete a Workout\t\t[ 2 ]   Update a workout\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1:
                    deleteWorkout(workoutsList);
                    break;
                case 2:
                    handleEditWorkout();
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please choose 0, 1, or 2.");
            }
        }
    }

    public void showWeeklySummary() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            List<Workout> workoutsList = workoutService.getWeeklyWorkouts();

            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered(String.format("[ Weekly Summary for %s ]", userService.getName()));
            System.out.println("");

            printWorkouts(workoutsList);
            if (workoutsList.isEmpty()) return;

            System.out.println("\t\t\t\t\tTotal Workouts: " + workoutsList.size());
            System.out.printf("\t\t\t\t\tTotal Training Volume: %.2f kg\n", workoutService.computeTotalTrainingVolumeKg(workoutsList));
            System.out.println("\t\t\t\t\tTotal Calories Burned: " + (int)workoutService.computeTotalCalories(workoutsList) + "\n");
            ConsoleUtils.printThinBorder();

            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t\t[ 1 ]   Delete a Workout\t\t[ 2 ]   Update a workout\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);

            switch (choice) {
                case 1:
                    deleteWorkout(workoutsList);
                    break;
                case 2:
                    handleEditWorkout();
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please choose 0, 1, or 2.");
            }

            System.out.println("");
        }
    }

    private void deleteWorkout(List<Workout> workoutsList) {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Delete a Workout ]");
        System.out.println("");

        printWorkouts(workoutsList); 

        int maxIndex = workoutsList.size();
        int choice = -1;

        while (choice < 0 || choice > maxIndex) {
            choice = ConsoleUtils.readRequiredInt("Enter row ID to delete (0 to cancel): ", true);
            
            if (choice > 0 && choice <= maxIndex) {
                Workout targetWorkout = workoutsList.get(choice - 1);
                String workoutName = targetWorkout.getName();

                if (workoutService.deleteWorkout(targetWorkout)) {
                    System.out.println("\t\t\t\t\tSuccessfully deleted " + workoutName + "!\n");

                    return;
                }
                
            } else if (choice == 0) {
                return;
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Invalid index. Choose 0-" + maxIndex + ".");
            }
        }
    }

    private void handleEditWorkout() {
        ConsoleUtils.printCentered("[ Edit a Workout ]");
        System.out.println("");
        
        List<Workout> history = workoutService.getAllWorkouts(); 
        if (history.isEmpty()) {
            System.out.println("\t\t\t\t\tNo workouts to edit.");
            return;
        }

        printWorkouts(history); 

        int choice = -1;
        while (choice < 0 || choice > history.size()) {
            choice = ConsoleUtils.readRequiredInt("Enter ID number to edit (0 to cancel): ", true);
            System.out.println("");

            if (choice == 0) {
                return;
            } else if (choice > history.size()) {
                System.out.println("Invalid input. Please select from 0-" + history.size() + ".");
            }
        }

        Workout original = history.get(choice - 1);

        LocalDate newDate = original.getDate();
        int newDuration = original.getDurationMinutes();

        int newSets = 0;
        int newReps = 0;
        double newWeight = 0.0;

        double currentDistance = 0.0;

        if (original instanceof StrengthWorkout) {
            StrengthWorkout sw = (StrengthWorkout) original;
            newSets = sw.getSetCount();
            newReps = sw.getRepCount();
            newWeight = sw.getExternalWeightKg();
        } else if (original instanceof CardioWorkout) {
            CardioWorkout cw = (CardioWorkout) original;
            currentDistance = cw.getDistanceKm();
        }

        System.out.println("\n\t\t\t\t\tWhat would you like to update?");
        System.out.println("\t\t\t\t\t[1]   Date (" + newDate + ")");
        System.out.println("\t\t\t\t\t[2]   Duration (" + newDuration + " min)");
        
        if (original instanceof StrengthWorkout) {
            System.out.println("\t\t\t\t\t[3]   Sets (" + newSets + ")");
            System.out.println("\t\t\t\t\t[4]   Reps (" + newReps + ")");
            System.out.println("\t\t\t\t\t[5]   Weight (" + newWeight + " kg)");
            System.out.println("\t\t\t\t\t[6]   Edit All Fields");
        } else {
            System.out.println("\t\t\t\t\t[3]   Edit All Fields"); 
        }
        
        System.out.println("\n\t\t\t\t\t[0]   Cancel\n");

        int fieldChoice = ConsoleUtils.readRequiredInt("Select option: ", true);

        if (fieldChoice == 0) {
            return;
        }

        switch (fieldChoice) {
            case 1:
                while (true) {
                    newDate = ConsoleUtils.readDateOrDefault("New Date", newDate);
                    if (newDate.isAfter(LocalDate.now())) {
                        System.out.println("\t\t\t\t\t[ ! ]   Date cannot be in the future.");
                    } else {
                        break;
                    }
                }
                break;
                
            case 2:
                newDuration = ConsoleUtils.readIntOrDefault("New Duration", newDuration);
                break;
                
            case 3: 
                if (original instanceof StrengthWorkout) {
                    newSets = ConsoleUtils.readIntOrDefault("New Sets", newSets);
                } else {
                    ConsoleUtils.printThinBorder();
                    newDate = ConsoleUtils.readDateOrDefault("New Date", newDate);
                    newDuration = ConsoleUtils.readIntOrDefault("New Duration", newDuration);
                }
                break;
                
            case 4:
                if (original instanceof StrengthWorkout) {
                    newReps = ConsoleUtils.readIntOrDefault("New Reps", newReps);
                } else {
                    System.out.println("\t\t\t\t\tInvalid selection.");
                    return;
                }
                break;
                
            case 5:
                if (original instanceof StrengthWorkout) {
                    newWeight = ConsoleUtils.readDoubleOrDefault("New Weight", newWeight);
                } else {
                    System.out.println("\t\t\t\t\tInvalid selection.");
                    return;
                }
                break;
                
            case 6:
                if (original instanceof StrengthWorkout) {
                    ConsoleUtils.printThinBorder();
                    
                    while (true) {
                        newDate = ConsoleUtils.readDateOrDefault("New Date", newDate);
                        if (newDate.isAfter(LocalDate.now())) {
                            System.out.println("\t\t\t\t\t[!] Date cannot be in the future.");
                        } else {
                            break;
                        }
                    }

                    newDuration = ConsoleUtils.readIntOrDefault("New Duration", newDuration);
                    newSets = ConsoleUtils.readIntOrDefault("New Sets", newSets);
                    newReps = ConsoleUtils.readIntOrDefault("New Reps", newReps);
                    newWeight = ConsoleUtils.readDoubleOrDefault("New Weight", newWeight);
                }
                break;
                
            default:
                System.out.println("\t\t\t\t\tInvalid selection.");
                return;
        }

        Workout updated = null;

        double newCalories = WorkoutService.calculateCaloriesBurned(
            workoutService.getMetForActivity(original.getName()), 
            userService.getWeightKg(), 
            newDuration
        );

        if (original instanceof StrengthWorkout) {
            updated = new StrengthWorkout(
                original.getId(), 
                original.getName(),           
                "Strength",
                newDate,
                newCalories,     
                newDuration,
                newSets,
                newReps,
                newWeight,
                (newSets * newReps * newWeight),
                ((StrengthWorkout) original).getBodyWeightFactor()
            );
        } else {
            updated = new CardioWorkout(
                original.getId(), 
                original.getName(),         
                "Cardio",
                newDate,
                newCalories,
                newDuration,
                currentDistance 
            );
        }

        System.out.println("");
        ConsoleUtils.printThinBorder();

        if (workoutService.updateWorkout(original, updated)) {
            System.out.println("\t\t\t\t\tUpdate successful!\n");
        } else {
            System.out.println("\t\t\t\t\t[ ! ]   Update failed.");
        }
    }

    public void printWorkouts(List<Workout> workoutList) {
        if (workoutList.isEmpty()) {
            System.out.println("\t\t\t\t\tNo workouts logged yet.\n");
            return;
        }

        int totalItems = workoutList.size();
        
        String tableHeader = String.format("   %-2s | %-39s | %-21s | %-41s |%-13s |%-13s | %-11s",
            "id",
            "   Exercise Name",
            "   Reps / Distance",
            "   Training Volume & Weight Used",
            "   Duration",
            "   Calories",
            " Date"
        );

        if (totalItems <= 10) {
            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();

            int i = 1;
            for (Workout w : workoutList) {
                String space = i < 10 ? "    " : "   ";
                System.out.println(space + i + w);
                i++;
            }
            ConsoleUtils.printThinBorder();

            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = 1;
        boolean viewing = true;

        while (viewing) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);

            ConsoleUtils.printCentered("[ Page " + currentPage + " of " + totalPages + " ]");
            System.out.println("");

            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();

            for (int i = start; i < end; i++) {
                String space = i < 9 ? "    " : "   ";
                System.out.println(space + (i + 1) + workoutList.get(i));
            }
            
            ConsoleUtils.printThinBorder();

            System.out.println("\t\t\t\t\t[ N ]   Next Page\t\t[ P ]   Prev Page\t\t[ Q ]   Done Viewing\n");
            String choice = ConsoleUtils.readRequiredString("Enter choice: ").toUpperCase();
            System.out.println("");

            switch (choice) {
                case "N":
                    if (currentPage < totalPages) {
                        ConsoleUtils.printBorder();
                        currentPage++;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the last page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "P":
                    if (currentPage > 1) {
                        ConsoleUtils.printBorder();
                        currentPage--;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the first page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "Q":
                    viewing = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice.\n");
                    ConsoleUtils.printBorder();
            }
        }
    }

    // ============================================================
    // Personal Records (Read/Delete)
    // ============================================================

    public void showPersonalRecords() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            List<PersonalRecord> prList = workoutService.getAllPRs();

            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered("[ Personal Records ]");
            System.out.println("");

            printAllPRs(prList);
            if (prList.isEmpty()) return;

            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t\t[ 1 ]   Delete a PR\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1:
                    deletePR(); 
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please choose 0 or 1.");
            }
        }
    }

    private void deletePR() {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Delete a PR ]");
        System.out.println("");

        List<PersonalRecord> currentPRs = workoutService.getAllPRs();

        int maxIndex = currentPRs.size();
        int choice = -1;

        while (choice < 0 || choice > maxIndex) {
            printAllPRs(currentPRs); 

            choice = ConsoleUtils.readRequiredInt("Enter row ID to delete (0 to cancel): ", true);
            
            if (choice > 0 && choice <= maxIndex) {
                PersonalRecord targetPR = currentPRs.get(choice - 1);
                String PRName = targetPR.getActivityName();

                if (workoutService.deletePR(PRName)) {
                    System.out.println("\t\t\t\t\tSuccessfully deleted " + PRName + "!\n");

                    currentPRs.remove(choice - 1);
                    
                    return;
                }
                
            } else if (choice == 0) {
                return;
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Invalid index. Choose 0-" + maxIndex + ".");
            }
        }
    }

    public void printAllPRs(List<PersonalRecord> prList) {
        if (prList.isEmpty()) {
            System.out.println("\t\t\t\t\tNo PRs found.\n");
            return;
        }

        int totalItems = prList.size();
        
        String tableHeader = String.format("   %-2s |   %-60s   |   %-37s   |   %-37s",
            "id",
            "Exercise Name", 
            "Personal Record", 
            "Date Achieved"
        );

        if (totalItems <= 10) {
            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();

            int i = 1;
            for (PersonalRecord pr : prList) {
                String space = i < 10 ? "    " : "   ";
                System.out.println(space + i + pr);
                i++;
            }
            ConsoleUtils.printThinBorder();

            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = 1;
        boolean viewing = true;

        while (viewing) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);

            ConsoleUtils.printCentered("[ Page " + currentPage + " of " + totalPages + " ]");
            System.out.println("");

            for (int i = start; i < end; i++) {
                String space = i < 9 ? "    " : "   ";
                System.out.println(space + (i + 1) + prList.get(i));
            }
            
            ConsoleUtils.printThinBorder();

            System.out.println("\t\t\t\t\t[ N ]   Next Page\t\t[ P ]   Prev Page\t\t[ Q ]   Done Viewing\n");
            String choice = ConsoleUtils.readRequiredString("Enter choice: ").toUpperCase();
            System.out.println("");

            switch (choice) {
                case "N":
                    if (currentPage < totalPages) {
                        ConsoleUtils.printBorder();
                        currentPage++;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the last page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "P":
                    if (currentPage > 1) {
                        ConsoleUtils.printBorder();
                        currentPage--;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the first page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "Q":
                    viewing = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice.\n");
                    ConsoleUtils.printBorder();
            }
        }
    }

    // ============================================================
    // Goal Management (Create/Read/Update/Delete)
    // ============================================================

    public void captureGoalInput() {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Set a Goal ]");
        System.out.println("");

        String goalType = null;

        while (goalType == null) {
            System.out.println("\t\t\t\t\tSelect a goal type.");
            System.out.println("\t\t\t\t\t[ 1 ]   Body: Target Body Weight");
            System.out.println("\t\t\t\t\t[ 2 ]   Strength: Weight Lifted (Load)");
            System.out.println("\t\t\t\t\t[ 3 ]   Strength: Rep Max");
            System.out.println("\t\t\t\t\t[ 4 ]   Cardio: Total Duration");
            System.out.println("\t\t\t\t\t[ 5 ]   General: Workout Frequency");
            System.out.println("\n\t\t\t\t\t[ 0 ]   Cancel\n");
            
            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            
            switch (choice) {
                case 1 -> goalType = "bodyweight";
                case 2 -> goalType = "weight lifted";
                case 3 -> goalType = "reps";
                case 4 -> goalType = "duration";
                case 5 -> goalType = "frequency";
                case 0 -> {
                    return;
                }
                default -> System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please enter 1-5.");
            }
        }

        String goalTitle = ConsoleUtils.readRequiredString("Enter a short title/description for your goal: ");
        String exerciseName;

        if (goalType.equals("weight lifted") || goalType.equals("reps")) {
            exerciseName = selectStrengthExerciseName();
            if (exerciseName == null) { return; }
        } else if (goalType.equals("duration")) {
            exerciseName = selectCardioExerciseName();
            if (exerciseName == null) { return; }
        } else if (goalType.equals("frequency")) {
            String type = selectWorkoutType();
            if (type == null) { return; }

            if (type.equalsIgnoreCase("Strength")) {
                exerciseName = selectStrengthExerciseName();
                if (exerciseName == null) { return; }
            } else {
                exerciseName = selectCardioExerciseName();
                if (exerciseName == null) { return; }
            }
        } else {
            exerciseName = null;
        }

        double targetValue = 0.0;

        if (goalType.equals("bodyweight")) {
            while (targetValue == 0.0) {
                double targetBodyweight = ConsoleUtils.readRequiredDouble("Enter target bodyweight (0 to cancel): ", true);
                if (targetBodyweight == 0) { return; }

                if (goalType.equals("bodyweight") && targetBodyweight < userService.getWeightKg()) {
                    goalType = "weight loss";
                    targetValue = targetBodyweight;
                } else if (goalType.equals("bodyweight") && targetBodyweight > userService.getWeightKg()) {
                    goalType = "weight gain";
                    targetValue = targetBodyweight;
                } else {
                    System.out.println("\t\t\t\t\t[ ! ]   Target weight cannot be equal to current weight. Please try again.");
                }
            }
        } else {
            targetValue = ConsoleUtils.readRequiredDouble("Enter target " + goalType + " (0 to cancel): ", true);
            if (targetValue == 0) { return; }
        }

        LocalDate startDate = null;
        LocalDate endDate = null;
        boolean areDatesValid = false;

        while (!areDatesValid) {
            startDate = ConsoleUtils.readRequiredLocalDate("Enter start date", false);
            endDate = ConsoleUtils.readRequiredLocalDate("Enter target end date (leave blank for open-ended)", true);

            if (endDate != null && startDate.isAfter(endDate)) {
                areDatesValid = false;
                System.out.println("\t\t\t\t\t[ ! ]   End date must be after start date.");
            } else {
                areDatesValid = true;
                break;
            }
        }

        System.out.println("");

        double currentValue = goalService.getCurrentValue(goalType, exerciseName, startDate);

        Goal g = new Goal(goalTitle, exerciseName, startDate, endDate, goalType, currentValue, targetValue);
        
        if (goalService.createGoal(g)) {    
            ConsoleUtils.printThinBorder();
            ConsoleUtils.printCentered("Goal created successfully!");
            System.out.println("");
        }
    }

    public void showGoalsMenu() {
        while (true) {
            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered("[ Goals ]");

            System.out.println("\n\t\t\t\t\t[ 1 ]   View active goals\t\t[ 2 ]   View all goals\t\t[ 0 ]   Back\n");
            
            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1:
                    showActiveGoals();
                    break;
                case 2:
                    showAllGoals();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice.");
            }
        }
    }

    private void showAllGoals() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            List<Goal> allGoals = goalService.getAllGoals();

            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered("[ Goals ]");
            System.out.println("");

            printGoals(allGoals);
            if (allGoals.isEmpty()) return;

            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t\t[ 1 ]   Delete a goal\t\t[ 2 ]   Update a goal\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1:
                    deleteGoal(allGoals);
                    break;
                case 2:
                    handleEditGoal(allGoals);
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please choose 0, 1, or 2.");
            }
        }
    }

    private void showActiveGoals() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            List<Goal> activeGoals = goalService.getActiveGoals();

            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered("[ Active Goals ]");
            System.out.println("");

            printGoals(activeGoals);
            if (activeGoals.isEmpty()) return;

            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t\t[ 1 ]   Delete a goal\t\t[ 2 ]   Update a goal\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1:
                    deleteGoal(activeGoals);
                    break;
                case 2:
                    handleEditGoal(activeGoals);
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice. Please choose 0, 1, or 2.");
            }
        }
    }

    private void handleEditGoal(List<Goal> goalsList) {
        ConsoleUtils.printCentered("[ Edit a Goal ]");
        System.out.println("");

        if (goalsList.isEmpty()) {
            System.out.println("\t\t\t\t\tNo goals found to edit.");
            return;
        }

        printGoals(goalsList);

        int choice = -1;
        while (choice < 0 || choice > goalsList.size()) {
            choice = ConsoleUtils.readRequiredInt("Enter row ID to edit (0 to cancel): ", true);
            System.out.println("");

            if (choice == 0) {
                return;
            } else if (choice > goalsList.size()) {
                System.out.println("Invalid input. Please select from 0-" + goalsList.size() + ".");
            }
        }

        Goal original = goalsList.get(choice - 1);

        String newTitle = original.getGoalTitle();
        double newTarget = original.getTargetValue();
        LocalDate newEndDate = original.getEndDate();
        
        String dateDisplay = (newEndDate == null) ? "Open-ended" : newEndDate.toString();

        System.out.println("\n\t\t\t\t\tWhat would you like to update?");
        System.out.println("\t\t\t\t\t[1]   Title (" + newTitle + ")");
        System.out.println("\t\t\t\t\t[2]   Target Value (" + newTarget + ")");
        System.out.println("\t\t\t\t\t[3]   End Date (" + dateDisplay + ")");
        System.out.println("\t\t\t\t\t[4]   Edit All Fields");
        System.out.println("\t\t\t\t\t[0]   Cancel");

        int fieldChoice = ConsoleUtils.readRequiredInt("Select option: ", true);

        if (fieldChoice == 0) {
            return;
        }

        switch (fieldChoice) {
            case 1:
                newTitle = ConsoleUtils.readStringOrDefault("New Title", newTitle);
                break;
            case 2:
                newTarget = ConsoleUtils.readDoubleOrDefault("New Target", newTarget);
                break;
            case 3:
                newEndDate = ConsoleUtils.readDateOrNull("New End Date", newEndDate);
                break;
            case 4:
                ConsoleUtils.printThinBorder();
                newTitle = ConsoleUtils.readStringOrDefault("New Title", newTitle);
                newTarget = ConsoleUtils.readDoubleOrDefault("New Target", newTarget);
                newEndDate = ConsoleUtils.readDateOrNull("New End Date", newEndDate);
                break;
            default:
                System.out.println("\t\t\t\t\tInvalid selection.");
                return;
        }

        Goal updated = new Goal(
            original.getId(),
            newTitle,
            original.getExerciseName(),
            original.getStartDate(),
            newEndDate,
            original.getGoalType(),
            original.getCurrentValue(),
            newTarget,
            original.getStatus() 
        );

        System.out.println("");
        ConsoleUtils.printThinBorder();

        if (goalService.updateGoal(original, updated)) {
            System.out.println("\t\t\t\t\tGoal updated successfully!\n");
        } else {
            System.out.println("\t\t\t\t\tUpdate failed.");
        }
    }

    private void deleteGoal(List<Goal> goalsList) {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Delete a Goal ]");
        System.out.println("");

        int choice = -1;
        int maxIndex = goalService.getGoalsSize();

        while (choice < 0 || choice > maxIndex) {

            printGoals(goalsList);

            choice = ConsoleUtils.readRequiredInt("Enter the row ID of the goal you'd like to delete (0 to go back): ", true);
            
            if (choice > 0 && choice <= maxIndex) {
                Goal goalToDelete = goalsList.get(choice - 1);
                String goalTitle = goalToDelete.getGoalTitle();

                if (goalService.deleteGoal(goalToDelete)) {
                    System.out.println("\t\t\t\t\tSuccessfully deleted goal: " + goalTitle + "!\n");

                    return;
                }
            } else if (choice == 0) {
                return;
            } else {
                System.out.println("\t\t\t\t\tInvalid row index. Please choose between 0-" + maxIndex + ".");
            }
        }
    }

    public void printGoals(List<Goal> goalsList) {
        if (goalsList.isEmpty()) {
            System.out.println("\t\t\t\t\tNo goals set yet.");
            return;
        }

        int totalItems = goalsList.size();
        
        String tableHeader = String.format("   %-2s |  %-31s  |  %-22s  |  %-20s  |  %-20s  | %-10s | %-10s | %-9s",
            "id",
            "Goal Title / Description",
            "Exercise Name",
            "Progress",
            "Target",
            "Start Date",
            "End Date",
            "Status"
        );

        if (totalItems <= 10) {
            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();


            int i = 1;
            for (Goal g : goalsList) {
                String space = i < 10 ? "    " : "   ";
                System.out.println(space + i + g);
                i++;
            }
            ConsoleUtils.printThinBorder();

            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = 1;
        boolean viewing = true;

        while (viewing) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);

            ConsoleUtils.printCentered("[ Page " + currentPage + " of " + totalPages + " ]");
            System.out.println("");

            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();

            for (int i = start; i < end; i++) {
                String space = i < 9 ? "    " : "   ";
                System.out.println(space + (i + 1) + goalsList.get(i));
            }
            
            ConsoleUtils.printThinBorder();

            System.out.println("\t\t\t\t\t[ N ]   Next Page\t\t[ P ]   Prev Page\t\t[ Q ]   Done Viewing\n");
            String choice = ConsoleUtils.readRequiredString("Enter choice: ").toUpperCase();
            System.out.println("");

            switch (choice) {
                case "N":
                    if (currentPage < totalPages) {
                        ConsoleUtils.printBorder();
                        currentPage++;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the last page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "P":
                    if (currentPage > 1) {
                        ConsoleUtils.printBorder();
                        currentPage--;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the first page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "Q":
                    viewing = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice.\n");
                    ConsoleUtils.printBorder();
            }
        }
    }

    // ============================================================
    // Body Metrics & Weight (Progress Tracking)
    // ============================================================

    public void updateWeight() {
        double newWeight = askForWeight();
        if (newWeight == 0) {
            return;
        }

        User current = userService.getCurrentUser();
        
        double newBMI = userService.calculateBMI(newWeight, current.getHeightCm());
        double newBMR = userService.calculateBMR(current.getHeightCm(), newWeight, current.getAge(), current.getSex());

        User updatedUser = new User(
            current.getName(),
            current.getAge(),
            current.getHeightCm(),
            newWeight,
            current.getSex(),
            newBMI,
            newBMR,
            current.getCurrentStreak(),
            current.getLastWorkoutDate()
        );
        userService.updateProfile(updatedUser);
        
        BodyMetric bm = new BodyMetric(
            0,
            current.getAge(), 
            current.getHeightCm(), 
            newWeight, 
            newBMI, 
            today
        );
        userService.addBodyMetric(bm);
        
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("Successfully updated weight!");
        System.out.println("");
    }

    public void updateBodyMetrics() {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Body Metrics ]");
        
        User current = userService.getCurrentUser();
        int newAge = current.getAge();
        double newWeight = current.getWeightKg();
        double newHeight = current.getHeightCm();

        System.out.println("\n\t\t\t\t\t[ 1 ]   Update Age");
        System.out.println("\t\t\t\t\t[ 2 ]   Update Weight");
        System.out.println("\t\t\t\t\t[ 3 ]   Update Height");
        System.out.println("\t\t\t\t\t[ 4 ]   Update All");
        System.out.println("\t\t\t\t\t[ 0 ]   Return to main menu\n");

        int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
        if (choice == 0) {
            return;
        }

        switch (choice) {
            case 1: 
                int a = askForAge(); 
                if (a == 0) { return; }
                newAge = a;
                break;
            case 2: 
                double w = askForWeight(); 
                if (w == 0) { return; }
                newWeight = w;
                break;
            case 3: 
                double h = askForHeight(); 
                if (h == 0) { return; }
                newHeight = h;
                break;
            case 4: 
                int allAge = askForAge();
                if (allAge == 0) { return; }
                newAge = allAge;

                double allWeight = askForWeight();
                if (allWeight == 0) { return; }
                newWeight = allWeight;

                double allHeight = askForHeight();
                if (allHeight == 0) { return; }
                newHeight = allHeight;
                break;
            default: System.out.println("\t\t\t\t\t[!] Invalid choice. Please select from 0-4."); return;
        }

        double newBMI = userService.calculateBMI(newWeight, newHeight);
        double newBMR = userService.calculateBMR(newHeight, newWeight, newAge, current.getSex());

        User updatedUser = new User(
            current.getName(),
            newAge,
            newHeight,
            newWeight,
            current.getSex(),
            newBMI,
            newBMR,
            current.getCurrentStreak(),
            current.getLastWorkoutDate()
        );
        userService.updateProfile(updatedUser);

        BodyMetric bm = new BodyMetric(0, newAge, newHeight, newWeight, newBMI, today);
        userService.addBodyMetric(bm);
        
        System.out.println("\t\t\t\t\tBody metrics updated successfully!");
        ConsoleUtils.printBorder();
    }

    public void showBodyMetricHistory() {
        boolean inSubMenu = true;

        while (inSubMenu) {
            List<BodyMetric> bodyMetrics = userService.getBodyMetricHistory();

            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered(String.format("[ %s's Body Metric History ]", userService.getName()));
            System.out.println("");

            printBodyMetrics(bodyMetrics);
            if (bodyMetrics.isEmpty()) return;

            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("\n\t\t\t\t\t[ 1 ]   Delete an entry\t\t[ 2 ]   Update an entry\t\t[ 0 ]   Back\n");

            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1:
                    deleteBodyMetric(bodyMetrics);
                    break;
                case 2:
                    handleEditBodyMetric();
                    break;
                case 0:
                    inSubMenu = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice.");
            }

            System.out.println("");
        }
    }

    private void handleEditBodyMetric() {
        ConsoleUtils.printCentered("[ Edit Body Metric ]");
        System.out.println("");

        List<BodyMetric> history = userService.getBodyMetricHistory();
        if (history.isEmpty()) return; 

        printBodyMetrics(history);

        int choice = -1;
        while (choice < 0 || choice > history.size()) {
            choice = ConsoleUtils.readRequiredInt("Enter row ID to edit (0 to cancel): ", true);
            System.out.println("");

            if (choice == 0) {
                return;
            } else if (choice > history.size()) { 
                System.out.println("\t\t\t\t\tInvalid input. Please select between 0-" + history.size() + ".");
            }
        }

        BodyMetric original = history.get(choice - 1);

        double newWeight = original.getWeightKg();
        double newHeight = original.getHeightCm();
        int newAge = original.getAge();

        System.out.println("\n\t\t\t\t\tWhat would you like to update?");
        System.out.println("\t\t\t\t\t[ 1 ]   Weight (" + newWeight + " kg)");
        System.out.println("\t\t\t\t\t[ 2 ]   Height (" + newHeight + " cm)");
        System.out.println("\t\t\t\t\t[ 3 ]   Age (" + newAge + ")");
        System.out.println("\t\t\t\t\t[ 4 ]   Edit All Fields");
        System.out.println("\n\t\t\t\t\t[ 0 ]   Cancel");

        int fieldChoice = ConsoleUtils.readRequiredInt("Select option: ", true);
        if (fieldChoice == 0) {
            return;
        }

        switch (fieldChoice) {
            case 1:
                newWeight = ConsoleUtils.readDoubleOrDefault("New Weight", newWeight);
                break;
            case 2:
                newHeight = ConsoleUtils.readDoubleOrDefault("New Height", newHeight);
                break;
            case 3:
                newAge = ConsoleUtils.readIntOrDefault("New Age", newAge);
                break;
            case 4:
                ConsoleUtils.printThinBorder();
                newWeight = ConsoleUtils.readDoubleOrDefault("New Weight", newWeight);
                newHeight = ConsoleUtils.readDoubleOrDefault("New Height", newHeight);
                newAge = ConsoleUtils.readIntOrDefault("New Age", newAge);
                break;
            default:
                System.out.println("Invalid selection.");
                return;
        }

        double newBMI = userService.calculateBMI(newWeight, newHeight);

        BodyMetric updated = new BodyMetric(
            original.getId(),
            newAge,
            newHeight,
            newWeight,
            newBMI,
            original.getDate()
        );

        System.out.println("");
        ConsoleUtils.printThinBorder();

        if (userService.updateBodyMetric(original, updated)) {
            System.out.println("\t\t\t\t\tBody metric updated successfully!\n");
        } else {
            System.out.println("\t\t\t\t\tBody metric update failed.");
        }
    }

    private void deleteBodyMetric(List<BodyMetric> bodyMetricsList) {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Delete Body Metric ]");
        System.out.println("");

        printBodyMetrics(bodyMetricsList);

        int maxIndex = bodyMetricsList.size();
        
        int choice = -1;
        while (choice < 0 || choice > maxIndex) {
            choice = ConsoleUtils.readRequiredInt("Enter row ID to delete (0 to cancel): ", true);
        }

        if (choice == 0) {
            return;
        }

        BodyMetric bm = bodyMetricsList.get(choice - 1);
        
        if (userService.deleteBodyMetric(bm)) {
            System.out.println("\t\t\t\t\tBody metric deleted successfully!");
        } else {
            System.out.println("\t\t\t\t\tFailed to delete body metric.");
        }
    }

    private void printBodyMetrics(List<BodyMetric> history) {
        if (history.isEmpty()) {
            System.out.println("\t\t\t\t\tNo body metric history found.\n");
            return;
        }

        int totalItems = history.size();

        String tableHeader = String.format("   %-2s |   %-32s   |   %-32s   |   %-28s   |   %s",
            "id",
            "Height",
            "Weight",
            "BMI",
            "Date"
        );

        if (totalItems <= 10) {
            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();

            int i = 1;
            for (BodyMetric b : history) {
                String space = i < 10 ? "    " : "   ";
                System.out.println(space + i + b);
                i++;
            }
            ConsoleUtils.printThinBorder();

            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = 1;
        boolean viewing = true;

        while (viewing) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);

            ConsoleUtils.printCentered("[ Page " + currentPage + " of " + totalPages + " ]");
            System.out.println("");

            ConsoleUtils.printThinBorderNoNewLine();
            System.out.println(tableHeader);
            ConsoleUtils.printThinBorderNoNewLine();

            for (int i = start; i < end; i++) {
                String space = i < 9 ? "    " : "   ";
                System.out.println(space + (i + 1) + history.get(i));
            }
            
            ConsoleUtils.printThinBorder();

            System.out.println("\t\t\t\t\t[ N ]   Next Page\t\t[ P ]   Prev Page\t\t[ Q ]   Done Viewing\n");
            
            String choice = ConsoleUtils.readRequiredString("Enter choice: ").toUpperCase();
            System.out.println("");

            switch (choice) {
                case "N":
                    if (currentPage < totalPages) {
                        ConsoleUtils.printBorder();
                        currentPage++;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the last page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "P":
                    if (currentPage > 1) {
                        ConsoleUtils.printBorder();
                        currentPage--;
                    }
                    else {
                        System.out.println("\t\t\t\t\t[ ! ]   Already on the first page.\n");
                        ConsoleUtils.printBorder();
                    }
                    break;
                case "Q":
                    viewing = false;
                    break;
                default:
                    System.out.println("\t\t\t\t\t[ ! ]   Invalid choice.\n");
                    ConsoleUtils.printBorder();
            }
        }
    }

    private int askForAge() {
        System.out.println("");
        return ConsoleUtils.readRequiredInt("Enter new age (0 to cancel): ", true);
    }

    private double askForWeight() {
        System.out.println("");
        return ConsoleUtils.readRequiredDouble("Enter new weight (kg) (0 to cancel): ", true);
    }

    private double askForHeight() {
        System.out.println("");
        return ConsoleUtils.readRequiredDouble("Enter new height (cm) (0 to cancel): ", true);
    }

    // ============================================================
    // Miscellaneous
    // ============================================================

    public void showQuote() {
        ConsoleUtils.printBorder();
        ConsoleUtils.printCentered("[ Motivational Quote ]");
        System.out.println("");
        
        ConsoleUtils.printCentered(workoutService.getQuote());
        
        System.out.println("");
        ConsoleUtils.printThinBorder();
        
        ConsoleUtils.pause(); 
    }
}