package HEAT.util;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ConsoleUtils {
    
    public static final int TOTAL_WIDTH = 161;
    private static final Scanner input = new Scanner(System.in);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");

    public static String readRequiredString(String prompt) {
        String result;
        while (true) {
            System.out.print("\t\t\t\t\t" + prompt);
            result = input.nextLine().trim();

            if (!result.isEmpty()) {
                return result;
            }

            System.out.println("\t\t\t\t\t[ ! ]   Input cannot be empty. Please try again.");
        }
    }

    public static int readRequiredInt(String prompt, boolean allowZero) {
        while (true) {
            String inputString = readRequiredString(prompt);

            try {
                int value = Integer.parseInt(inputString);
                if (allowZero) {
                    if (value >= 0) { return value; }
                    else { System.out.println("\t\t\t\t\t[ ! ]   Error: Value cannot be negative."); }
                } else {
                    if (value > 0) { return value; }
                    else { System.out.println("\t\t\t\t\t[ ! ]   Error: Value must be greater than 0."); }
                }
            } catch (NumberFormatException e) {
                System.out.println("\t\t\t\t\t[ ! ]   Error. Please enter digits only.");
            }
        }
    }

    public static double readRequiredDouble(String prompt, boolean allowZero) {
        while (true) {
            String inputString = readRequiredString(prompt);

            try {
                double value = Double.parseDouble(inputString);
                if (allowZero) {
                    if (value >= 0) { return value; }
                    else { System.out.println("\t\t\t\t\t[ ! ]   Error: Value cannot be negative."); }
                } else {
                    if (value > 0) { return value; }
                    else { System.out.println("\t\t\t\t\t[ ! ]   Error: Value must be greater than 0."); }
                }
            } catch (NumberFormatException e) {
                System.out.println("\t\t\t\t\t[ ! ]   Invalid number. Please try again.");
            }
        }
    }

    public static LocalDate readRequiredLocalDate(String message, boolean allowEmpty) {
        while (true) {
            System.out.print("\t\t\t\t\t" + message + " [YYYY-MM-DD]: ");
            String date = input.nextLine().trim();

            if (date.isEmpty() && allowEmpty) {
                System.out.println("\t\t\t\t\tCreating open-ended goal...");
                return null;
            }

            try {
                return LocalDate.parse(date, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("\t\t\t\t\t [ ! ]   Invalid date format. Please use YYYY-MM-DD (e.g., 2025-12-31).");
            }
        }
    }

    public static String readStringOrDefault(String prompt, String defaultValue) {
        System.out.print("\t\t\t\t\t" + prompt + " [" + defaultValue + "]: ");
        String result = input.nextLine().trim();
        return result.isEmpty() ? defaultValue : result;
    }

    public static int readIntOrDefault(String prompt, int defaultValue) {
        while (true) {
            String inputString = readStringOrDefault(prompt, String.valueOf(defaultValue));
            try {
                return Integer.parseInt(inputString);
            } catch (NumberFormatException e) {
                System.out.println("\t\t\t\t\t[ ! ]   Invalid number. Please try again.");
            }
        }
    }

    public static double readDoubleOrDefault(String prompt, double defaultValue) {
        while (true) {
            String inputString = readStringOrDefault(prompt, String.valueOf(defaultValue));
            try {
                return Double.parseDouble(inputString);
            } catch (NumberFormatException e) {
                System.out.println("\t\t\t\t\t[ ! ]   Invalid number. Please try again.");
            }
        }
    }

    public static LocalDate readDateOrDefault(String prompt, LocalDate defaultValue) {
        while (true) {
            String inputString = readStringOrDefault(prompt, defaultValue.toString());
            try {
                return LocalDate.parse(inputString, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("\t\t\t\t\t[ ! ]   Invalid date format (YYYY-MM-DD).");
            }
        }
    }

    public static LocalDate readDateOrNull(String prompt, LocalDate currentValue) {
        String currentStr = (currentValue == null) ? "Open-ended" : currentValue.toString();
        
        System.out.print("\t\t\t\t\t" + prompt + " (Type 'none' to clear) [" + currentStr + "]: ");
        String result = input.nextLine().trim();

        if (result.isEmpty()) {
            return currentValue;
        }

        if (result.equalsIgnoreCase("none") || result.equalsIgnoreCase("open-ended") || result.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            return LocalDate.parse(result, formatter);
        } catch (DateTimeParseException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Invalid date format. Keeping original value.");
            return currentValue;
        }
    }    

    public static String promptForEdit(String label, String currentValue) {
        System.out.print(label + "[" + currentValue + "]: ");
        String userInput = input.nextLine().trim();
        
        if (userInput.isEmpty()) {
            return currentValue;
        }

        return userInput;
    }

    public static void printBorder() {
        System.out.println("[]" + "=".repeat(TOTAL_WIDTH - 4) + "[]\n");
    }

    public static void printThinBorder() {
        System.out.println("[]" + "-".repeat(TOTAL_WIDTH - 4) + "[]\n");
    }

    public static void printThinBorderNoNewLine() {
        System.out.println("[]" + "-".repeat(TOTAL_WIDTH - 4) + "[]");
    }    

    public static void printCentered(String text) {
        if (text.length() > TOTAL_WIDTH) {
            text = text.substring(0, TOTAL_WIDTH - 3) + "...";
        }

        int paddingLeft = (TOTAL_WIDTH - text.length()) / 2;
        int paddingRight = TOTAL_WIDTH - text.length() - paddingLeft;

        String leftSpace = " ".repeat(paddingLeft);
        String rightSpace = " ".repeat(paddingRight);

        System.out.println(leftSpace + text + rightSpace);
    }

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: If the system command fails, just print a bunch of newlines
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    public static void pause() {
        System.out.println("\t\t\t\t\tPress Enter to return...");
        try {
            input.nextLine(); 
        } catch (Exception e) {
            // Fallback if scanner isn't available
            try { System.in.read(); } catch (Exception ignored) {}
        }
    }
}
