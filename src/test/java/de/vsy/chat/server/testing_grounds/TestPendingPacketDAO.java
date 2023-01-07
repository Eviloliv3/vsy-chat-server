package de.vsy.chat.server.testing_grounds;

import de.vsy.server.persistent_data.PersistentDataFileCreator;
import de.vsy.server.persistent_data.PersistentDataLocationCreator;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestPendingPacketDAO {

    /*
     * @Test void writePackets () throws InterruptedException {
     * PacketCompiler.addOriginatorEntityProvider(this::getOriginator);
     * PacketCompiler.addContentIdentificator( new
     * ContentIdentificationProviderImpl()); var dao = new PendingPacketDAO();
     * dao.createFileAccess(18002);
     * dao.setPendingPackets(PendingType.PROCESSOR_BOUND, Collections.emptyMap());
     *
     * List<PacketContent> allPacketContentSamples = new ArrayList<>(); final var
     * authData = AuthenticationDTO.valueOf("login", "password"); final var
     * communicatorData = CommunicatorData.valueOf(15001, 15001, "test name"); final
     * var commDTO = ConvertCommDataToDTO.convertFrom(communicatorData);
     * allPacketContentSamples.add(new LoginRequestDTO(authData));
     * allPacketContentSamples.add(new LoginResponseDTO(commDTO));
     * allPacketContentSamples.add(new LogoutRequestDTO(commDTO));
     * allPacketContentSamples.add(new LogoutResponseDTO(true));
     * allPacketContentSamples.add(new ReconnectRequestDTO(commDTO));
     * allPacketContentSamples.add(new ReconnectResponseDTO(true));
     * allPacketContentSamples.add(new AccountCreationRequestDTO());
     * allPacketContentSamples.add(new
     * ClientStatusChangeDTO(ClientService.MESSENGER, true, commDTO));
     * allPacketContentSamples.add(new MessengerSetupDTO(Collections.emptyMap(),
     * Collections.emptyMap())); allPacketContentSamples.add(new
     * MessengerTearDownDTO(true)); allPacketContentSamples.add(new
     * ContactStatusChangeDTO(EligibleContactEntity.CLIENT, true, commDTO,
     * Collections.emptyList())); final var relationRequest = new
     * ContactRelationRequestDTO(EligibleContactEntity.CLIENT,
     * commDTO.getCommunicatorId(), MARKUS_1_COMM.getCommunicatorId(), commDTO,
     * true); allPacketContentSamples.add(relationRequest);
     * allPacketContentSamples.add(ContactRelationResponseDTO.valueOf(
     * relationRequest, commDTO, true)); allPacketContentSamples.add(new
     * ErrorDTO("error", null)); allPacketContentSamples.add(new
     * TextMessageDTO(commDTO.getCommunicatorId(), EligibleContactEntity.CLIENT,
     * MARKUS_1_COMM.getCommunicatorId(), "test nachricht"));
     * allPacketContentSamples.add(new
     * BaseStatusSyncDTO((SimpleStatusSyncBuilder)new
     * SimpleStatusSyncBuilder<>().withToAdd(true).withClientState(
     * ClientState.ACTIVE_MESSENGER).withContactData(commDTO).
     * withOriginatingServerId(2134))); allPacketContentSamples.add(new
     * ExtendedStatusSyncDTO((ExtendedStatusSyncBuilder)new
     * ExtendedStatusSyncBuilder<>().withContactIdSet(Collections.emptySet()).
     * withClientState(ClientState.ACTIVE_MESSENGER).withContactData(commDTO).
     * withOriginatingServerId(1234))); allPacketContentSamples.add(new
     * ServerFailureDTO((ServerFailureContentBuilder)new
     * ServerFailureContentBuilder().withFailedServerId(4234).
     * withOriginatingServerId(4234))); allPacketContentSamples.add(new
     * InterServerCommSyncDTO(1234));
     *
     *
     * for(var content : allPacketContentSamples){ final var toWrite =
     * PacketCompiler.createRequest(CommunicationEndpoint.getClientEntity(
     * MARKUS_1_COMM.getCommunicatorId()),content);
     * dao.appendPendingPacket(PendingType.PROCESSOR_BOUND, toWrite); } final var
     * pendingProcessorBound = dao.readPendingPackets(PendingType.PROCESSOR_BOUND);
     * Assertions.assertEquals(allPacketContentSamples.size(),
     * pendingProcessorBound.size()); }
     *
     * CommunicationEndpoint getOriginator () { return
     * CommunicationEndpoint.getClientEntity(15001); }
     *
     *
     * @Test void testMultipleThreadChannelAccess() throws InterruptedException {
     * final var path = PersistentDataLocationCreator.createDirectoryPath(
     * PersistentDataLocationCreator.DataOwnershipDescriptor.SERVER, "test"); final
     * var file = PersistentDataFileCreator.createAndGetFilePath(path,
     * "fileLock.lock", LogManager.getLogger()); try(var raf = new
     * RandomAccessFile(file.toFile(), "r")){ Runnable test = () -> { try { var
     * channel = raf.getChannel(); var lock = raf.getChannel().tryLock(0,
     * Long.MAX_VALUE, true); Assertions.assertTrue(lock.isValid(), "successful");
     * Thread.sleep(500); lock.release(); } catch (IOException |
     * InterruptedException e) { throw new RuntimeException(e); } }; ExecutorService
     * threadPool = Executors.newFixedThreadPool(2); threadPool.execute(test);
     * threadPool.execute(test); Thread.sleep(750); threadPool.shutdownNow(); }
     * catch (IOException e) { throw new RuntimeException(e); } }
     */
    @Test
    void testParallelReadingDifferentChannelsSameJVM() throws InterruptedException {
        var test = new Runnable() {
            @Override
            public void run() {
                final String path;
                try {
                    path = PersistentDataLocationCreator
                            .createDirectoryPath(PersistentDataLocationCreator.DataOwnershipDescriptor.SERVER,
                                    "test");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                final var file = PersistentDataFileCreator.createAndGetFilePath(path, "fileLock.lock",
                        LogManager.getLogger());
                RandomAccessFile fileChannel = null;
                try {
                    fileChannel = new RandomAccessFile(file.toFile(), "r");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                try {
                    try {
                        FileLock lock = null;
                        if (!fileChannel.getChannel().isOpen()) {
                            lock = fileChannel.getChannel().tryLock(0, Long.MAX_VALUE, false);
                        }
                        var buffer = ByteBuffer.allocate(1024);
                        Thread.sleep(200);
                        Assertions.assertEquals(-1, fileChannel.getChannel().read(buffer));
                        if (lock != null && fileChannel.getChannel().isOpen()) {
                            lock.release();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    var buffer = ByteBuffer.allocate(1024);
                    fileChannel.getChannel().read(buffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        threadPool.execute(test);
        threadPool.execute(test);
        threadPool.execute(test);
        Thread.sleep(700);
        threadPool.shutdownNow();
    }
}
