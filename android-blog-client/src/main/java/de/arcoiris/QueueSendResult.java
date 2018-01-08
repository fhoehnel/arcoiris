package de.arcoiris;

/**
 * Created by fho on 28.12.2017.
 */

public class QueueSendResult {
    private int entriesToSend;
    private int success;
    private int failed;

    public void setEntriesToSend(int newVal) {
        entriesToSend = newVal;
    }

    public int getEntriesToSend() {
        return entriesToSend;
    }

    public void setSuccess(int newVal) {
         success = newVal;
    }

    public int getSuccess() {
        return success;
    }

    public void setFailed(int newVal) {
        failed = newVal;
    }

    public int getFailed() {
        return failed;
    }
}
