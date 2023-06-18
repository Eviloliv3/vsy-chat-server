package de.vsy.server.server_packet.packet_creation;

import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.shared_module.packet_creation.ContentIdentificationProviderImpl;
import de.vsy.shared_transmission.packet.content.PacketContent;
import de.vsy.shared_transmission.packet.property.packet_identifier.ContentIdentifier;

public class ServerContentIdentificationProviderImpl extends ContentIdentificationProviderImpl {

    {
        super.registeredIdentifiers.putAll(new ServerStatusIdentificationProvider().getIdentifiers());
    }

    @Override
    public ContentIdentifier getContentIdentifier(PacketContent data) {
        final PacketContent toIdentify;

        if (data instanceof SimpleInternalContentWrapper) {
            toIdentify = ((SimpleInternalContentWrapper) data).getWrappedContent();
        } else {
            toIdentify = data;
        }
        return super.getContentIdentifier(toIdentify);
    }
}
