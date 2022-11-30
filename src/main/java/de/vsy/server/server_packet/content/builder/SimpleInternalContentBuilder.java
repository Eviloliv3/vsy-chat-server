package de.vsy.server.server_packet.content.builder;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.vsy.server.server_packet.content.SimpleInternalContentWrapper;
import de.vsy.shared_transmission.packet.content.PacketContent;

/**
 * The Class SimpleInternalContentBuilder.*/
@JsonPOJOBuilder
public class SimpleInternalContentBuilder extends
    ServerPacketContentBuilder<SimpleInternalContentBuilder> {

  private PacketContent wrappedContent;

  public PacketContent getWrappedContent() {
    return this.wrappedContent;
  }

  /**
   * With.
   *
   * @param wrappedContent the wrapped content
   * @return the simple internal content builder
   */
  public SimpleInternalContentBuilder withContent(final PacketContent wrappedContent) {
    this.wrappedContent = wrappedContent;
    return getInstanciable();
  }

  @Override
  public SimpleInternalContentBuilder getInstanciable() {
    return this;
  }

  @Override
  public SimpleInternalContentWrapper build() {
    return new SimpleInternalContentWrapper(this);
  }
}
