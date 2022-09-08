package ead_checker;

import java.util.TimerTask;

import java.util.Timer;

public class App {
    public static void main(String[] args) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        EadChecker eadChecker = new EadChecker();
        Timer t = new Timer();
        TimerTask timerTask = new TimerTask(){
            @Override
            public void run() {
                eadChecker.checkCaseStatus();
            };
        };
        long delay = 0;
        long period = 60 * 1000;
        t.scheduleAtFixedRate(timerTask, delay, period);
    } 
}
