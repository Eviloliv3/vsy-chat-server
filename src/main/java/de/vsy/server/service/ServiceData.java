package de.vsy.server.service;

import static java.util.Map.copyOf;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.EnumMap;
import java.util.Map;

/**
 * Die zu verwendende Servicespezifikationen.
 *
 * @author Frederic Heath
 */
@JsonPOJOBuilder
public class ServiceData {

  private final Map<ServiceResponseDirection, Service.TYPE> responseDirections;
  private final String serviceBaseName;
  private final Service.TYPE serviceType;
  private int serviceId;
  private String serviceName;

  /**
   * Instantiates a new service dataManagement.
   *
   * @param dataBuilder the dataManagement builder
   */
  private ServiceData(final ServiceDataBuilder dataBuilder) {
    this.serviceType = dataBuilder.serviceType;
    this.serviceBaseName = dataBuilder.serviceBaseName;
    this.responseDirections = dataBuilder.responseDirections;
  }

  public Map<ServiceResponseDirection, Service.TYPE> getResponseDirections() {
    return copyOf(this.responseDirections);
  }

  public String getServiceBaseName() {
    return this.serviceBaseName;
  }

  public Service.TYPE getServiceType() {
    return this.serviceType;
  }

  public int getServiceId() {
    return this.serviceId;
  }

  public void setServiceId(int serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceName() {
    return this.serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * The Enum ServiceResponseDirection.
   */
  public enum ServiceResponseDirection {
    INBOUND, OUTBOUND
  }

  /**
   * The Class ServiceDataBuilder.
   */
  public static class ServiceDataBuilder {

    private Map<ServiceResponseDirection, Service.TYPE> responseDirections;
    private String serviceBaseName;
    private Service.TYPE serviceType;

    /**
     * Instantiates a new service dataManagement builder.
     */
    private ServiceDataBuilder() {
    }

    /**
     * Creates the.
     *
     * @return the service dataManagement builder
     */
    public static ServiceDataBuilder create() {
      return new ServiceDataBuilder();
    }

    /**
     * Builds the.
     *
     * @return the service dataManagement
     */
    public ServiceData build() {
      return new ServiceData(this);
    }

    /**
     * With direction.
     *
     * @param direction   the direction
     * @param serviceType the service type
     * @return the service dataManagement builder
     */
    public ServiceDataBuilder withDirection(final ServiceResponseDirection direction,
        final Service.TYPE serviceType) {

      if (direction == null) {
        throw new IllegalArgumentException("Keine Richtung angegeben.");
      }

      if (this.serviceType == null) {
        throw new IllegalArgumentException("Kein Servicetyp angegeben.");
      }

      if (this.responseDirections == null) {
        this.responseDirections = new EnumMap<>(ServiceResponseDirection.class);
      }
      this.responseDirections.put(direction, serviceType);
      return this;
    }

    /**
     * With name.
     *
     * @param baseName the base name
     * @return the service dataManagement builder
     */
    public ServiceDataBuilder withName(final String baseName) {
      this.serviceBaseName = baseName;
      return this;
    }

    /**
     * With type.
     *
     * @param serviceType the service type
     * @return the service dataManagement builder
     */
    public ServiceDataBuilder withType(final Service.TYPE serviceType) {
      this.serviceType = serviceType;
      return this;
    }
  }
}
