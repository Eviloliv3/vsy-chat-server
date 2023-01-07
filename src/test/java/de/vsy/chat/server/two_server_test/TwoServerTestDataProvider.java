/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.two_server_test;

import de.vsy.chat.server.raw_server_test.TestClientDataProvider;
import de.vsy.shared_transmission.dto.authentication.AuthenticationDTO;

import java.util.List;

import static java.util.List.of;

/**
 *
 */
public class TwoServerTestDataProvider {

    public static final List<Integer> CLIENT_CONNECTION_PORTS = of(7371, 7371);
    public static final List<AuthenticationDTO> CLIENT_AUTHENTICATION_DATA_LIST = of(
            TestClientDataProvider.FRANK_1_AUTH, TestClientDataProvider.PETER_1_AUTH);
}
