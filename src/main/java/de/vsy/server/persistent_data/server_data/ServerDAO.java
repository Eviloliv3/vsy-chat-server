package de.vsy.server.persistent_data.server_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.PersistentDataFileCreator;
import de.vsy.server.persistent_data.PersistentDataLocationCreator;
import de.vsy.server.persistent_data.SynchronousFileManipulator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static de.vsy.server.persistent_data.DataOwnershipDescriptor.SERVER;

public abstract class ServerDAO implements ServerDataAccess{

    protected static final Logger LOGGER = LogManager.getLogger();
    protected final SynchronousFileManipulator dataProvider;
    private final DataFileDescriptor fileDescriptor;

    public ServerDAO(final DataFileDescriptor fileDescriptor, JavaType dataFormat){
        this.dataProvider = new SynchronousFileManipulator(dataFormat);
        this.fileDescriptor = fileDescriptor;
    }
    @Override
    public void removeFileAccess() {
        this.dataProvider.removeFileReferences();
    }

    @Override
    public void removeFiles(){
        //TODO
    }

    @Override
    public void createFileAccess() throws IllegalStateException {
        var directories = PersistentDataLocationCreator.createDirectoryPaths(SERVER, null);

        if(directories == null) {
            throw new IllegalStateException("Error occurred during directory creation.");
        }
        var filePaths = PersistentDataFileCreator.createAndGetFilePaths(directories, fileDescriptor.getDataFilename(), LOGGER);

        if(filePaths == null) {
            throw new IllegalStateException("Files were not created. directories: "+ Arrays.asList(directories) +"; filename: "  + fileDescriptor.getDataFilename());
        }
        this.dataProvider.setFilePaths(filePaths);
    }
}
