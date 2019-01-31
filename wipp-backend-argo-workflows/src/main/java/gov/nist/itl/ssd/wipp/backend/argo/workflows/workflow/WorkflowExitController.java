/**
 * 
 */
package gov.nist.itl.ssd.wipp.backend.argo.workflows.workflow;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.nist.itl.ssd.wipp.backend.core.CoreConfig;
import gov.nist.itl.ssd.wipp.backend.core.model.job.Job;
import gov.nist.itl.ssd.wipp.backend.core.model.job.JobRepository;
import gov.nist.itl.ssd.wipp.backend.core.model.job.JobStatus;
import gov.nist.itl.ssd.wipp.backend.core.model.workflow.Workflow;
import gov.nist.itl.ssd.wipp.backend.core.model.workflow.WorkflowRepository;
import gov.nist.itl.ssd.wipp.backend.core.model.workflow.WorkflowStatus;
import gov.nist.itl.ssd.wipp.backend.core.rest.exception.ClientException;
import gov.nist.itl.ssd.wipp.backend.images.imagescollection.ImagesCollection;
import gov.nist.itl.ssd.wipp.backend.images.imagescollection.ImagesCollectionRepository;
import gov.nist.itl.ssd.wipp.backend.images.imagescollection.images.ImageHandler;

/**
 * @author Mylene Simon <mylene.simon at nist.gov>
 *
 */
@Controller
@RequestMapping(CoreConfig.BASE_URI + "/workflows/{workflowId}/exit")
public class WorkflowExitController {
	
	@Autowired
    CoreConfig config;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private ImagesCollectionRepository imagesCollectionRepository;
    
    @Autowired
    private ImageHandler imageRepository;

	
	@RequestMapping(
	        value = "",
	        method = RequestMethod.POST,
	        produces = { "application/json" }
	    )
	    public ResponseEntity<Workflow> exit(
	        @PathVariable("workflowId") String workflowId,
	        @RequestBody String status
	    ) {
		
	        // Retrieve Workflow object
	        Optional<Workflow> wippWorkflow = workflowRepository.findById(
	            workflowId
	        );

	        if(!wippWorkflow.isPresent()) {
	            throw new ClientException("Received status for unknown workflow");
	        }
	        
	        Workflow workflow = wippWorkflow.get();
	        
	        // Check validity of status
	        WorkflowStatus wfStatus;
	        status = status.toUpperCase();
	        try {
	        	wfStatus = WorkflowStatus.valueOf(status);
	        } catch (IllegalArgumentException ex){
	        	throw new ClientException("Received unknown status " + status + " for workflow " + workflowId);
	        }

	        boolean success = false;
	        String errorMessage = "";
	        
	        switch(wfStatus) {
		        case SUCCEEDED:
		        	success = true;
		        	break;
		        case ERROR:
		        case FAILED:
		        	errorMessage = "Error during workflow execution.";
		        	break;
		        case CANCELLED:
		        	errorMessage = "Workflow cancelled.";
		        	break;
		        default:
		        	throw new ClientException("Received non-exit status for workflow " + workflowId);
	        }

	     // Retrieve workflow's jobs to import results in case of success
	        List<Job> jobList = jobRepository.findByWippWorkflow(workflowId);
	        for(Job job: jobList) {
	        	job.setStatus(JobStatus.valueOf(status));
	            if(success) {
	            	ImagesCollection outputImagesCollection = new ImagesCollection(job);
	            	outputImagesCollection = imagesCollectionRepository.save(
	            			outputImagesCollection);

	                try {
	                    imageRepository.importFolder(outputImagesCollection.getId(),
	                            // new File(getJobTempFolder(job), "images")); 
	                    		// TODO: output conventions for plugins
	                    		getJobTempFolder(job));
	                } catch (IOException ex) {
	                	imagesCollectionRepository.delete(outputImagesCollection);
	                    job.setStatus(JobStatus.ERROR);
	                    job.setError("Unable to import job result");
	                }

	            } else {
	            	job.setError(errorMessage);
	            }
	            jobRepository.save(job);
	        }
	        workflow.setEndTime(new Date());
	        workflow.setStatus(wfStatus);
	        workflowRepository.save(workflow);

	        return new ResponseEntity<>(workflow, HttpStatus.OK);
	    }
	
	protected final File getJobTempFolder(Job job) {
        return new File(config.getJobsTempFolder(), job.getId());
    }

}
