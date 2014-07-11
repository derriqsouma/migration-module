package org.openmrs.module.migrate.fragment.controller;

import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by derric on 7/10/14.
 */
public class Moh361BFragmentController {

    public void controller(@FragmentParam(value = "file", required = false) String file,
                           FragmentModel model) {

        if (file != "") {
            String path = "/home/derric/Desktop/ampath_data/MOH_361B/" + file;
            model.addAttribute("file", file);
        }
    }

    public SimpleObject submit(@RequestParam(value = "file", required = false) String file,
                               UiUtils ui, PageModel model) {

            return SimpleObject.create(file);

    }
}
