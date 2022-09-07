package ead_checker;

import java.util.TimerTask;

import java.util.Timer;

public class App {
    public static void main(String[] args) {
        EadChecker eadChecker = new EadChecker();
        Timer t = new Timer();
        TimerTask timerTask = new TimerTask(){
            @Override
            public void run() {
                // 'receiptNumber, case range, recipient's gmail, sender's gmail, sender's password'
                eadChecker.checkCaseStatus(args[0], args[1], args[2], args[3], args[4]);
            };
        };
        long delay = 0;
        long period = 60 * 1000;
        t.scheduleAtFixedRate(timerTask, delay, period);
    } 
}
