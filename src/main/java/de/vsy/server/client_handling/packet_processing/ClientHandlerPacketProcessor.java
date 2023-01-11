package de.vsy.server.client_handling.packet_processing;

import de.vsy.server.data.access.HandlerAccessManager;
import de.vsy.server.data.socketConnection.LocalServerConnectionData;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.shared_module.packet_exception.PacketProcessingException;
import de.vsy.shared_module.packet_exception.PacketValidationException;
import de.vsy.shared_module.packet_management.ClientDataProvider;
import de.vsy.shared_module.packet_processing.ContentProcessor;
import de.vsy.shared_module.packet_processing.PacketProcessor;
import de.vsy.shared_module.packet_processing.ProcessingCondition;
import de.vsy.shared_module.packet_validation.content_validation.PacketContentValidator;
import de.vsy.shared_transmission.packet.Packet;
import de.vsy.shared_transmission.packet.content.PacketContent;

public class ClientHandlerPacketProcessor<T extends PacketContent> implements PacketProcessor {
    protected final ProcessingCondition processingCondition;
    protected final PacketContentValidator<T> contentValidator;
    protected final ContentProcessor<T> contentProcessor;
    protected final LocalServerConnectionData serverConnectionData;
    protected final ClientDataProvider localClientDataManager;

    public ClientHandlerPacketProcessor(final ClientDataProvider clientData,
                                        final ProcessingCondition condition,
                                        final PacketContentValidator<T> contentValidator,
                                        final ContentProcessor<T> contentProcessor) {
        this.processingCondition = condition;
        this.contentValidator = contentValidator;
        this.contentProcessor = contentProcessor;
        this.localClientDataManager = clientData;
        this.serverConnectionData = HandlerAccessManager.getLocalServerConnectionData();
    }

    @Override
    public void processPacket(Packet input)
            throws PacketValidationException, PacketProcessingException {

        if (this.processingCondition.checkCondition()) {
            final T validatedContent = this.extractPacketContent(input);
            this.contentProcessor.processContent(validatedContent);
        } else {
            throw new PacketValidationException(this.processingCondition.getErrorMessage());
        }
    }

    /**
     * Extracts the content from Packet and unwraps the content from SimpleInternalContentWrapper, if
     * necessary. Then validates the extracted content, casts it to the required type and returns it,
     * if no invalid data was specified.
     *
     * @param request the request to process
     * @return content the validated Content
     * @throws PacketValidationException if content could not successfully be validated or cast to the
     *                                   required type
     */
    protected T extractPacketContent(final Packet request) throws PacketValidationException {
        PacketContent content = request.getPacketContent();

        if (content instanceof SimpleInternalContentWrapper internalContentWrapper) {
            content = internalContentWrapper.getWrappedContent();
        }
        return this.contentValidator.castAndValidateContent(content);
    }
}
