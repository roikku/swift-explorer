package org.swiftexplorer.swift.operations;

import java.io.File;
import java.util.List;

import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.StoredObject;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;

public interface LargeObjectManager {
    public void uploadObjectAsSegments(StoredObject obj, File file, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) ;
    public void uploadObjectAsSegments(StoredObject obj, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) ;
    public boolean isSegmented (StoredObject obj) ;
    public String getSumOfSegmentsMd5 (StoredObject obj) ;
    public List<StoredObject> getSegmentsList (StoredObject obj) ;
    public long getActualSegmentSize (StoredObject obj) ;
}
