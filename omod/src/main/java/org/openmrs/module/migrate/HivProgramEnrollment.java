package org.openmrs.module.migrate;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by derric on 8/15/14.
 */
@Component("migrate.HivProgramEnrollment")
public class HivProgramEnrollment extends AbstractChore {
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
    PatientService patientService = Context.getPatientService();

    @Override
    public void perform(PrintWriter output) throws APIException {
        PatientIdentifierType upnIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        List<Patient> allPatients = patientService.getAllPatients();
        Map<Patient, PatientIdentifier> patientsWithUpn = new HashMap<Patient, PatientIdentifier>();

        for (Patient patient : allPatients) {
            PatientIdentifier upnID = patient.getPatientIdentifier(upnIdType);
            if (upnID != null) {
                patientsWithUpn.put(patient, patient.getPatientIdentifier(upnIdType));
            }
        }

        System.out.println("\n\n*****\n\n"+patientsWithUpn.size()+"\n\n*****\n\n");
    }
}
