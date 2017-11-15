package alexmog.apilib.api.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static Pattern pattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    
    public static boolean checkEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}