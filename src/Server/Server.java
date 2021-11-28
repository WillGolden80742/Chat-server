    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import com.formdev.flatlaf.FlatDarkLaf;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author William
 */
public class Server {

    private ServerSocket serverSocket;
    private int i;
    
    public void setServerSocket(int porta) throws IOException {
        serverSocket = new ServerSocket(porta);
    }

    public Socket waitConnection() throws IOException {
        System.out.println("Sessão : " + i++ + "\nWait connection...");
        Socket socket = serverSocket.accept();
        return socket;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize LaF");
        }
        try {
            Server server = new Server();
            System.out.println("Starting socket...\n");
            server.setServerSocket(2134);
            while (true) {
                Socket socket = server.waitConnection();
                System.out.println("Cliente conectado!");
                TreatConnection treatConnection = new TreatConnection(socket);
                Thread t = new Thread(treatConnection);
                t.start();
                System.out.println("Cliente finalizado\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(TreatConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
