/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import Model.DAO.arquivoDAO;
import Model.DAO.clientDAO;
import Model.DAO.contactsListDAO;
import Model.DAO.messagesDAO;
import Model.bean.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.Communication;
import util.States;

/**
 *
 * @author William
 */
public class TreatConnection implements Runnable {

    private Socket socket;
    private States states = States.CONNECTED;

    public TreatConnection(Socket socket) {
        this.socket = socket;
    }

    public void treatConnection(Socket socket) throws IOException, ClassNotFoundException {

        try {
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outPut = new ObjectOutputStream(socket.getOutputStream());

            System.out.println("Tratando...");

            while (states != States.EXIT) {
                Communication communication = (Communication) input.readObject();
                String operation = communication.getOperation();
                Communication reply = null;
                switch (states) {
                    case CONNECTED:
                        reply = executeOperation(operation, communication);
                        if (String.valueOf(reply.getParam("LOGINREPLY")).equals("OK")) {
                            this.states = states.AUTHENTICATED;
                            System.out.println("Autenticado!!");
                        }
                        break;
                    case AUTHENTICATED:
                        System.out.println("LOGADO!");
                        reply = executeOperation(operation, communication);
                        break;
                    default:
                        break;
                }
                outPut.writeObject(reply);
                outPut.flush();
            }
            input.close();
            outPut.close();
        } catch (IOException ex) {
            System.out.println("Problema no tratamento da conexão com o cliente: " + socket.getInetAddress());
            System.out.println("Erro: " + ex.getMessage());
            Logger.getLogger(TreatConnection.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            System.out.println("!!!Finalizando!!!");
            closeSocket(socket);
        }
    }

    private Communication executeOperation(String op, Communication communication) {
        messagesDAO mDAO = new messagesDAO();
        clientDAO cliDAO = new clientDAO();
        arquivoDAO arqDAO = new arquivoDAO();
        contactsListDAO contactDAO = new contactsListDAO();
        Communication reply = new Communication(op + "REPLY");
        switch (op) {
            case "LOGIN":
                String loginReply = cliDAO.authenticated((String) communication.getParam("nickName"), (String) communication.getParam("password"));
                reply.setParam("LOGINREPLY", loginReply);
                System.out.println("login reply :" + loginReply);
                break;
            case "BIOMETRIC":
                String[] biometricReply = cliDAO.biometricAuthenticated((String) communication.getParam("ANDROIDID"));
                reply.setParam("BIOMETRICREPLY", biometricReply[0]);
                reply.setParam("NICKNAME", biometricReply[1]);
                reply.setParam("WELCOME", biometricReply[2]);
                System.out.println("login reply :" + biometricReply[0]);
                break;
            case "CHECKDEVICE":
                reply.setParam("CHECKDEVICEREPLY", cliDAO.checkDevice((String) communication.getParam("ANDROIDID")));
                break;
            case "READ":
                contactsListDAO cDAO = new contactsListDAO();
                reply.setParam("READREPLY", cDAO.read((String) communication.getParam("nickName")));
                break;
            case "MESSAGENOTRECEIVED": {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TreatConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
                reply.setParam("MESSAGENOTRECEIVEDREPLY", mDAO.readNotReceived((String) communication.getParam("nickName"), (String) communication.getParam("contactNickName")));
                break;
            }
            case "MESSAGENOTRECEIVEDALLCONTACTS": {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TreatConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
                reply.setParam("MESSAGENOTRECEIVEDALLCONTACTSREPLY", mDAO.readNotReceivedAllContacts((String) communication.getParam("nickName")));
                break;
            }
            case "MESSAGE":
                List<Message> message = mDAO.read((String) communication.getParam("nickName"), (String) communication.getParam("contactNickName"));
                reply.setParam("MESSAGEREPLY", message);
                break;
            case "CREATEMESSAGE":
                mDAO.create((Message) communication.getParam("SENDEDMESSAGE"));
                reply.setParam("STATUSMESSAGE", mDAO.getStatus());
                break;
            case "DELETEMESSAGE":
                mDAO.delete((int) communication.getParam("idMessage"), (String) communication.getParam("msgFrom"), (String) communication.getParam("msgTo"));
                reply.setParam("STATUSMESSAGE", mDAO.getStatus());
                break;
            case "DOWNLOADFILE":
                reply.setParam("DOWNLOADFILEREPLY", arqDAO.read((String) communication.getParam("nomeHash")));
                break;
            case "CHECKFILE":
                reply.setParam("CHECKFILEREPLY", arqDAO.checkFile((String) communication.getParam("nomeHash")));
                System.out.print("count " + arqDAO.checkFile((String) communication.getParam("nomeHash")));
                break;
            case "CHECKCLIENT":
                reply.setParam("CHECKCLIENTREPLY", cliDAO.checkClient((String) communication.getParam("nickName")));
                break;
            case "CHECKCONTACT":
                reply.setParam("CHECKCONTACTREPLY", contactDAO.checkContact((String) communication.getParam("nickName"), (String) communication.getParam("contactNickName")));
                break;
            case "SEARCHCONTACT":
                reply.setParam("SEARCHCONTACTREPLY", cliDAO.search((String) communication.getParam("nickName")));
                break;
            case "CREATEACCOUNT":
                byte[] pictureC = (byte[]) communication.getParam("picture");
                String formatC = (String) communication.getParam("format");
                String nameC = (String) communication.getParam("name");
                String nickNameC = (String) communication.getParam("nickName");
                String passwordC = (String) communication.getParam("password");
                reply.setParam("CREATEACCOUNTREPLY", cliDAO.createAccount(pictureC, formatC, nameC, nickNameC, passwordC));
                break;
            case "EDITACCOUNT":
                byte[] pictureE = (byte[]) communication.getParam("picture");
                String formatE = (String) communication.getParam("format");
                String nameE = (String) communication.getParam("name");
                String nickNameE = (String) communication.getParam("nickName");
                String passwordE = (String) communication.getParam("password");
                String deviceIDE = (String) communication.getParam("deviceID");
                reply.setParam("EDITACCOUNTREPLY", cliDAO.editAccount(pictureE, formatE, nameE, nickNameE, deviceIDE, passwordE));
                break;
            case "PROFILEIMAGE":
                reply.setParam("PROFILEIMAGEREPLY", cliDAO.profilePic((String) communication.getParam("nickName")));
                break;
            case "LOGOUT":
                states = states.EXIT;
                System.out.println("Deslogado!");
                break;
            default:
                break;
        }
        return reply;
    }

    private void closeSocket(Socket s) throws IOException {
        s.close();
        System.out.println("Encerrada conexão!");
    }

    @Override
    public void run() {
        try {
            System.out.println("Iniciando thread do cliente +" + socket.getInetAddress());
            treatConnection(socket);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(TreatConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
