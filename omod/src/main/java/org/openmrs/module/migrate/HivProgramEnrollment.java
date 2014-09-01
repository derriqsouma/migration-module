package org.openmrs.module.migrate;

import org.openmrs.*;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by derric on 8/25/14.
 */


public class HivProgramEnrollment {
    PatientService patientService = Context.getPatientService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();


    public HivProgramEnrollment() {

    }

    public void init() {
        PatientIdentifierType upnIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
        List<Patient> allPatients = patientService.getAllPatients();
        PatientProgram patientProgram = new PatientProgram();
        for (Patient patient : allPatients) {
            PatientIdentifier upnID = patient.getPatientIdentifier(upnIdType);

            if (upnID != null) {
                List<PatientProgram> patientPrograms = new ArrayList<PatientProgram>((workflowService.getPatientPrograms(patient, hivProgram, null, null, null, null, true)));
                if(patientPrograms.isEmpty()){
                    //enroll into hiv

                    patientProgram.setPatient(patient);//enroll in HIV Program
                    patientProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));

                    patientProgram.setDateEnrolled(new Date());

                    workflowService.savePatientProgram(patientProgram);

                }

            }
        }
    }

}
