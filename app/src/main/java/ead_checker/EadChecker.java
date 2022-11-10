package ead_checker;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class EadChecker {

    private void generateCaseStatus(long receiptNumber, CaseRecord localCaseRecord,
            CaseRecord latestCaseRecord, StringBuilder messageStringBuilder) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WAC").append(receiptNumber).append(": ").append("\n").append("WAS: ");
        if (localCaseRecord == null) {
            stringBuilder.append("N/A");
        } else {
            stringBuilder.append(localCaseRecord.getTitle()).append(": ")
                    .append(localCaseRecord.getContent());
        }
        stringBuilder.append("\n").append("IS: ").append(latestCaseRecord.getTitle()).append(" - ")
                .append(latestCaseRecord.getContent()).append("\n").append("\n");

        messageStringBuilder.append(stringBuilder);
    }

    private void checkCaseStatus(long startNumber, long endNumber) {
        ConcurrentMap<Long, CaseRecord> localDataMap = readLocalData();
        ConcurrentMap<Long, CaseRecord> latestDataMap = new ConcurrentHashMap<>();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors); // IO-heavy
        long receiptNumber = startNumber;
        while (receiptNumber <= endNumber) {
            Runnable singleCaseChecker =
                    new SingleCaseChecker(receiptNumber, localDataMap, latestDataMap);
            executor.execute(singleCaseChecker);
            receiptNumber++;
        }
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ErrorHandler.getInstance().handleError(ex);
        }
        // If there are any updates, generate the message and overwrite the local-case-data;
        boolean hasUpdates = false;
        StringBuilder messageStringBuilder = new StringBuilder();
        for (long receiptNum : latestDataMap.keySet()) {
            CaseRecord localCaseRecord =
                    localDataMap.containsKey(receiptNum) ? localDataMap.get(receiptNum) : null;
            CaseRecord latestCaseRecord = latestDataMap.get(receiptNum);
            if (localCaseRecord == null
                    || !localCaseRecord.getTitle().equals(latestCaseRecord.getTitle())) {
                hasUpdates = true;
                generateCaseStatus(receiptNumber, localCaseRecord, latestCaseRecord,
                        messageStringBuilder);
            }
        }
        if (hasUpdates) {
            writeLocalData(latestDataMap);
            // Send message
            String subject = "UPDATES ON EAD STATUS!";
            MessageSender.getInstance().sendMessage(subject, messageStringBuilder.toString());
        }
    }

    private ConcurrentMap<Long, CaseRecord> readLocalData() {
        ConcurrentMap<Long, CaseRecord> localDataMap = null;
        String filename = "local-case-data.ser";
        // Deserialization
        try {
            // Reading the Case record from local file
            FileInputStream file = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(file);
            localDataMap = (ConcurrentMap<Long, CaseRecord>) in.readObject();
            in.close();
            file.close();
        } catch (IOException ex) {
            return new ConcurrentHashMap<>();
        } catch (ClassNotFoundException ex) {
            ErrorHandler.getInstance().handleError(ex);
        }
        return localDataMap;
    }

    private void writeLocalData(ConcurrentMap<Long, CaseRecord> latestDataMap) {
        String filename = "local-case-data.ser";
        // Serialization
        try {
            FileOutputStream file = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(latestDataMap);
            out.close();
            file.close();
        } catch (IOException ex) {
            ErrorHandler.getInstance().handleError(ex);
        }
    }

    public void checkCaseStatus() {
        int indexRange = Integer.valueOf(Args.getInstance().getRange());
        long startIndex =
                Long.valueOf(Args.getInstance().getReceiptNumber().substring(3)) - indexRange / 2;
        long endIndex = startIndex + indexRange / 2;
        checkCaseStatus(startIndex, endIndex);
    }
}
