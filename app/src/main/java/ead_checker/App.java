package ead_checker;

public class App {
    public static void main(String[] args) {
        EadChecker eadChecker = new EadChecker();
        eadChecker.checkCaseStatus(args[0], args[1]);
    } 
}
