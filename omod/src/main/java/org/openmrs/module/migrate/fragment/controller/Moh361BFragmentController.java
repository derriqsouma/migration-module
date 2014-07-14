package org.openmrs.module.migrate.fragment.controller;

import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * Created by derric on 7/10/14.
 */
public class Moh361BFragmentController {

    public void controller(@FragmentParam(value = "file", required = false) String file,
                           FragmentModel model) {

        if (file != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_361B/" + file;
            System.out.println(path);
            model.addAttribute("file", file);
        }
    }

    public SimpleObject submit(@RequestParam(value = "file", required = false) String file,
                               UiUtils ui,
                               HttpSession session,
                               @SpringBean KenyaUiUtils kenyaUi) {

//        kenyaUi.notifySuccess(session, "submitted");
            System.out.println(file +"  .derrick");
        return SimpleObject.create();
    }
}
