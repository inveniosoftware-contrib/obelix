import java.util.HashMap;
import java.util.Map;

import static spark.Spark.before;
import static spark.Spark.halt;

public class WebInterfaceAuth {

    public static void enableCORS(final String origin, final String methods, final String headers) {
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }

    public static boolean tokenIsValid(String service, String user, String token) {
        String key = service+"_"+user;
        Map<String, String> validTokens = new HashMap<>();
        validTokens.put("obelix_frecar", "rh38949834gfg9f9");
        return validTokens.containsKey(key) && validTokens.get(key).equals(token);
    }

    public static void requireValidToken() {
        before((request, response) -> {
            try {
                String[] split = request.uri().split("/");
                if (!tokenIsValid(split[1], split[2], split[3])) {
                    halt(401, "403 forbidden");
                }
            }
            catch (ArrayIndexOutOfBoundsException e) {
                halt(401, "403 forbidden");
            }
        });
    }


}
