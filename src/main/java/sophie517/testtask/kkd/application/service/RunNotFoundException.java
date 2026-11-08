package sophie517.testtask.kkd.application.service;

public class RunNotFoundException extends RuntimeException {
    public RunNotFoundException(String runId) {
        super("Run '" + runId + "' not found");
    }
}
