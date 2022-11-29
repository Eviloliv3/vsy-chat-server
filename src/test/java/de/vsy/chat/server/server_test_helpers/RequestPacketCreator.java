/*
 *
 */
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.packet.Packet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creation tool for simple Packetcreation and transmission.
 */
public class RequestPacketCreator {

  private final Map<PacketPropertiesProvider, List<Class<?>>> associations;
  private final OutputBuffer outputHandle;

  /**
   * Instantiates a new request creator.
   *
   * @param outputHandle the output handle
   * @param clientData   the client dataManagement
   */
  public RequestPacketCreator(final OutputBuffer outputHandle, final CommunicatorDTO clientData) {
    this.outputHandle = outputHandle;
    this.associations = new HashMap<>();
    // setupPropertyAssociations(clientData);
  }

  /**
   * Request.
   *
   * @param request the request
   */
  public void request(final Packet request) {
    sendRequest(request);
  }

  /**
   * Send request.
   *
   * @param output the output
   */
  private void sendRequest(final Packet output) {
    outputHandle.appendPacket(output);
  }

}
