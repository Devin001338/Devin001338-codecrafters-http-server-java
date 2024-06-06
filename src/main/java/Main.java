import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);

      while (true) {
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("Accepted new connection");

        InputStream input = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = reader.readLine();
        System.out.println("Request Line: " + line);

        if (line != null) {
          String[] HttpRequest = line.split(" ");
          String path = HttpRequest[1];
          System.out.println("Requested Path: " + path);

          OutputStream output = clientSocket.getOutputStream();

          if (path.startsWith("/echo/")) {
            String echoStr = path.substring(6); // Extract the string after "/echo/"
            String responseBody = echoStr;
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                     "Content-Type: text/plain\r\n" +
                                     "Content-Length: " + responseBody.length() + "\r\n" +
                                     "\r\n";
            output.write(responseHeaders.getBytes());
            output.write(responseBody.getBytes());
          } else if ("/".equals(path) || "/index.html".equals(path)) {
            String responseBody = "Welcome to the homepage!";
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                                     "Content-Type: text/plain\r\n" +
                                     "Content-Length: " + responseBody.length() + "\r\n" +
                                     "\r\n";
            output.write(responseHeaders.getBytes());
            output.write(responseBody.getBytes());
          } else {
            String responseHeaders = "HTTP/1.1 404 Not Found\r\n\r\n";
            output.write(responseHeaders.getBytes());
          }

          output.close();
        }

        // Close the streams and socket
        reader.close();
        clientSocket.close();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          System.out.println("Failed to close server socket: " + e.getMessage());
        }
      }
    }
  }
}
