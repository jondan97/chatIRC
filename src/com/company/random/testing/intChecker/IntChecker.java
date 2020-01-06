package com.company.random.testing.intChecker;

public class IntChecker {
    public static void main(String[] args) {
        String str = "5g";
        System.out.println(isInteger(str));
    }

    //Checks if input is an integer
    //takes a string as a parameter
    //returns boolean
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
