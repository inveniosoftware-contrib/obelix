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
        String key = service+"-"+user;
        Map<String, String> validTokens = new HashMap<>();
        validTokens.put("obelix-cern-prod", "id8734g8sodif9hw3894fh");
        validTokens.put("obelix-cern-test", "abdsidfsi8gf9wfigf32r2");
        validTokens.put("obelix-ir-test", "ihf9ewhg943gf98wuiofheuh");
        validTokens.put("obelix-frecar", "h928f9384gf93849f38g9");
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
