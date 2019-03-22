/*
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United
 * States Code this software is not subject to copyright protection and is
 * in the public domain. This software is an experimental system. NIST assumes
 * no responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or
 * any other characteristic. We would appreciate acknowledgement if the
 * software is used.
 */
package gov.nist.itl.ssd.wipp.backend.data.imagescollection;

import gov.nist.itl.ssd.wipp.backend.core.CoreConfig;
import gov.nist.itl.ssd.wipp.backend.core.model.data.DataHandler;
import gov.nist.itl.ssd.wipp.backend.core.model.job.Job;
import gov.nist.itl.ssd.wipp.backend.data.imagescollection.images.ImageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * @author Samia Benjida <samia.benjida at nist.gov>
 */
@Component("collectionDataHandler")
public class ImagesCollectionDataHandler implements DataHandler {

    @Autowired
    CoreConfig config;

    @Autowired
    private ImagesCollectionRepository imagesCollectionRepository;

    @Autowired
    private ImageHandler imageRepository;

    public ImagesCollectionDataHandler() {
    }

    @Override
    public void importData(Job job, String outputName) throws IOException {
        ImagesCollection outputImagesCollection = new ImagesCollection(job, outputName);
        outputImagesCollection = imagesCollectionRepository.save(
                outputImagesCollection);
        try {
            imageRepository.importFolder(outputImagesCollection.getId(),
                    // new File(getJobTempFolder(job), "images"));
                    // TODO: output conventions for plugins
                    getJobOutputTempFolder(job, outputName));
        } catch (IOException ex) {
            imagesCollectionRepository.delete(outputImagesCollection);
            throw ex;
        }
    }

    public String exportDataAsParam(String value) {
        String imagesCollectionId = value;
        File inputImagesFolder = imageRepository.getFilesFolder(imagesCollectionId);
        String imagesCollectionPath = inputImagesFolder.getAbsolutePath();
        return imagesCollectionPath;
    }

    private final File getJobOutputTempFolder(Job job, String outputName) {
        return new File(config.getJobsTempFolder(), job.getId() + "/" +  outputName);
    }
}
