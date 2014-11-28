import static spark.Spark.get;

public class HelloWorld {

    public static void main(String[] args) {
        get("/hello", (req, res) -> "Hello World");
    }

}
