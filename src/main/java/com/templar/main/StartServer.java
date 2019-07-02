package com.templar.main;


import com.templar.handler.Server;

public class StartServer {

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
