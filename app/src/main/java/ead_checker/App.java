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
                // long start = System.currentTimeMillis();
                eadChecker.checkCaseStatus();
                // long end = System.currentTimeMillis();
                // System.out.println("Elapsed Time in milliseconds: " + (end - start));
            };
        };
        long delay = 0;
        long period = 24 * 60 * 60 * 1000; // period: 24h
        t.scheduleAtFixedRate(timerTask, delay, period);
    } 
}
