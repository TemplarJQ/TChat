package com.templar.main;

import com.templar.handler.Client;

public class StartClient {
    public static void main(String[] args) {
        Client client = new Client();
        client.connect();
        client.startGetUserInput();
    }
}
