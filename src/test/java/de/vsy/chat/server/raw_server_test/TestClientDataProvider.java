/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package de.vsy.chat.server.raw_server_test;

import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.shared_transmission.dto.authentication.AuthenticationDTO;

import java.util.List;

import static java.util.List.of;

/** @author fredward */
public
class TestClientDataProvider {

    public static final AuthenticationDTO FRANK_1_AUTH = AuthenticationDTO.valueOf(
            "frank1", "login");
    public static final AuthenticationDTO MARKUS_1_AUTH = AuthenticationDTO.valueOf(
            "markus1", "login");
    public static final AuthenticationDTO ADRIAN_1_AUTH = AuthenticationDTO.valueOf(
            "adrian1", "login");
    public static final AuthenticationDTO PETER_1_AUTH = AuthenticationDTO.valueOf(
            "peter1", "login");
    public static final AuthenticationDTO MAX_1_AUTH = AuthenticationDTO.valueOf(
            "max1", "login");
    public static final AuthenticationDTO THOMAS_1_AUTH = AuthenticationDTO.valueOf(
            "thomas1", "login");
    public static final CommunicatorDTO FRANK_1_COMM = CommunicatorDTO.valueOf(15001,
                                                                               "Frank Relation1");
    public static final CommunicatorDTO MARKUS_1_COMM = CommunicatorDTO.valueOf(
            15002, "Markus Relation2");
    public static final CommunicatorDTO ADRIAN_1_COMM = CommunicatorDTO.valueOf(
            15003, "Adrian Relation3");
    public static final CommunicatorDTO PETER_1_COMM = CommunicatorDTO.valueOf(15004,
                                                                               "Peter Relation4");
    public static final CommunicatorDTO MAX_1_COMM = CommunicatorDTO.valueOf(15005,
                                                                             "Max Status1");
    public static final CommunicatorDTO THOMAS_1_COMM = CommunicatorDTO.valueOf(
            15006, "Thomas Status2");
    public static final List<AuthenticationDTO> AUTH_CLIENT_LIST = of(FRANK_1_AUTH);
    public static final List<AuthenticationDTO> STATUS_CLIENT_LIST = of(MAX_1_AUTH,
                                                                        THOMAS_1_AUTH);
    public static final List<AuthenticationDTO> RELA_CLIENT_LIST = of(FRANK_1_AUTH,
                                                                      MARKUS_1_AUTH,
                                                                      PETER_1_AUTH,
                                                                      ADRIAN_1_AUTH);
    public static final List<AuthenticationDTO> CHAT_CLIENT_LIST = of(FRANK_1_AUTH,
                                                                      MARKUS_1_AUTH);
}
