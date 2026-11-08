package sophie517.testtask.kkd.application.service;

public class UnitNotFoundException extends RuntimeException {
    public UnitNotFoundException(String unitId) {
        super("Unit '" + unitId + "' not found in the selected run");
    }
}
