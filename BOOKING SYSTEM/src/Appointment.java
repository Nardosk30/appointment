import javafx.beans.property.*;

public class Appointment {
    private final IntegerProperty appointmentId;
    private final IntegerProperty userId;
    private final StringProperty date;
    private final StringProperty time;
    private final StringProperty status;

    public Appointment(int appointmentId, int userId, String date, String time, String status) {
        this.appointmentId = new SimpleIntegerProperty(appointmentId);
        this.userId = new SimpleIntegerProperty(userId);
        this.date = new SimpleStringProperty(date);
        this.time = new SimpleStringProperty(time);
        this.status = new SimpleStringProperty(status);
    }

    public int getAppointmentId() { return appointmentId.get(); }
    public IntegerProperty appointmentIdProperty() { return appointmentId; }

    public int getUserId() { return userId.get(); }
    public IntegerProperty userIdProperty() { return userId; }

    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }

    public String getTime() { return time.get(); }
    public StringProperty timeProperty() { return time; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
}

