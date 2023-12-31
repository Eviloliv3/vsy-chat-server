package de.vsy.server.persistent_data.client_data;

import com.fasterxml.jackson.databind.JavaType;
import de.vsy.server.persistent_data.DataFileDescriptor;
import de.vsy.server.persistent_data.PersistentDataFileCreator;
import de.vsy.server.persistent_data.PersistentDataLocationCreator;
import de.vsy.server.persistent_data.SynchronousFileManipulator;
import de.vsy.shared_module.data_element_validation.StringCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static de.vsy.server.persistent_data.DataPathType.EXTENDED;

public abstract class ClientDAO implements ExtendedPathAccess {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected final SynchronousFileManipulator dataProvider;
    private final DataFileDescriptor fileDescriptor;

    public ClientDAO(DataFileDescriptor fileDescriptor, JavaType dataFormat) {
        this.dataProvider = new SynchronousFileManipulator(dataFormat);
        this.fileDescriptor = fileDescriptor;
    }

    @Override
    public void removeFileAccess() {
        this.dataProvider.removeFileReferences();
    }

    @Override
    public void createAccess(String pathExtension) throws IllegalStateException, IllegalArgumentException {
        var checkResult = StringCheck.checkString(pathExtension);

        if (checkResult.isPresent()) {
            throw new IllegalArgumentException(checkResult.get());
        }
        var directories = PersistentDataLocationCreator.createDirectoryPaths(EXTENDED, pathExtension);

        if (directories == null) {
            throw new IllegalStateException("Error occurred during directory creation.");
        }
        var filePaths = PersistentDataFileCreator.createAndGetFilePaths(directories, fileDescriptor.getDataFilename(), LOGGER);

        if (filePaths == null) {
            throw new IllegalStateException("Files were not created. directories: " + Arrays.asList(directories) + "; filename: " + fileDescriptor.getDataFilename());
        }
        this.dataProvider.setFilePaths(filePaths);
    }
}
