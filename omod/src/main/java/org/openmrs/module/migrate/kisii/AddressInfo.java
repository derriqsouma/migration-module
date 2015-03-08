package org.openmrs.module.migrate.kisii;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.wrapper.PersonWrapper;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derric on 3/3/15.
 */
public class AddressInfo {
    String path;
    int counter = 0;
    HttpSession session;
    KenyaUiUtils kenyaUi;
    ObsService obsService = Context.getObsService();
    EncounterService encounterService = Context.getEncounterService();
    LocationService locationService = Context.getLocationService();
    FormService formService = Context.getFormService();
    ProviderService providerService = Context.getProviderService();
    ConceptService conceptService = Context.getConceptService();
    Patient patient = new Patient();
    ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
    ConvertStringToDate convertStringToDate = new ConvertStringToDate();
    PatientService patientService = Context.getPatientService();
    Location defaultLocation = Context.getService(KenyaEmrService.class).getDefaultLocation();

    public AddressInfo(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.path = path;
        this.session = session;
        this.kenyaUi = kenyaUi;
    }

    public void init() throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        ReadExcelSheetKisii readExcelSheetKakuma = new ReadExcelSheetKisii(path, session, kenyaUi);
        sheetData = readExcelSheetKakuma.readExcelSheet();

        checkForPatient(sheetData);
    }

    private void checkForPatient(List<List<Object>> sheetData) throws ParseException {

        for (int i = 1; i < sheetData.size(); i++) {
            patient = null;
            List<Object> rowData = sheetData.get(i);
            String[] upn1 = String.valueOf(rowData.get(1)).replaceAll("\\.", "").split("E");
            String upn = upn1[0];

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);

            if (patientListUsingUpn.size() > 0) {
                if (upn.isEmpty()) {
                    saveAddressInfo(patient, rowData);

                } else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    saveAddressInfo(patient, rowData);
                }
            }
        }
    }

    private void saveAddressInfo(Patient patient, List<Object> rowData){
        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddress1(rowData.get(2).toString());
//        personAddress.setAddress2(rowData.get(8).toString());
        personAddress.setAddress3(rowData.get(8).toString());
        personAddress.setAddress5(rowData.get(6).toString());
        personAddress.setAddress6(rowData.get(5).toString());
        personAddress.setCountyDistrict(rowData.get(4).toString());

        patient.addAddress(personAddress);

        PersonWrapper personWrapper = new PersonWrapper(patient);
        personWrapper.setTelephoneContact(rowData.get(3).toString());

        patientService.savePatient(patient);

    }
}
