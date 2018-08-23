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
package gov.nist.itl.ssd.wipp.backend.images.imagescollection;

import gov.nist.itl.ssd.wipp.backend.core.rest.exception.ClientException;
import gov.nist.itl.ssd.wipp.backend.images.imagescollection.images.ImageHandler;
import gov.nist.itl.ssd.wipp.backend.images.imagescollection.metadatafiles.MetadataFileHandler;

import java.util.Date;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 *
 * @author Antoine Vandecreme <antoine.vandecreme at nist.gov>
 */
@Component
@RepositoryEventHandler(ImagesCollection.class)
public class ImagesCollectionEventHandler {

    @Autowired
    private ImagesCollectionRepository imagesCollectionRepository;

    @Autowired
    private ImageHandler imageRepository;

    @Autowired
    private MetadataFileHandler metadataFileRepository;

    @Autowired
    private ImagesCollectionLogic imagesCollectionLogic;

    @HandleBeforeCreate
    public void handleBeforeCreate(ImagesCollection imagesCollection) {
        imagesCollectionLogic.assertCollectionNameUnique(
                imagesCollection.getName());
        imagesCollection.setCreationDate(new Date());
    }

    @HandleBeforeSave
    public void handleBeforeSave(ImagesCollection imagesCollection) {
        ImagesCollection oldTc = imagesCollectionRepository.findOne(
                imagesCollection.getId());

        if (!Objects.equals(
                imagesCollection.getCreationDate(),
                oldTc.getCreationDate())) {
            throw new ClientException("Can not change creation date.");
        }

        if (!Objects.equals(
                imagesCollection.getSourceJob(),
                oldTc.getSourceJob())) {
            throw new ClientException("Can not change source job.");
        }

        if (!Objects.equals(imagesCollection.getName(), oldTc.getName())) {
            imagesCollectionLogic.assertCollectionNameUnique(
                    imagesCollection.getName());
        }

        if (imagesCollection.isLocked() != oldTc.isLocked()) {
            if (!imagesCollection.isLocked()) {
                throw new ClientException("Can not unlock images collection.");
            }
            imagesCollectionLogic.assertCollectionNotImporting(oldTc);
            imagesCollectionLogic.assertCollectionHasNoImportError(oldTc);
        }
    }

    @HandleBeforeDelete
    public void handleBeforeDelete(ImagesCollection imagesCollection) {
        ImagesCollection oldTc = imagesCollectionRepository.findOne(
                imagesCollection.getId());
        imagesCollectionLogic.assertCollectionNotLocked(oldTc);
    }

    @HandleAfterDelete
    public void handleAfterDelete(ImagesCollection imagesCollection) {
        imageRepository.deleteAll(imagesCollection.getId(), false);
        metadataFileRepository.deleteAll(imagesCollection.getId(), false);
    }

}
