package org.openmrs.module.migrate.page.controller;

import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.migrate.MigrateConstant;
import org.openmrs.module.migrate.Moh361A;
import org.openmrs.module.migrate.Moh361B;
import org.openmrs.module.migrate.Moh408;
import org.openmrs.module.migrate.kakuma.KakumaEid;
import org.openmrs.module.migrate.kakuma.KakumaPatients;
import org.openmrs.module.migrate.kakuma.KakumaVisits;
import org.openmrs.module.migrate.kisii.Kisii;
import org.openmrs.module.migrate.maragua.MaraguaPatients;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * Created by derric on 6/19/14.
 */

@AppPage(MigrateConstant.APP_MIGRATE)
public class MigrationHomePageController {

    @RequestMapping()
    public void controller(UiUtils ui,
                           @RequestParam(value = "moh361Afile", required = false) String moh361Afile,
                           @RequestParam(value = "moh361Bfile", required = false) String moh361Bfile,
                           @RequestParam(value = "moh408file", required = false) String moh408file,
                           @RequestParam(value = "kakuma_patients", required = false) String kakuma_patients,
                           @RequestParam(value = "kakuma_visits", required = false) String kakuma_visits,
                           @RequestParam(value = "kakuma_eid", required = false) String kakuma_eid,
                           @RequestParam(value = "maragua_patients", required = false) String maragua_patients,
                           @RequestParam(value = "maragua_visits", required = false) String maragua_visits,
                           @RequestParam(value = "kisii", required = false) String kisii,
                           PageModel model,
                           HttpSession session,
                           @SpringBean KenyaUiUtils kenyaUi) throws Exception {

        if (kakuma_patients != "") {
            String path = "/home/derric/Desktop/migration/migration/kakuma/" + kakuma_patients;
            KakumaPatients kakumaPatients = new KakumaPatients(path, session, kenyaUi);
            kakumaPatients.init();

        }
        if (kakuma_visits != "") {
            String path = "/home/derric/Desktop/migration/migration/kakuma/" + kakuma_visits;
            KakumaVisits kakumaVisits = new KakumaVisits(path, session, kenyaUi);
            kakumaVisits.init();

        }
        if (kakuma_eid != "") {
            String path = "/home/derric/Desktop/migration/migration/kakuma/" + kakuma_eid;
            KakumaEid kakumaEid= new KakumaEid(path, session, kenyaUi);
            kakumaEid.init();

        }

        if (maragua_patients != "") {
            String path = "/home/derric/Dropbox/I-TECH/migration/maragua/" + maragua_patients;

            MaraguaPatients maraguaPatients = new MaraguaPatients(path, session, kenyaUi);
            maraguaPatients.init();
        }

        if (kisii != "") {
            String path = "/home/derric/Desktop/migration/migration/" + kisii;
            Kisii kisii1 = new Kisii(path, session, kenyaUi);
            kisii1.init();

        }

        if (moh361Afile != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_361A/" + moh361Afile;
            model.addAttribute("file", moh361Afile);

            Moh361A moh361A = new Moh361A(path, session, kenyaUi);
            moh361A.initMoh361A();
        }

        if (moh361Bfile != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_361B/" + moh361Bfile;

            Moh361B moh361B = new Moh361B(path, session, kenyaUi);
            moh361B.initMoh361B();

        }

        if (moh408file != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_408/" + moh408file;
            Moh408 moh408 = new Moh408(path, session, kenyaUi);
            moh408.initMoh408();
        }

    }

}
