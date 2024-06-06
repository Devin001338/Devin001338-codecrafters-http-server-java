import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    //
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    //
    try {
    serverSocket = new ServerSocket(4221);
    serverSocket.setReuseAddress(true);
    clientSocket = serverSocket.accept(); // Wait for connection from client.
    
    clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      InputStream clientSocketInputStream = clientSocket.getInputStream();
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(clientSocketInputStream));
      String[] requestLine = reader.readLine().split(" ");
      if (requestLine[1].equals("/")) {
        clientSocket.getOutputStream().write(
            "HTTP/1.1 200 OK\r\n\r\n".getBytes());
      } else {
        clientSocket.getOutputStream().write(
            "HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }

    System.out.println("accegitpted new connection");
    
    } catch (IOException e) {
    System.out.println("IOException: " + e.getMessage());
    }
  }
}
