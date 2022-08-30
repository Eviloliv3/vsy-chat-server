/*
 *
 */
package de.vsy.chat.server.server_test_helpers;

import de.vsy.shared_module.shared_module.packet_management.OutputBuffer;
import de.vsy.shared_transmission.shared_transmission.dto.CommunicatorDTO;
import de.vsy.shared_transmission.shared_transmission.packet.Packet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Creation tool for simple Packetcreation and transmission. */
public
class RequestPacketCreator {

    private final Map<PacketPropertiesProvider, List<Class<?>>> associations;
    private final OutputBuffer outputHandle;

    /**
     * Instantiates a new request creator.
     *
     * @param outputHandle the output handle
     * @param clientData the client dataManagement
     */
    public
    RequestPacketCreator (final OutputBuffer outputHandle,
                          final CommunicatorDTO clientData) {
        this.outputHandle = outputHandle;
        this.associations = new HashMap<>();
        // setupPropertyAssociations(clientData);
    }

    /**
     * Request.
     *
     * @param request the request
     */
    public
    void request (final Packet request) {
        sendRequest(request);
    }

    /*
     */
    /**
     * Sets the up property associations.
     *
     * @param clientData the new up property associations
     */
  /*
  private
  void setupPropertyAssociations (final ClientDataProvider clientData) {
      associations.put(new ClientBoundPacketPropertiesCreator(clientData),
                       addClientBoundList());
      associations.put(new ServerBoundPacketPropertiesCreator(clientData),
                       addServerBoundList());
  }*/

    /**
     * Adds the client bound list.
     *
     * @return the list< class<? extends PacketDataManagement>>
     */
  /*
  private
  List<Class<?>> addClientBoundList () {
      final List<Class<?>> clientBoundList = new ArrayList<>(5);

      clientBoundList.add(TextMessageDTO.class);
      clientBoundList.add(ContactRelationResponseDTO.class);
      clientBoundList.add(ContactRelationRequestDTO.class);
      clientBoundList.add(ErrorDTO.class);

      return clientBoundList;
  }*/

    /**
     * Adds the server bound list.
     *
     * @return the list< class<? extends PacketDataManagement>>
     */
  /*
  private
  List<Class<?>> addServerBoundList () {
      final List<Class<?>> serverBoundList = new ArrayList<>(6);

      serverBoundList.add(LoginRequestDTO.class);
      serverBoundList.add(LogoutRequestDTO.class);
      serverBoundList.add(ReconnectRequestDTO.class);
      serverBoundList.add(NewAccountRequestDTO.class);
      serverBoundList.add(MessengerTearDownDTO.class);
      serverBoundList.add(ClientMessengerStatusDTO.class);

      return serverBoundList;
  }*/

    /**
     * Send request.
     *
     * @param output the output
     */
    private
    void sendRequest (final Packet output) {
        outputHandle.appendPacket(output);
    }

    /**
     * Choose PacketProperties provider.
     *
     * @param dataClass the dataManagement class
     * @return the PacketProperties provider
     */
  /*
  private PacketPropertiesProvider choosePacketPropertiesProvider(final Class<?> dataClass) {
    PacketPropertiesProvider resultingProvider = null;
    Iterator<Entry<PacketPropertiesProvider, List<Class<?>>>> iterate;
    Entry<PacketPropertiesProvider, List<Class<?>>> association;

    iterate = associations.entrySet().iterator();

    while (iterate.hasNext()) {
      association = iterate.next();

      if (association.getValue().contains(dataClass)) {
        resultingProvider = association.getKey();
        break;
      }
    }
    return resultingProvider;
  }
  */
}
