package org.openmrs.module.migrate;

import org.openmrs.*;
import org.openmrs.api.*;
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
    FormService formService = Context.getFormService();
    EncounterService encounterService = Context.getEncounterService();
    ProviderService providerService = Context.getProviderService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();


    public HivProgramEnrollment() {

    }

    public void init() {
        PatientIdentifierType upnIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
        List<Patient> allPatients = patientService.getAllPatients();

        int counter =0;

        for (Patient patient : allPatients) {
            PatientIdentifier upnID = patient.getPatientIdentifier(upnIdType);

            if (upnID != null) {
                PatientProgram patientProgram = new PatientProgram();
                List<PatientProgram> patientPrograms = new ArrayList<PatientProgram>((workflowService.getPatientPrograms(patient, hivProgram, null, null, null, null, true)));
                if(patientPrograms.size()>0){
                    
                    //enroll into hiv
                    Encounter encounter = new Encounter();
                    encounter.setPatient(patient);
                    encounter.setForm(formService.getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));
                    encounter.setEncounterType(encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
                    encounter.setLocation(defaultLocation);
                    encounter.setDateCreated(new Date());
                    encounter.setProvider(encounterService.getEncounterRoleByUuid("a0b03050-c99b-11e0-9572-0800200c9a66"), providerService.getProviderByUuid("ae01b8ff-a4cc-4012-bcf7-72359e852e14"));
                    encounter.setEncounterDatetime(new Date());

                    encounterService.saveEncounter(encounter);

                    patientProgram.setPatient(patient);
                    patientProgram.setProgram(workflowService.getProgramByUuid("dfdc6d40-2f2f-463d-ba90-cc97350441a8"));
                    patientProgram.setDateEnrolled(new Date());
                    workflowService.savePatientProgram(patientProgram);
                    counter +=1;
                }

            }
        }

        System.out.println("\n\n Patients Enrolled =>: "+ counter +"\n\n");
    }

}
