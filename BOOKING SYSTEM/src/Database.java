import java.util.HashMap;

public class Database {
    private static HashMap<String, String> users = new HashMap<>();

    public static boolean authenticateUser(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    public static boolean registerUser(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, password);
        return true;
    }
    
}
