import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Send {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        SaltyProtocol sender = new SaltyProtocol("localhost", 5000, 300);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("input> ");
                sender.test(bufferedReader.readLine().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
