package org.openmrs.module.migrate.kisii;

import org.openmrs.DrugOrder;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.migrate.ConvertStringToDate;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derric on 3/2/15.
 */
public class RegimenSubstitution {
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

    public RegimenSubstitution(String path, HttpSession session, KenyaUiUtils kenyaUi) {
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
            String upn = rowData.get(1).toString();
//            String amrId = rowData.get(0).toString();

            List<Patient> patientListUsingUpn = patientService.getPatients(null, upn, null, true);
//            List<Patient> patientListUsingAmrId = patientService.getPatients(null, amrId, null, true);

            if (patientListUsingUpn.size() > 0) {
                if (upn.isEmpty()) {
//                    patient = patientService.getPatient(patientListUsingAmrId.get(0).getPatientId());
                    saveDrugOrders(patient,rowData);

                } else {
                    patient = patientService.getPatient(patientListUsingUpn.get(0).getPatientId());
                    System.out.println("\n\n\n ****" + patient.getFamilyName());
                    saveDrugOrders(patient,rowData);
                }
            }
        }
    }

    private void saveDrugOrders(Patient patient, List<Object> rowData) throws ParseException {

            if (rowData.get(2) != "" && rowData.get(3) != "") {
                String[] substitutedRegimen = getRegimen(rowData.get(3).toString()).split("\\+");

                List<DrugOrder> drugOrderList = Context.getOrderService().getDrugOrdersByPatient(patient);
                if (!drugOrderList.isEmpty()) {
                    for (DrugOrder drugOrder : drugOrderList) {
                        drugOrder.setDiscontinued(true);
                        drugOrder.setDiscontinuedDate(convertStringToDate.convert(rowData.get(2).toString()));
                        getRegimenSwitchReason(drugOrder, rowData.get(41).toString());
                    }

                    for (int i = 0; i < substitutedRegimen.length; i++) {
                        DrugOrder lastDrugOrder = new DrugOrder();

                        lastDrugOrder.setPatient(patient);
                        lastDrugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7"));
                        lastDrugOrder.setStartDate(convertStringToDate.convert(rowData.get(38).toString()));
                        lastDrugOrder.setFrequency("");
                        lastDrugOrder.setDose(300.00);
                        lastDrugOrder.setUnits("mg");

                        if (substitutedRegimen[i] != null) {
                            setDrugsConcepts(lastDrugOrder, substitutedRegimen[i]);
                            Context.getOrderService().saveOrder(lastDrugOrder);
                        }

                    }
                }

            }

    }

    private void getRegimenSwitchReason(DrugOrder drugOrder, String s) {

        if (s.contains("1.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("102AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("2.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("1434AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("3.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("160559AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("4.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("160567AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("5.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("160561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else if (s.contains("6.0")){
            drugOrder.setDiscontinuedReason(conceptService.getConceptByUuid("1754AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }else {
            drugOrder.setDiscontinuedReasonNonCoded("");
        }
    }

    private String getRegimen(String s) {
        String regimen = null;
        if (s.equalsIgnoreCase("1.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("2.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("3.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("4.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("5.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("6.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("7.0")){
            regimen = "AZT+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("8.0")){
            regimen = "AZT+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("9.0")){
            regimen = "AZT+DDI+NFV";
        }else if (s.equalsIgnoreCase("10.0")){
            regimen = "AZT+DDI+NFV";
        }else if (s.equalsIgnoreCase("11.0")){
            regimen = "ABC+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("12.0")){
            regimen = "ABC+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("13.0")){
            regimen = "ABC+DDI+NFV";
        }else if (s.equalsIgnoreCase("14.0")){
            regimen = "ABC+DDI+NFV";
        }else if (s.equalsIgnoreCase("15.0")){
            regimen = "AZT+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("16.0")){
            regimen = "AZT+3TC+IDV/r";
        }else if (s.equalsIgnoreCase("17.0")){
            regimen = "AZT+DDL+IDV/r";
        }else if (s.equalsIgnoreCase("18.0")){
            regimen = "AZT+DDL+IDV/r";
        }else if (s.equalsIgnoreCase("19.0")){
            regimen = "TDF+ABC+LPV/r";
        }else if (s.equalsIgnoreCase("33.0")){
            regimen = "ABC+DDI+LPV/r";
        }else if (s.equalsIgnoreCase("34.0")){
            regimen = "ABC+DDI+NFV";
        }else if (s.equalsIgnoreCase("54.0")){
            regimen = "AZT+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("21.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("22.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("23.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("24.0")){
            regimen = "D4T+3TC+NVP";
        }else if (s.equalsIgnoreCase("26.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("27.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("28.0")){
            regimen = "D4T+3TC+EFV";
        }else if (s.equalsIgnoreCase("29.0")){
            regimen = "D4T+ABC+LPV/r";
        }else if (s.equalsIgnoreCase("30.0")){
            regimen = "D4T+ABC+NFV";
        }else if (s.equalsIgnoreCase("35.0")){
            regimen = "AZT+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("38.0")){
            regimen = "TDF+3TC+NVP";
        }else if (s.equalsIgnoreCase("39.0")){
            regimen = "TDF+3TC+EFV";
        }else if (s.equalsIgnoreCase("40.0")){
            regimen = "ABC+3TC+EFV";
        }else if (s.equalsIgnoreCase("42.0")){
            regimen = "ABC+3TC+LPV/r";
        }else if (s.equalsIgnoreCase("43.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("44.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("45.0")){
            regimen = "AZT+3TC+EFV";
        }else if (s.equalsIgnoreCase("46.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("47.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("48.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("49.0")){
            regimen = "AZT+3TC+NVP";
        }else if (s.equalsIgnoreCase("50.0")){
            regimen = "ABC+3TC+AZT";
        }else if (s.equalsIgnoreCase("51.0")){
            regimen = "ABC+3TC+NVP";
        }else if (s.equalsIgnoreCase("52.0")){
            regimen = "ABC+3TC+NVP";
        }

        return regimen;
    }

    private void setDrugsConcepts(DrugOrder drugOrder, String regimen) {


        if (regimen.contains("3TC")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("78643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ABC")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("70056AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if (regimen.contains("AZT")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("86663AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }
        else if (regimen.contains("DDI")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("74807AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("EFV")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("75523AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("LPV/r")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("794AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("NVP")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("80586AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("IDV/r")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("77995AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("D4T")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("84309AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("TDF")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("84795AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("IDV/r")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("77995AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("NELFINAVIR'UNK")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("80487AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("EMTRICITABINE")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("75628AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ATAZANAVIR")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("71647AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("ETRAVIRINE")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("159810AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        } else if (regimen.contains("RALTEGRAVIR")) {

            drugOrder.setConcept(conceptService.getConceptByUuid("154378AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }else if (regimen.equalsIgnoreCase("NFV")){
            drugOrder.setConcept(conceptService.getConceptByUuid(""));
        }
    }

}
