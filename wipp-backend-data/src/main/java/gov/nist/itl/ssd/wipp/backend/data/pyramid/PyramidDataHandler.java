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
package gov.nist.itl.ssd.wipp.backend.data.pyramid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nist.itl.ssd.wipp.backend.core.CoreConfig;
import gov.nist.itl.ssd.wipp.backend.core.model.data.DataHandler;
import gov.nist.itl.ssd.wipp.backend.core.model.job.Job;

/**
 * @author Mohamed Ouladi <mohamed.ouladi at nist.gov>
 */
@Component("pyramidDataHandler")
public class PyramidDataHandler implements DataHandler{

	
    @Autowired
    CoreConfig config;
    
    @Autowired
    private PyramidRepository pyramidRepository;

    public PyramidDataHandler() {
    }

    @Override
    public void importData(Job job, String outputName) throws IOException {
		Pyramid outputPyramid= new Pyramid(job, outputName);
		outputPyramid = pyramidRepository.save(outputPyramid);

        try {        	
        	 File pyramidFolder = new File(config.getPyramidsFolder(), outputPyramid.getId());
        	 pyramidFolder.mkdirs();
        	 Files.move(getJobOutputTempFolder(job, outputName).toPath(), pyramidFolder.toPath());
        } catch (IOException ex) {
        	pyramidRepository.delete(outputPyramid);
            throw ex;
        }
    }

    public String exportDataAsParam(String value) {
        String pyramidId = value;
        File inputPyramidFolder = new File(config.getPyramidsFolder(), pyramidId);
        String pyramidPath = inputPyramidFolder.getAbsolutePath();
        return pyramidPath;
    }
    
    private final File getJobOutputTempFolder(Job job, String outputName) {
        return new File( new File(config.getJobsTempFolder(), job.getId()), outputName);
    }
}
