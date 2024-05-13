import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AWSClienteSocket {

    private final static int PUERTO = 11000;
    private static final String DNSAWS = "ec2-54-173-21-231.compute-1.amazonaws.com";
    //private static final String DNSAWS = "127.0.0.1";

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Scanner in = new Scanner(System.in);
        System.out.print("Introduce la frase a enviar en minúsculas: ");
        String frase = in.nextLine();

        try (Socket socket = new Socket(DNSAWS, PUERTO)) {
            ObjectOutputStream salida = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            System.out.println("Se envia la frase: " + frase);
            salida.writeObject(frase);
            salida.flush(); //vaciamos el buffer

            ObjectInputStream entrada = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            System.out.println("La frase recibida es: " + (String) entrada.readObject());
        } catch (IOException ex) {
            System.err.println("Error. De entrada salida.");
        }
    }
}
